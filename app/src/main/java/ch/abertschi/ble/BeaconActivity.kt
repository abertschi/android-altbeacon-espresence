package ch.abertschi.ble

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BeaconActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model = (application as BeaconApplication).model
        val bleManager = (application as BeaconApplication).bleManager

        checkPermissions()

        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment(model, bleManager))
            .commit()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = BeaconApplication.permissions

            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
            } else {
                startBeaconService()
            }
        } else {
            startBeaconService()
        }
    }

    private fun startBeaconService() {
        (application as BeaconApplication).bleManager.startTransmission()
    }
}