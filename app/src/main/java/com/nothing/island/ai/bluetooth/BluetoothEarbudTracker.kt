package com.nothing.island.ai.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.nothing.island.ai.engine.IslandEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Battery percentages for true wireless earbuds or Bluetooth audio accessories.
 *
 * Platform Limitation (Android):
 * - Standard AOSP exposes a single headset battery level via
 *   `android.bluetooth.device.action.BATTERY_LEVEL_CHANGED` (`extra.BATTERY_LEVEL`) when supported
 *   by the headset over HFP.
 * - Per-bud and charging-case metadata keys (`METADATA_UNTETHERED_LEFT_BATTERY` = 10,
 *   `METADATA_UNTETHERED_RIGHT_BATTERY` = 11, `METADATA_UNTETHERED_CASE_BATTERY` = 12) require
 *   `BLUETOOTH_PRIVILEGED` (system/privileged signature) on stock Android 12+ and throw
 *   `SecurityException` or return null for unprivileged third-party apps.
 * - Unsupported or unavailable fields are strictly kept as `null` ("--") and never fabricated.
 */
data class EarbudBatterySnapshot(
    val primaryPercent: Int? = null,
    val leftPercent: Int? = null,
    val rightPercent: Int? = null,
    val casePercent: Int? = null
) {
    fun hasAnyBatteryData(): Boolean =
        primaryPercent != null || leftPercent != null || rightPercent != null || casePercent != null

    /**
     * Resolves a single representative percentage when available, without inventing a value when unknown.
     */
    fun representativePercent(): Int? =
        primaryPercent ?: leftPercent ?: rightPercent ?: casePercent

    companion object {
        val UNKNOWN = EarbudBatterySnapshot()

        fun normalizePercent(raw: Int?): Int? = raw?.takeIf { it in 0..100 }
    }
}

/**
 * Connected Bluetooth audio peripheral snapshot.
 */
data class BluetoothDeviceSnapshot(
    val name: String?,
    val address: String,
    val battery: EarbudBatterySnapshot = EarbudBatterySnapshot.UNKNOWN,
    val isAudioActive: Boolean = false,
    val lastUpdatedMs: Long = 0L
) {
    val displayName: String
        get() = name?.trim()?.takeIf { it.isNotEmpty() } ?: "Bluetooth Audio"
}

/**
 * Utility helpers for parsing Bluetooth battery extras and untethered metadata safely.
 */
object EarbudBatteryParser {
    const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    const val METADATA_UNTETHERED_LEFT_BATTERY = 10
    const val METADATA_UNTETHERED_RIGHT_BATTERY = 11
    const val METADATA_UNTETHERED_CASE_BATTERY = 12

    fun parseMetadataBytes(rawBytes: ByteArray?): Int? {
        if (rawBytes == null || rawBytes.isEmpty()) return null
        val text = runCatching { rawBytes.toString(Charsets.UTF_8).trim() }.getOrNull() ?: return null
        val parsed = text.toIntOrNull() ?: return null
        return EarbudBatterySnapshot.normalizePercent(parsed)
    }

    fun mergeBattery(
        existing: EarbudBatterySnapshot,
        incoming: EarbudBatterySnapshot
    ): EarbudBatterySnapshot {
        return EarbudBatterySnapshot(
            primaryPercent = incoming.primaryPercent ?: existing.primaryPercent,
            leftPercent = incoming.leftPercent ?: existing.leftPercent,
            rightPercent = incoming.rightPercent ?: existing.rightPercent,
            casePercent = incoming.casePercent ?: existing.casePercent
        )
    }

    fun sanitizeSnapshot(
        primary: Int? = null,
        left: Int? = null,
        right: Int? = null,
        caseLevel: Int? = null
    ): EarbudBatterySnapshot {
        return EarbudBatterySnapshot(
            primaryPercent = EarbudBatterySnapshot.normalizePercent(primary),
            leftPercent = EarbudBatterySnapshot.normalizePercent(left),
            rightPercent = EarbudBatterySnapshot.normalizePercent(right),
            casePercent = EarbudBatterySnapshot.normalizePercent(caseLevel)
        )
    }
}

