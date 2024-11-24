package ch.abertschi.ble

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

class SsidManager(
    private val context: Context
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var started = false

    var onConnectedCb: (() -> Unit)? = null
        get() = field
        set(value) {
            field = value
        }
    var onDisconnectedCb: (() -> Unit)? = null
        get() = field
        set(value) {
            field = value
        }

    fun getCurrentSsid(): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                return wifiInfo.ssid.trim('"')
            }
        }
        return null
    }


    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.i(BeaconApplication.TAG, "capabilities changed")
            super.onCapabilitiesChanged(network, networkCapabilities)
            val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            if (!isWifi) {
                Log.i(BeaconApplication.TAG, "power off Wi-Fi")
                onDisconnectedCb?.invoke()
            } else {
                val ssid = getCurrentSsid()
                if (!ssid.isNullOrEmpty()) {
                    // XXX: This is redundant, we end up with too many calls
                    onConnectedCb?.invoke()
                }
            }
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                Log.i(BeaconApplication.TAG, "Connected to Wi-Fi")
                this@SsidManager.onConnectedCb?.invoke()
            }

        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.i(BeaconApplication.TAG, "Wi-Fi disconnected")
            this@SsidManager.onDisconnectedCb?.invoke()
        }
    }

    fun startMonitoring() {
        if (started) {
            Log.i(BeaconApplication.TAG, "already started")
            return
        }
        Log.i(BeaconApplication.TAG, "start monitoring in ssid manager")
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        started = true
    }

    fun stopMonitoring() {
        if (started) {
            Log.i(BeaconApplication.TAG, "stop monitoring in ssid manager")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        started = false
    }
}