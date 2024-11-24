package ch.abertschi.ble

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_BALANCED
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.BaseExpandableListAdapter
import androidx.core.content.ContextCompat
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import java.util.Arrays


class BleManager(val context: Context, val model: Model, val ssidManager: SsidManager) {

    var beaconTransmitter: BeaconTransmitter? = null

    var ssidManagerRunning = false

    fun enableSsidMonitoring() {
        if (ssidManagerRunning) {
            return
        }

        ssidManagerRunning = true
        ssidManager.onConnectedCb = {
            startTransmission()
        }
        ssidManager.onDisconnectedCb = {
            val ssid = ssidManager.getCurrentSsid()
            if (!model.getSsid().any { it.trim() == ssid?.trim() }) {
                stop()
            }
        }
        ssidManager.startMonitoring()
    }

    fun disableSsidMonitoring() {
        if (!ssidManagerRunning) {
            return
        }
        ssidManagerRunning = false
        ssidManager.onConnectedCb = null
        ssidManager.onDisconnectedCb = null
        ssidManager.stopMonitoring()
    }

    fun isTransmitting(): Boolean {
        return beaconTransmitter != null && beaconTransmitter!!.isStarted
    }

    fun isAllowed(): Boolean {
        val ssid = ssidManager.getCurrentSsid()
        val isOn = model.isOn()
        val isWifi = model.isWifi()
        println("${ssid}, ${isOn}, ${isWifi}")

        return isOn && (!isWifi || model.getSsid().any { it.trim() == ssid?.trim() })
    }

    private fun runForegroundService() {
        val intent = Intent(this.context, BeaconService::class.java)
        ContextCompat.startForegroundService(this.context, intent)
    }

    private fun stopForegroundService() {
        val stopIntent = Intent(this.context, BeaconService::class.java)
        context.stopService(stopIntent)
    }

    fun startTransmission() {
        if (model.isWifi()) {
            Log.i(BeaconApplication.TAG, "enabling ssid monitoring")
            enableSsidMonitoring()
        }
        if (isTransmitting()) {
            Log.i(BeaconApplication.TAG, "already transmitting")
            return
        }
        if (!isAllowed()) {
            Log.i(BeaconApplication.TAG, "ble manager not allowed to run")
            return
        }

        runForegroundService()
        notify(BleManagerEvent())

        Log.i(BeaconApplication.TAG, "transmission is allowed, starting..")

        beaconTransmitter = BeaconTransmitter(
            this.context,
            BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        )
        val beacon = Beacon.Builder().setId1(model.getBeaconId()).setId2(model.getBeaconMajor())
            .setId3(model.getBeaconMinor()).setManufacturer(0x004c).setTxPower(model.getTxPower())
            .setDataFields(Arrays.asList(*arrayOf(0L))).build()

        var mode = ADVERTISE_MODE_BALANCED
        when (model.getAdvertisementMode()) {
            Model.AdvertisementMode.ModeLowLatency -> mode = ADVERTISE_MODE_LOW_LATENCY
            Model.AdvertisementMode.ModeLowPower -> mode = ADVERTISE_MODE_LOW_POWER
            Model.AdvertisementMode.ModeBalanced -> mode = ADVERTISE_MODE_BALANCED
            else -> mode = ADVERTISE_MODE_BALANCED
        }

        beaconTransmitter!!.advertiseMode = mode
        Log.i(BeaconApplication.TAG, "mode: " + mode)
        beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {

            override fun onStartFailure(errorCode: Int) {
                Log.e(BeaconApplication.TAG, "on start failure: " + errorCode)
                super.onStartFailure(errorCode)
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.e(BeaconApplication.TAG, "on start success")
                super.onStartSuccess(settingsInEffect)
            }
        })
    }

    fun stop() {
        if (isTransmitting()) {
            beaconTransmitter?.stopAdvertising()
            beaconTransmitter = null
        }
        notify(BleManagerEvent())
        disableSsidMonitoring()
        stopForegroundService()
    }

    class BleManagerEvent {}

    private val listeners = mutableListOf<(BleManagerEvent) -> Unit>()

    fun addListener(listener: (BleManagerEvent) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (BleManagerEvent) -> Unit) {
        listeners.remove(listener)
    }

    private fun notify(event: BleManagerEvent) {
        for (listener in listeners) {
            listener(event)
        }
    }
}