/**
 * Decoupled platform bridge for querying Bluetooth state and registering broadcasts cleanly in tests.
 */
interface BluetoothPlatformBridge {
    fun hasConnectPermission(): Boolean
    fun isAdapterEnabled(): Boolean
    fun queryConnectedAudioDevices(): List<BluetoothDeviceSnapshot>
    fun registerReceiver(receiver: BroadcastReceiver)
    fun unregisterReceiver(receiver: BroadcastReceiver)
}

/**
 * Production Android Bluetooth bridge enforcing permission checks and defensive SecurityException guards.
 */
class AndroidBluetoothPlatformBridge(private val context: Context) : BluetoothPlatformBridge {
    companion object {
        private const val TAG = "AndroidBtBridge"
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    override fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun isAdapterEnabled(): Boolean {
        return runCatching { bluetoothManager?.adapter?.isEnabled == true }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    override fun queryConnectedAudioDevices(): List<BluetoothDeviceSnapshot> {
        if (!hasConnectPermission() || !isAdapterEnabled()) return emptyList()
        val manager = bluetoothManager ?: return emptyList()
        val results = LinkedHashMap<String, BluetoothDeviceSnapshot>()
        val profiles = buildList {
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(BluetoothProfile.LE_AUDIO)
            }
        }
        val now = System.currentTimeMillis()
        for (profile in profiles) {
            val devices = runCatching { manager.getConnectedDevices(profile) }.getOrElse { emptyList() }
            for (device in devices) {
                val snapshot = extractSnapshot(device, isAudioActive = true, timestampMs = now)
                if (snapshot != null) {
                    val prev = results[snapshot.address]
                    results[snapshot.address] = if (prev != null) {
                        prev.copy(
                            name = snapshot.name ?: prev.name,
                            battery = EarbudBatteryParser.mergeBattery(prev.battery, snapshot.battery),
                            isAudioActive = prev.isAudioActive || snapshot.isAudioActive
                        )
                    } else {
                        snapshot
                    }
                }
            }
        }
        return results.values.toList()
    }

    override fun registerReceiver(receiver: BroadcastReceiver) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(EarbudBatteryParser.ACTION_BATTERY_LEVEL_CHANGED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to register Bluetooth receiver", e)
        }
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver) {
        runCatching { context.unregisterReceiver(receiver) }
            .onFailure { e -> Log.w(TAG, "Failed to unregister Bluetooth receiver", e) }
    }

    @SuppressLint("MissingPermission")
    internal fun extractSnapshot(
        device: BluetoothDevice,
        isAudioActive: Boolean,
        timestampMs: Long
    ): BluetoothDeviceSnapshot? {
        if (!hasConnectPermission()) return null
        val address = runCatching { device.address }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (!isLikelyAudioDevice(device)) return null
        val name = resolveDeviceName(device, address)
        val primary = queryBatteryLevelViaReflection(device)
        val left = queryMetadataBatteryViaReflection(device, EarbudBatteryParser.METADATA_UNTETHERED_LEFT_BATTERY)
        val right = queryMetadataBatteryViaReflection(device, EarbudBatteryParser.METADATA_UNTETHERED_RIGHT_BATTERY)
        val casePct = queryMetadataBatteryViaReflection(device, EarbudBatteryParser.METADATA_UNTETHERED_CASE_BATTERY)
        return BluetoothDeviceSnapshot(
            name = name,
            address = address,
            battery = EarbudBatteryParser.sanitizeSnapshot(primary, left, right, casePct),
            isAudioActive = isAudioActive,
            lastUpdatedMs = timestampMs
        )
    }

