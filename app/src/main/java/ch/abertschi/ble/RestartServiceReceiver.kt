package ch.abertschi.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        (context.applicationContext as BeaconApplication).bleManager.startTransmission()
    }
}