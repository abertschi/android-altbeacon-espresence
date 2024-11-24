package ch.abertschi.ble;

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference

class SettingsFragment(val model: Model, val bleManager: BleManager) : PreferenceFragmentCompat() {

    private fun updateId(id: String) {
        findPreference<Preference>("beacon_summary")!!.summary =
            id
    }

    private fun updateStatus() {
        findPreference<Preference>("service_status")!!.summary =
            "on: ${model.isOn()}\nallowed: ${bleManager.isAllowed()}"
    }

    private fun startTransmission() {
        toastPowerOn()
        Log.i(BeaconApplication.TAG, "enabling service...")
        bleManager.startTransmission()
        updateStatus()
    }

    private fun stopTransmission() {
        findPreference<SwitchPreference>("power")!!.isChecked = false
        model.setOn(false)
        toastPowerOff()
        bleManager.stop()
        updateStatus()
        redrawView()
    }

    private fun toastPowerOn() {
        Toast.makeText(this.context, "${model.getId()} on", Toast.LENGTH_SHORT).show()
    }

    private fun toastPowerOff() {
        Toast.makeText(this.context, "${model.getId()} off", Toast.LENGTH_SHORT).show()
    }

    private fun redrawView() {
        // XXX: hack to redraw and update all dynamic fields
        updateId(model.getId())
        updateStatus()

        val rootView = view
        rootView?.invalidate()
        rootView?.requestLayout()
        Log.i(BeaconApplication.TAG, "redrawing view.....")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        if (model.getBeaconId().isNullOrBlank()) {
            model.setBeaconId(model.newBeaconId())
        }

        redrawView()

        bleManager.addListener {
            activity?.runOnUiThread {
                redrawView()
            }
        }

        val beaconIdView = findPreference<EditTextPreference>("beacon_id")!!
        beaconIdView.text = model.getBeaconId()

        findPreference<Preference>("beacon_summary")!!
            .setOnPreferenceClickListener {
                Log.i(BeaconApplication.TAG, "opn long click")
                val clipboard =
                    this@SettingsFragment.model.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", model.getId())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    this@SettingsFragment.model.context,
                    "Copied beacon ID to clip board", Toast.LENGTH_LONG
                ).show()
                true
            }

        // XXX: These hooks have the new state not yet commited
        // keep in mind when querying model
        findPreference<SwitchPreference>("power")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "power: $newValue")
            val on = newValue as Boolean
            model.setOn(on)

            if (on) {
                startTransmission()
            } else {
                stopTransmission()
            }
            true
        }
        findPreference<DropDownPreference>("advertisement_mode")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "advertisement_mode: $newValue")
            stopTransmission()
            true
        }
        findPreference<EditTextPreference>("tx_power")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "tx_power: $newValue")
            stopTransmission()
            true
        }
        findPreference<EditTextPreference>("beacon_id")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "beacon_id: $newValue")
            stopTransmission()
            if (!model.isValidBeaconId(newValue as String)) {
                Toast.makeText(this.context, "invalid id", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false
            }

            updateId(
                model.getId(
                    newValue as String,
                    model.getBeaconMajor(),
                    model.getBeaconMinor()
                )
            )
            true
        }
        findPreference<EditTextPreference>("beacon_major")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "beacon_major: $newValue")
            updateId(model.getId(model.getId(), newValue as String, model.getBeaconMinor()))
            stopTransmission()
            true
        }
        findPreference<EditTextPreference>("beacon_minor")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "beacon_minor: $newValue")
            updateId(model.getId(model.getId(), model.getBeaconMajor(), newValue as String))
            stopTransmission()
            true
        }
        findPreference<SwitchPreference>("wifi_only")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "wifi_only: $newValue")
            stopTransmission()
            true
        }
        findPreference<EditTextPreference>("ssid")!!.setOnPreferenceChangeListener { preference, newValue ->
            Log.i(BeaconApplication.TAG, "ssid: $newValue")
            stopTransmission()
            true
        }
    }
}