    @SuppressLint("MissingPermission")
    internal fun resolveDeviceName(device: BluetoothDevice, address: String?): String? {
        val direct = runCatching { device.name }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (direct != null) return direct
        if (!hasConnectPermission() || address.isNullOrBlank()) return null
        val bonded = runCatching { bluetoothManager?.adapter?.bondedDevices }.getOrNull().orEmpty()
        return bonded.firstOrNull { candidate ->
            runCatching { candidate.address.equals(address, ignoreCase = true) }.getOrDefault(false)
        }?.let { match ->
            runCatching { match.name }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun isLikelyAudioDevice(device: BluetoothDevice): Boolean {
        val btClass = runCatching { device.bluetoothClass }.getOrNull() ?: return true
        return when (btClass.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO,
            BluetoothClass.Device.Major.UNCATEGORIZED -> true
            else -> runCatching {
                btClass.hasService(BluetoothClass.Service.AUDIO) ||
                    btClass.hasService(BluetoothClass.Service.RENDER)
            }.getOrDefault(false)
        }
    }

    private fun queryBatteryLevelViaReflection(device: BluetoothDevice): Int? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            val raw = method.invoke(device) as? Int
            EarbudBatterySnapshot.normalizePercent(raw)
        }.getOrNull()
    }

    private fun queryMetadataBatteryViaReflection(device: BluetoothDevice, key: Int): Int? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getMetadata", Int::class.javaPrimitiveType)
            val bytes = method.invoke(device, key) as? ByteArray
            EarbudBatteryParser.parseMetadataBytes(bytes)
        }.getOrNull()
    }
}

/**
 * Tracks connected Bluetooth headsets/earbuds, handles multi-device determinism,
 * permission/adapter safety, and emits normalized [IslandEvent] updates.
 */
