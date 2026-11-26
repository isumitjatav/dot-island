package com.nothing.island.ai.media

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandEventEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MediaSessionTracker
 *
 * Integrates with Android MediaSessionManager via standard AOSP APIs to manage active media sessions
 * from Spotify, YouTube Music, Apple Music, Audible, and compatible Android media apps.
 *
 * Responsibilities:
 * - Detects active media controllers and listens for session registration/destruction.
 * - Extracts real-time metadata (title, artist, album, artwork, duration).
 * - Tracks playback state (playing, paused, position, speed, seekable actions).
 * - Dispatches transport control commands (play, pause, next, previous, seekTo).
 * - Emits reactive [IslandEvent.MediaPlaybackChanged] to Nothing Island engine.
 * - Maintains an honest playback status: Never fabricates metadata or fake visualizer motion.
 */
/**
 * Pure state cache for background resumable playback (retained across PAUSED/STOPPED,
 * cleared strictly when the system session is destroyed / swiped from Recents).
 */
class ResumableMediaCache {
    private val _state = MutableStateFlow<MediaTrackState?>(null)
    val state: StateFlow<MediaTrackState?> = _state.asStateFlow()

    fun onTrackObserved(track: MediaTrackState) {
        if (track == MediaTrackState.EMPTY ||
            track.packageName.isBlank() ||
            track.trackTitle.isBlank()
        ) {
            return
        }
        _state.value = track.copy(
            isPlaying = false,
            playbackState = if (track.isPlaying) PlaybackState.STATE_PAUSED else track.playbackState,
            isHistorical = true,
        )
    }

    fun onPlaybackPausedOrStopped(playbackState: Int = PlaybackState.STATE_PAUSED) {
        val existing = _state.value ?: return
        _state.value = existing.copy(
            isPlaying = false,
            playbackState = playbackState,
            isHistorical = true,
        )
    }

    fun onSessionDestroyed() {
        _state.value = null
    }
}

