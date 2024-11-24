package ch.abertschi.ble

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat


class BeaconApplication() : Application() {
    companion object {
        val TAG = "ch.abertschi.ble"
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    lateinit var model: Model
    lateinit var bleManager: BleManager
    lateinit var ssidManager: SsidManager

    override fun onCreate() {
        super.onCreate()
        model = Model(this)
        ssidManager = SsidManager(this)
        bleManager = BleManager(this, model, ssidManager)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isEmpty()) {
            Log.i(TAG, "starting service...")
            val intent = Intent(this, BeaconService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } else {
            Log.i(TAG, "Not all permissions granted. cant start service...")
        }
    }
}
