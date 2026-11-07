package com.nothing.island.ai.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.nothing.island.ai.engine.IslandEvent

/**
 * Handles Wi-Fi connection and SSID change observation via ConnectivityManager (REQ-61).
 * Respects Android 10+ / 17 location permission requirements for SSID extraction.
 */
class NetworkCallbackHandler(
    private val context: Context,
    private val onEvent: (IslandEvent) -> Unit
) {
    companion object {
        private const val TAG = "NetworkCallbackHandler"
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var isRegistered = false
    private var lastConnectedState: Boolean? = null
    private var lastSsid: String? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateWifiState(network)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateWifiState(network, networkCapabilities)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            if (lastConnectedState != false) {
                lastConnectedState = false
                lastSsid = null
                Log.d(TAG, "Wi-Fi disconnected")
                onEvent(IslandEvent.WifiStateChanged(isConnected = false, ssid = null))
            }
        }
    }

    private fun updateWifiState(network: Network, caps: NetworkCapabilities? = null) {
        val capabilities = caps ?: connectivityManager?.getNetworkCapabilities(network) ?: return
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

        if (isWifi) {
            val ssid = getWifiSsid(capabilities)
            if (lastConnectedState != true || lastSsid != ssid) {
                lastConnectedState = true
                lastSsid = ssid
                Log.d(TAG, "Wi-Fi connected: ssid=$ssid")
                onEvent(IslandEvent.WifiStateChanged(isConnected = true, ssid = ssid))
            }
        }
    }

    private fun getWifiSsid(capabilities: NetworkCapabilities): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiInfo = capabilities.transportInfo as? WifiInfo
            if (wifiInfo != null) {
                val rawSsid = wifiInfo.ssid
                if (rawSsid != null && rawSsid != WifiManager.UNKNOWN_SSID && rawSsid != "<unknown ssid>") {
                    return rawSsid.removeSurrounding("\"")
                }
            }
        }

        val hasLocationPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPerm) {
            try {
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager?.connectionInfo
                val rawSsid = connectionInfo?.ssid
                if (rawSsid != null && rawSsid != WifiManager.UNKNOWN_SSID && rawSsid != "<unknown ssid>") {
                    return rawSsid.removeSurrounding("\"")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read SSID: ${e.message}")
            }
        }

        return null
    }

    fun start() {
        if (isRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            isRegistered = true
            Log.d(TAG, "Registered NetworkCallback for Wi-Fi")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NetworkCallback", e)
        }
    }

    fun stop() {
        if (!isRegistered) return
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
            isRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister NetworkCallback", e)
        }
    }
}