class MediaSessionTracker(
    private val context: Context,
    private val eventEngine: IslandEventEngine,
    private val audioOutputTracker: AudioOutputDeviceTracker? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    companion object {
        private const val TAG = "MediaSessionTracker"

        @Volatile
        private var instance: MediaSessionTracker? = null

        fun getInstance(
            context: Context,
            eventEngine: IslandEventEngine,
            audioOutputTracker: AudioOutputDeviceTracker? = null
        ): MediaSessionTracker {
            return instance ?: synchronized(this) {
                instance ?: MediaSessionTracker(
                    context.applicationContext,
                    eventEngine,
                    audioOutputTracker
                ).also { instance = it }
            }
        }

        private const val LAUNCH_TAG = "MediaAppLauncher"
        const val MEDIA_LAUNCH_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

        /**
         * Returns true only when emitting to [IslandEventEngine] represents a real change in
         * active track/package/playback state rather than a pure position tick.
         */
        fun shouldEmitMediaPlaybackEvent(
            previous: MediaTrackState?,
            current: MediaTrackState
        ): Boolean {
            if (previous == null || previous == MediaTrackState.EMPTY) return true
            if (previous.packageName != current.packageName) return true
            if (previous.trackTitle != current.trackTitle) return true
            if (previous.artistName != current.artistName) return true
            if (previous.isPlaying != current.isPlaying) return true
            val wasStopped = previous.playbackState == PlaybackState.STATE_STOPPED
            val isStopped = current.playbackState == PlaybackState.STATE_STOPPED
            return wasStopped != isStopped
        }

        internal fun resolveMediaLaunchIntentWith(
            packageName: String,
            lookupLaunchIntent: (String) -> Intent?,
            applyFlags: (Intent, Int) -> Unit = { intent, flags -> intent.addFlags(flags) },
        ): Intent? {
            if (packageName.isBlank()) return null
            val rawIntent = lookupLaunchIntent(packageName) ?: return null
            applyFlags(rawIntent, MEDIA_LAUNCH_FLAGS)
            return rawIntent
        }

        fun resolveMediaLaunchIntent(packageManager: PackageManager, packageName: String): Intent? =
            resolveMediaLaunchIntentWith(
                packageName = packageName,
                lookupLaunchIntent = { pkg -> packageManager.getLaunchIntentForPackage(pkg) },
            )

        fun launchMediaPlayer(
            context: Context,
            packageName: String,
            sessionActivity: PendingIntent? = null,
        ): Boolean {
            if (sessionActivity != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val options = ActivityOptions.makeBasic()
                            .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                            .toBundle()
                        sessionActivity.send(context, 0, null, null, null, null, options)
                    } else {
                        sessionActivity.send()
                    }
                    Log.d(LAUNCH_TAG, "Launched media player via sessionActivity for $packageName")
                    return true
                } catch (e: Exception) {
                    Log.w(LAUNCH_TAG, "sessionActivity send failed for $packageName, falling back", e)
                }
            }

            if (packageName.isNotBlank()) {
                try {
                    val launchIntent = resolveMediaLaunchIntent(context.packageManager, packageName)
                    if (launchIntent != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val options = ActivityOptions.makeBasic()
                                .setPendingIntentBackgroundActivityStartMode(
                                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                )
                                .toBundle()
                            context.startActivity(launchIntent, options)
                        } else {
                            context.startActivity(launchIntent)
                        }
                        Log.d(LAUNCH_TAG, "Launched media player via launchIntent for $packageName")
                        return true
                    } else {
                        Log.w(LAUNCH_TAG, "No launch intent resolved for $packageName")
                    }
                } catch (e: Exception) {
                    Log.w(LAUNCH_TAG, "Fallback startActivity failed for $packageName", e)
                }
            }
            return false
        }
    }

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val packageManager: PackageManager = context.packageManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentTrack = MutableStateFlow(MediaTrackState.EMPTY)
    val currentTrack: StateFlow<MediaTrackState> = _currentTrack.asStateFlow()

    private val resumableCache = ResumableMediaCache()
    val lastResumableMedia: StateFlow<MediaTrackState?> = resumableCache.state

    private var activeController: MediaController? = null
    private var lastResumableController: MediaController? = null
    private var activeControllerCompat: MediaControllerCompat? = null
    private var lastKnownValidTrack: MediaTrackState? = null

    private val resumableDestroyCallback = object : MediaController.Callback() {
        override fun onSessionDestroyed() {
            Log.i(TAG, "Cached resumable media session destroyed (app swiped from Recents).")
            clearResumableSession()
        }
    }

    private val controllerCompatCallback = object : MediaControllerCompat.Callback() {
        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.d(TAG, "ControllerCompat callback: onRepeatModeChanged: $repeatMode")
            updateFromActiveController()
        }
    }

    private val preferencesRepository: IslandPreferencesRepository by lazy {
        IslandPreferencesRepository.getInstance(context.applicationContext)
    }

    init {
        scope.launch {
            try {
                preferencesRepository.liveMusicPreferences.collect {
                    mainHandler.post {
                        updateFromActiveController()
                    }
                }
            } catch (_: Exception) {
                // Safety catch for unit test environments without full Application context
            }
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "Controller callback: onMetadataChanged")
            updateFromActiveController()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Controller callback: onPlaybackStateChanged: state=${state?.state}")
            updateFromActiveController()
        }

        override fun onSessionDestroyed() {
            Log.i(TAG, "Active media session destroyed.")
            handleSessionDestroyed()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            Log.d(TAG, "Active sessions changed. Found ${controllers?.size ?: 0} sessions.")
            onActiveSessionsListChanged(controllers)
        }

    private var isRegistered = false

    /**
     * Initializes active session discovery using the NotificationListenerService component identity.
     */
    fun startTracking(listenerComponent: ComponentName) {
        if (isRegistered || mediaSessionManager == null) return
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent,
                mainHandler
            )
            isRegistered = true
            val initialSessions = mediaSessionManager.getActiveSessions(listenerComponent)
            onActiveSessionsListChanged(initialSessions)
            Log.i(TAG, "MediaSessionTracker started successfully with component: $listenerComponent")
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing notification listener permission to query MediaSessions", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaSessionTracker", e)
        }
    }

    /**
     * Handles changes in the list of active media sessions.
     */
    private fun onActiveSessionsListChanged(controllers: List<MediaController>?) {
        val excluded = try {
            preferencesRepository.liveMusicPreferences.value.excludedPackages
        } catch (_: Exception) {
            emptySet()
        }

        val eligible = controllers?.filter { controller ->
            val pkg = controller.packageName ?: ""
            !excluded.contains(pkg)
        }

        if (eligible.isNullOrEmpty()) {
            val prevController = activeController
            activeController?.unregisterCallback(controllerCallback)
            activeController = null
            if (prevController != null && prevController == lastResumableController) {
                runCatching { prevController.registerCallback(resumableDestroyCallback, mainHandler) }
            }
            setStandbyState()
            return
        }

        // Prioritize currently playing controller, or fallback to first available
        val bestController = eligible.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: eligible.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: eligible.first()

        if (bestController != activeController) {
            if (lastResumableController != null && lastResumableController != bestController) {
                runCatching { lastResumableController?.unregisterCallback(resumableDestroyCallback) }
            }
            activeController?.unregisterCallback(controllerCallback)
            activeControllerCompat?.unregisterCallback(controllerCompatCallback)
            activeController = bestController
            lastResumableController = bestController
            activeControllerCompat = try {
                MediaSessionCompat.Token.fromToken(bestController.sessionToken)?.let { token ->
                    MediaControllerCompat(context, token).also {
                        it.registerCallback(controllerCompatCallback, mainHandler)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create MediaControllerCompat for ${bestController.packageName}", e)
                null
            }
            activeController?.registerCallback(controllerCallback, mainHandler)
            Log.d(TAG, "Selected new active MediaController: ${bestController.packageName}")
        }

        updateFromActiveController()
    }

    /**
     * Inspects the active controller and computes the updated [MediaTrackState].
     */
    fun updateFromActiveController() {
        val controller = activeController
        if (controller == null) {
            setStandbyState()
            return
        }

        val excluded = try {
            preferencesRepository.liveMusicPreferences.value.excludedPackages
        } catch (_: Exception) {
            emptySet()
        }
        val pkg = controller.packageName ?: ""
        if (excluded.contains(pkg)) {
            Log.d(TAG, "Active controller $pkg is blacklisted, dropping controller")
            activeController?.unregisterCallback(controllerCallback)
            activeController = null
            setStandbyState()
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val sessionActivity = controller.sessionActivity ?: activeControllerCompat?.sessionActivity
        val repeatMode = activeControllerCompat?.repeatMode ?: PlaybackStateCompat.REPEAT_MODE_NONE

        val rawTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val rawArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
        val rawAlbum = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val onlyShowWhilePlaying = try {
            preferencesRepository.liveMusicPreferences.value.onlyShowWhilePlaying
        } catch (_: Exception) {
            true
        }
        val positionMs = playbackState?.position?.coerceAtLeast(0L) ?: 0L
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val playbackSpeed = playbackState?.playbackSpeed ?: 1.0f

        val actions = playbackState?.actions ?: 0L
        val canSeek = (actions and PlaybackState.ACTION_SEEK_TO) != 0L
        val canSkipNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L
        val canSkipPrev = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L || canSeek

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg.substringAfterLast('.').uppercase()
        }

        // Title and artist fallback
        val trackTitle = if (!rawTitle.isNullOrBlank()) rawTitle else "Audio Track"
        val artistName = if (!rawArtist.isNullOrBlank()) rawArtist else appName

        // Extract and cache artwork bitmap
        val artworkKey = "${pkg}_${trackTitle}_${artistName}"
        var cachedBitmap = MediaArtworkCache.get(artworkKey)
        var dominantColor = MediaArtworkCache.getDominantColor(artworkKey)

        if (cachedBitmap == null) {
            val extractedBitmap: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

            if (extractedBitmap != null) {
                cachedBitmap = extractedBitmap
                dominantColor = MediaArtworkCache.put(artworkKey, extractedBitmap)
            }
        }

        val audioOutput = audioOutputTracker?.currentOutputName?.value ?: "Phone Speaker"

        val updatedState = MediaTrackState(
            sessionId = controller.sessionToken.toString(),
            packageName = pkg,
            appDisplayName = appName,
            trackTitle = trackTitle,
            artistName = artistName,
            albumName = rawAlbum,
            artworkBitmap = cachedBitmap,
            artworkKey = artworkKey,
            dominantColor = dominantColor,
            isPlaying = isPlaying,
            playbackState = playbackState?.state ?: PlaybackState.STATE_NONE,
            durationMs = durationMs,
            currentPositionMs = positionMs,
            playbackSpeed = playbackSpeed,
            lastPositionUpdateTime = System.currentTimeMillis(),
            canSeek = canSeek,
            canSkipNext = canSkipNext,
            canSkipPrevious = canSkipPrev,
            audioOutputName = audioOutput,
            isHistorical = false,
            sessionActivity = sessionActivity,
            repeatMode = repeatMode
        )

        lastResumableController = controller
        if (!rawTitle.isNullOrBlank()) {
            resumableCache.onTrackObserved(updatedState)
        } else {
            resumableCache.onPlaybackPausedOrStopped(playbackState?.state ?: PlaybackState.STATE_PAUSED)
        }

        if (onlyShowWhilePlaying && !isPlaying) {
            Log.d(TAG, "onlyShowWhilePlaying is enabled and audio is not playing for $pkg -> standby (resumable retained)")
            lastKnownValidTrack = updatedState
            setStandbyState()
            return
        }

        val previous = _currentTrack.value
        val trackChanged = !updatedState.isSameTrack(previous)

        _currentTrack.value = updatedState
        lastKnownValidTrack = updatedState

        if (!shouldEmitMediaPlaybackEvent(previous = previous, current = updatedState)) {
            return
        }

        // Emit to Nothing Island event engine only when track metadata or playback state changes
        scope.launch {
            if (isPlaying || playbackState?.state == PlaybackState.STATE_PAUSED) {
                eventEngine.emit(
                    IslandEvent.MediaPlaybackChanged(
                        id = "media_${pkg}",
                        packageName = pkg,
                        trackTitle = trackTitle,
                        artistName = artistName,
                        isPlaying = isPlaying,
                        durationMs = durationMs,
                        currentPositionMs = positionMs
                    )
                )
            } else if (playbackState?.state == PlaybackState.STATE_STOPPED) {
                eventEngine.emit(IslandEvent.MediaPlaybackStopped(id = "media_stop_${pkg}", packageName = pkg))
            }
        }
    }

    /**
     * Handles active session destroyed event (e.g., user swipes music app away from Recents).
     */
    private fun handleSessionDestroyed() {
        activeController?.unregisterCallback(controllerCallback)
        activeControllerCompat?.unregisterCallback(controllerCompatCallback)
        activeController = null
        activeControllerCompat = null
        clearResumableSession()
    }

    private fun clearResumableSession() {
        runCatching { lastResumableController?.unregisterCallback(resumableDestroyCallback) }
        lastResumableController = null
        lastKnownValidTrack = null
        resumableCache.onSessionDestroyed()
        setStandbyState()
    }

    /**
     * Reverts to honest standby state with last-known track information.
     */
    private fun setStandbyState() {
        val last = lastKnownValidTrack
        val standbyState = if (last != null) {
            last.copy(
                isPlaying = false,
                playbackState = PlaybackState.STATE_STOPPED,
                isHistorical = true
            )
        } else {
            MediaTrackState.EMPTY
        }

        _currentTrack.value = standbyState

        scope.launch {
            eventEngine.emit(
                IslandEvent.MediaPlaybackStopped(
                    id = "media_stopped_${System.currentTimeMillis()}",
                    packageName = last?.packageName ?: ""
                )
            )
        }
    }

    // =========================================================================
    // TRANSPORT CONTROLS
    // =========================================================================

    fun play() {
        resumeLastMedia()
    }

    /**
     * Resumes playback via the cached/active [MediaController.TransportControls.play],
     * falling back to targeted [KeyEvent.KEYCODE_MEDIA_PLAY] if transport controls are unavailable.
     */
    fun resumeLastMedia(): Boolean {
        val controller = activeController ?: lastResumableController
        val targetPackage = _lastResumableMediaPackage()
        if (controller != null) {
            try {
                controller.transportControls.play()
                Log.d(TAG, "Transport action: play via controller ($targetPackage)")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Controller transportControls.play failed; falling back to KEYCODE_MEDIA_PLAY", e)
            }
        }
        return dispatchMediaPlayKeyFallback(targetPackage)
    }

    private fun _lastResumableMediaPackage(): String =
        lastResumableMedia.value?.packageName
            ?.takeIf { it.isNotBlank() }
            ?: _currentTrack.value.packageName

    private fun dispatchMediaPlayKeyFallback(targetPackage: String): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val eventTime = SystemClock.uptimeMillis()
        var dispatched = false
        if (audioManager != null) {
            runCatching {
                audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
                audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
                dispatched = true
                Log.d(TAG, "Fallback KEYCODE_MEDIA_PLAY dispatched via AudioManager")
            }.onFailure { e -> Log.w(TAG, "AudioManager KEYCODE_MEDIA_PLAY dispatch failed", e) }
        }
        if (targetPackage.isNotBlank()) {
            runCatching {
                val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(targetPackage)
                    putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
                }
                context.sendBroadcast(upIntent)
                dispatched = true
            }
        }
        return dispatched
    }

    fun pause() {
        try {
            activeController?.transportControls?.pause()
            Log.d(TAG, "Transport action: pause")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute pause", e)
        }
    }

    fun togglePlayPause() {
        if (_currentTrack.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun skipToNext() {
        try {
            activeController?.transportControls?.skipToNext()
            Log.d(TAG, "Transport action: skipToNext")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute skipToNext", e)
        }
    }

    fun skipToPrevious() {
        try {
            val track = _currentTrack.value
            val currentPos = track.computeCurrentPosition()

            // Standard media player UX: If played for > 3 seconds, rewind to beginning
            if (currentPos > 3000L || !track.canSkipPrevious) {
                seekTo(0L)
                Log.d(TAG, "Transport action: skipToPrevious -> rewind to 0ms (current position: $currentPos ms)")
            } else {
                activeController?.transportControls?.skipToPrevious()
                Log.d(TAG, "Transport action: skipToPrevious -> skipToPrevious() called")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute skipToPrevious, falling back to seekTo(0)", e)
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val track = _currentTrack.value
        if (!track.canSeek) {
            Log.w(TAG, "Transport action: seekTo rejected because active session does not support ACTION_SEEK_TO")
            return
        }
        val safeTarget = if (track.durationMs > 0L) {
            positionMs.coerceIn(0L, track.durationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        try {
            activeController?.transportControls?.seekTo(safeTarget)
            Log.d(TAG, "Transport action: seekTo $safeTarget ms")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute seekTo", e)
        }
    }

    /**
     * Injects a synthetic track state for deterministic testing or headless environments.
     */
    fun injectTrackForTesting(state: MediaTrackState) {
        _currentTrack.value = state
        lastKnownValidTrack = state
        if (state != MediaTrackState.EMPTY && state.trackTitle.isNotBlank()) {
            resumableCache.onTrackObserved(state)
        }
        scope.launch {
            if (state.isPlaying) {
                eventEngine.emit(
                    IslandEvent.MediaPlaybackChanged(
                        id = "media_test_${state.packageName}",
                        packageName = state.packageName,
                        trackTitle = state.trackTitle,
                        artistName = state.artistName,
                        isPlaying = state.isPlaying,
                        durationMs = state.durationMs,
                        currentPositionMs = state.currentPositionMs
                    )
                )
            } else {
                eventEngine.emit(
                    IslandEvent.MediaPlaybackStopped(
                        id = "media_test_stop",
                        packageName = state.packageName
                    )
                )
            }
        }
    }

    fun setRepeatMode(mode: Int) {
        try {
            activeControllerCompat?.transportControls?.setRepeatMode(mode)
            Log.d(TAG, "Transport action: setRepeatMode $mode")
            _currentTrack.value = _currentTrack.value.copy(repeatMode = mode)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute setRepeatMode", e)
        }
    }

    fun getActiveSessionActivity(): PendingIntent? =
        activeController?.sessionActivity
            ?: activeControllerCompat?.sessionActivity
            ?: _currentTrack.value.sessionActivity

    fun release() {
        try {
            activeController?.unregisterCallback(controllerCallback)
            activeControllerCompat?.unregisterCallback(controllerCompatCallback)
            activeController = null
            activeControllerCompat = null
            if (isRegistered && mediaSessionManager != null) {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
                isRegistered = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release MediaSessionTracker", e)
        }
    }
}