class BluetoothEarbudTracker(
    private val bridge: BluetoothPlatformBridge,
    private val onEvent: (IslandEvent) -> Unit,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    companion object {
        private const val TAG = "BluetoothEarbudTracker"

        private val DEVICE_COMPARATOR: Comparator<BluetoothDeviceSnapshot> =
            compareByDescending<BluetoothDeviceSnapshot> { it.isAudioActive }
                .thenByDescending { it.battery.hasAnyBatteryData() }
                .thenByDescending { it.lastUpdatedMs }
                .thenBy { it.address }

        fun selectPrimaryDevice(devices: Collection<BluetoothDeviceSnapshot>): BluetoothDeviceSnapshot? {
            return devices.sortedWith(DEVICE_COMPARATOR).firstOrNull()
        }
    }

    private val trackedDevices = ConcurrentHashMap<String, BluetoothDeviceSnapshot>()
    private var isStarted = false

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceSnapshot>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothDeviceSnapshot>> = _connectedDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<BluetoothDeviceSnapshot?>(null)
    val activeDevice: StateFlow<BluetoothDeviceSnapshot?> = _activeDevice.asStateFlow()

    private val _isPermissionMissing = MutableStateFlow(false)
    val isPermissionMissing: StateFlow<Boolean> = _isPermissionMissing.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntent(intent)
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true
        bridge.registerReceiver(receiver)
        refreshConnectedDevices(emitConnectedEvent = false)
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        bridge.unregisterReceiver(receiver)
        trackedDevices.clear()
        _connectedDevices.value = emptyList()
        _activeDevice.value = null
    }

    fun refreshConnectedDevices(emitConnectedEvent: Boolean = true) {
        if (!bridge.hasConnectPermission()) {
            _isPermissionMissing.value = true
            trackedDevices.clear()
            _connectedDevices.value = emptyList()
            _activeDevice.value = null
            Log.d(TAG, "Bluetooth permission missing; skipping device query gracefully.")
            return
        }
        _isPermissionMissing.value = false
        if (!bridge.isAdapterEnabled()) {
            handleAdapterDisabled()
            return
        }

        val current = runCatching { bridge.queryConnectedAudioDevices() }.getOrElse { emptyList() }
        trackedDevices.clear()
        current.forEach { snapshot ->
            trackedDevices[snapshot.address] = snapshot
        }
        val primary = recomputeActiveDevice()
        if (emitConnectedEvent && primary != null) {
            emitConnected(primary)
        }
    }

    @SuppressLint("MissingPermission")
    internal fun handleIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (!bridge.hasConnectPermission()) {
            _isPermissionMissing.value = true
            return
        }
        _isPermissionMissing.value = false

        when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    handleAdapterDisabled()
                } else if (state == BluetoothAdapter.STATE_ON) {
                    refreshConnectedDevices(emitConnectedEvent = true)
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = extractDevice(intent) ?: return
                val snapshot = buildSnapshotFromIntent(device, isAudioActive = false)
                if (snapshot != null) onDeviceConnected(snapshot)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = extractDevice(intent) ?: return
                val address = runCatching { device.address }.getOrNull() ?: return
                val bridgeImpl = bridge as? AndroidBluetoothPlatformBridge
                val name = bridgeImpl?.resolveDeviceName(device, address) ?: runCatching { device.name }.getOrNull()
                onDeviceDisconnected(address = address, nameFallback = name)
            }

            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                val device = extractDevice(intent) ?: return
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    val snapshot = buildSnapshotFromIntent(device, isAudioActive = true)
                    if (snapshot != null) onDeviceConnected(snapshot)
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    val address = runCatching { device.address }.getOrNull() ?: return
                    val bridgeImpl = bridge as? AndroidBluetoothPlatformBridge
                    val name = bridgeImpl?.resolveDeviceName(device, address) ?: runCatching { device.name }.getOrNull()
                    onDeviceDisconnected(address = address, nameFallback = name)
                }
            }

            EarbudBatteryParser.ACTION_BATTERY_LEVEL_CHANGED -> {
                val device = extractDevice(intent) ?: return
                val address = runCatching { device.address }.getOrNull() ?: return
                val rawLevel = intent.getIntExtra(EarbudBatteryParser.EXTRA_BATTERY_LEVEL, -1)
                val bridgeImpl = bridge as? AndroidBluetoothPlatformBridge
                val name = bridgeImpl?.resolveDeviceName(device, address) ?: runCatching { device.name }.getOrNull()
                onBatteryUpdated(
                    address = address,
                    nameFallback = name,
                    incomingBattery = EarbudBatteryParser.sanitizeSnapshot(primary = rawLevel)
                )
            }
        }
    }

    fun onDeviceConnected(snapshot: BluetoothDeviceSnapshot) {
        val now = timeProvider()
        val prev = trackedDevices[snapshot.address]
        val merged = if (prev != null) {
            prev.copy(
                name = snapshot.name?.takeIf { it.isNotBlank() } ?: prev.name,
                battery = EarbudBatteryParser.mergeBattery(prev.battery, snapshot.battery),
                isAudioActive = prev.isAudioActive || snapshot.isAudioActive,
                lastUpdatedMs = now
            )
        } else {
            snapshot.copy(lastUpdatedMs = if (snapshot.lastUpdatedMs > 0L) snapshot.lastUpdatedMs else now)
        }
        val prevPrimary = _activeDevice.value
        trackedDevices[merged.address] = merged
        val newPrimary = recomputeActiveDevice()

        val isNewConnection = prev == null
        val becamePrimary = newPrimary?.address == merged.address && prevPrimary?.address != merged.address
        val gainedAudioOrBattery = merged.address == newPrimary?.address &&
            ((!prev?.isAudioActive.orFalse() && merged.isAudioActive) ||
                (!prev?.battery?.hasAnyBatteryData().orFalse() && merged.battery.hasAnyBatteryData()))

        if (newPrimary != null && (isNewConnection || becamePrimary || gainedAudioOrBattery)) {
            emitConnected(newPrimary)
        }
    }

    fun onBatteryUpdated(
        address: String,
        nameFallback: String? = null,
        incomingBattery: EarbudBatterySnapshot
    ) {
        val prev = trackedDevices[address]
        if (prev == null) {
            if (!incomingBattery.hasAnyBatteryData()) return
            onDeviceConnected(
                BluetoothDeviceSnapshot(
                    name = nameFallback,
                    address = address,
                    battery = incomingBattery,
                    isAudioActive = true,
                    lastUpdatedMs = timeProvider()
                )
            )
            return
        }
        val mergedBattery = EarbudBatteryParser.mergeBattery(prev.battery, incomingBattery)
        if (mergedBattery == prev.battery) return
        val updated = prev.copy(
            name = nameFallback?.takeIf { it.isNotBlank() } ?: prev.name,
            battery = mergedBattery,
            lastUpdatedMs = timeProvider()
        )
        trackedDevices[address] = updated
        val primary = recomputeActiveDevice()
        if (primary?.address == address) {
            emitConnected(primary)
        }
    }

    fun onDeviceDisconnected(address: String, nameFallback: String? = null) {
        val removed = trackedDevices.remove(address)
        val prevPrimary = _activeDevice.value
        val nextPrimary = recomputeActiveDevice()

        if (removed != null || prevPrimary?.address == address) {
            val resolvedName = removed?.displayName
                ?: nameFallback?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Bluetooth Audio"
            onEvent(
                IslandEvent.BluetoothDeviceDisconnected(
                    id = "bt_disc_${address}_${timeProvider()}",
                    deviceName = resolvedName,
                    deviceAddress = address
                )
            )
            if (nextPrimary != null) {
                emitConnected(nextPrimary)
            }
        }
    }

    fun handleAdapterDisabled() {
        val prevPrimary = _activeDevice.value
        trackedDevices.clear()
        _connectedDevices.value = emptyList()
        _activeDevice.value = null
        if (prevPrimary != null) {
            onEvent(
                IslandEvent.BluetoothDeviceDisconnected(
                    id = "bt_adapter_off_${prevPrimary.address}_${timeProvider()}",
                    deviceName = prevPrimary.displayName,
                    deviceAddress = prevPrimary.address
                )
            )
        }
    }

    private fun recomputeActiveDevice(): BluetoothDeviceSnapshot? {
        val ordered = trackedDevices.values.sortedWith(DEVICE_COMPARATOR)
        _connectedDevices.value = ordered
        val primary = ordered.firstOrNull()
        _activeDevice.value = primary
        return primary
    }

    private fun emitConnected(snapshot: BluetoothDeviceSnapshot) {
        onEvent(
            IslandEvent.BluetoothDeviceConnected(
                id = "bt_conn_${snapshot.address}_${timeProvider()}",
                deviceName = snapshot.displayName,
                deviceAddress = snapshot.address,
                batteryLevelPercentage = snapshot.battery.representativePercent(),
                leftBatteryPercentage = snapshot.battery.leftPercent,
                rightBatteryPercentage = snapshot.battery.rightPercent,
                caseBatteryPercentage = snapshot.battery.casePercent
            )
        )
    }

    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildSnapshotFromIntent(device: BluetoothDevice, isAudioActive: Boolean): BluetoothDeviceSnapshot? {
        if (!bridge.hasConnectPermission()) return null
        val bridgeImpl = bridge as? AndroidBluetoothPlatformBridge
        if (bridgeImpl != null) {
            return bridgeImpl.extractSnapshot(device, isAudioActive, timeProvider())
        }
        val address = runCatching { device.address }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val name = runCatching { device.name }.getOrNull()
        return BluetoothDeviceSnapshot(
            name = name,
            address = address,
            battery = EarbudBatterySnapshot.UNKNOWN,
            isAudioActive = isAudioActive,
            lastUpdatedMs = timeProvider()
        )
    }

    private fun Boolean?.orFalse(): Boolean = this == true
}
