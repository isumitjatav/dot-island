package com.nothing.island.ai.telephony

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Thread-safe bounded LRU cache independent of Android framework stubs for JVM unit testability.
 */
internal class BoundedLruCache<V : Any>(private val maxEntries: Int) {
    private val map = object : LinkedHashMap<String, V>(maxEntries.coerceAtLeast(4), 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>?): Boolean {
            return size > maxEntries.coerceAtLeast(4)
        }
    }

    @Synchronized
    fun get(key: String): V? = map[key]

    @Synchronized
    fun put(key: String, value: V) {
        map[key] = value
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}

/**
 * Resolves and caches circular caller avatars from:
 * 1. ContactsContract (when [Manifest.permission.READ_CONTACTS] is granted), querying
 *    PhoneLookup (for numbers/tel URIs) and display-name filter lookup.
 * 2. Notification [android.app.Person] icon / [Notification.EXTRA_LARGE_ICON].
 * 3. Returns `null` when unavailable so CallCard falls back to the monochrome dot-matrix initials avatar.
 */
open class GenericCallerPhotoResolver<T : Any>(
    maxCacheEntries: Int = 24,
    private val hasContactsPermission: () -> Boolean,
    private val lookupPhotoByPhone: (String) -> T?,
    private val lookupPhotoByName: (String) -> T?,
    private val fallbackNotificationPhoto: () -> T?,
    private val isValidEntry: (T) -> Boolean = { true },
) {
    private val cache = BoundedLruCache<T>(maxCacheEntries)

    fun resolve(
        callerDisplayName: String?,
        phoneNumberOrUri: String? = null,
    ): T? {
        val cleanPhone = CallerPhotoResolver.normalizePhoneLookupInput(phoneNumberOrUri)
        val cleanName = callerDisplayName?.trim()?.takeIf { it.isNotEmpty() }
        val cacheKey = cleanPhone ?: cleanName

        if (cacheKey != null) {
            cache.get(cacheKey)?.let { cached ->
                if (isValidEntry(cached)) return cached
            }
        }

        val resolved = if (hasContactsPermission()) {
            (cleanPhone?.let { lookupPhotoByPhone(it) }
                ?: cleanName?.let { lookupPhotoByName(it) })
                ?: fallbackNotificationPhoto()
        } else {
            fallbackNotificationPhoto()
        }

        if (resolved != null && cacheKey != null && isValidEntry(resolved)) {
            cache.put(cacheKey, resolved)
        }
        return resolved
    }

    fun clearCache() {
        cache.clear()
    }
}

class CallerPhotoResolver(
    maxCacheEntries: Int = 24,
    hasContactsPermission: () -> Boolean,
    lookupPhotoByPhone: (String) -> Bitmap?,
    lookupPhotoByName: (String) -> Bitmap?,
    fallbackNotificationPhoto: () -> Bitmap?,
) : GenericCallerPhotoResolver<Bitmap>(
    maxCacheEntries = maxCacheEntries,
    hasContactsPermission = hasContactsPermission,
    lookupPhotoByPhone = lookupPhotoByPhone,
    lookupPhotoByName = lookupPhotoByName,
    fallbackNotificationPhoto = fallbackNotificationPhoto,
    isValidEntry = { !it.isRecycled },
) {
    companion object {
        private val sharedCache = BoundedLruCache<Bitmap>(24)

        fun normalizePhoneLookupInput(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            val stripped = if (trimmed.startsWith("tel:", ignoreCase = true)) {
                Uri.decode(trimmed.substring(4))
            } else {
                trimmed
            }
            val digits = stripped.filter { it.isDigit() || it == '+' }
            return digits.takeIf { it.length >= 3 }
        }

        fun resolve(
            context: Context,
            callerDisplayName: String?,
            phoneNumberOrUri: String? = null,
            notificationExtras: Bundle? = null,
        ): Bitmap? {
            val cleanPhone = normalizePhoneLookupInput(phoneNumberOrUri)
                ?: extractPhoneFromPersonUri(notificationExtras)
            val cleanName = callerDisplayName?.trim()?.takeIf { it.isNotEmpty() }
            val cacheKey = cleanPhone ?: cleanName

            if (cacheKey != null) {
                sharedCache.get(cacheKey)?.let { cached ->
                    if (!cached.isRecycled) return cached
                }
            }

            val resolver = CallerPhotoResolver(
                hasContactsPermission = {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CONTACTS,
                    ) == PackageManager.PERMISSION_GRANTED
                },
                lookupPhotoByPhone = { phone -> queryPhonePhoto(context, phone) },
                lookupPhotoByName = { name -> queryNamePhoto(context, name) },
                fallbackNotificationPhoto = { extractNotificationPhoto(context, notificationExtras) },
            )
            val bitmap = resolver.resolve(cleanName, cleanPhone)
            if (bitmap != null && cacheKey != null) {
                sharedCache.put(cacheKey, bitmap)
            }
            return bitmap
        }

        private fun extractPhoneFromPersonUri(extras: Bundle?): String? {
            if (extras == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_CALL_PERSON, android.app.Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_CALL_PERSON)
            }
            return normalizePhoneLookupInput(person?.uri)
        }

        private fun queryPhonePhoto(context: Context, phone: String): Bitmap? = runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone),
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
            )
            context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val lookupKey = cursor.getString(1)
                    val contactUri = ContactsContract.Contacts.getLookupUri(id, lookupKey)
                    openHighResPhotoStream(context, contactUri)
                } else {
                    null
                }
            }
        }.getOrNull()

        private fun queryNamePhoto(context: Context, displayName: String): Bitmap? = runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_FILTER_URI,
                Uri.encode(displayName),
            )
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
            )
            context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val lookupKey = cursor.getString(1)
                    val contactUri = ContactsContract.Contacts.getLookupUri(id, lookupKey)
                    openHighResPhotoStream(context, contactUri)
                } else {
                    null
                }
            }
        }.getOrNull()

        private fun openHighResPhotoStream(context: Context, contactUri: Uri?): Bitmap? {
            if (contactUri == null) return null
            return ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                contactUri,
                true,
            )?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }

        private fun extractNotificationPhoto(context: Context, extras: Bundle?): Bitmap? {
            if (extras == null) return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_CALL_PERSON, android.app.Person::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_CALL_PERSON)
                }
                val iconDrawable = runCatching { person?.icon?.loadDrawable(context) }.getOrNull()
                val personBitmap = drawableToBitmap(iconDrawable)
                if (personBitmap != null) return personBitmap
            }

            val largeBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_LARGE_ICON)
            }
            if (largeBitmap != null) return largeBitmap

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_LARGE_ICON, android.graphics.drawable.Icon::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_LARGE_ICON)
                }
                drawableToBitmap(runCatching { largeIcon?.loadDrawable(context) }.getOrNull())
            } else {
                null
            }
        }

        private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
            if (drawable == null) return null
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            return runCatching {
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }.getOrNull()
        }
    }
}
