package ch.abertschi.ble

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.UUID

class Model(val context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getSsid(): List<String> {
        return prefs.getString("ssid", "")!!.split(",").map { it.trim() }
    }

    fun isOn() = prefs.getBoolean("power", false)

    fun setOn(on: Boolean) = prefs.edit().putBoolean("power", on).commit()

    fun isWifi() = prefs.getBoolean("wifi_only", false)

    fun getBeaconMinor() = prefs.getString("beacon_minor", "0")!!

    fun getBeaconMajor() = prefs.getString("beacon_major", "0")!!

    fun getBeaconId() = prefs.getString("beacon_id", "")!!

    fun setBeaconId(id: String) = prefs.edit().putString("beacon_id", id).commit()

    fun getTxPower() = prefs.getString("tx_power", "-50")!!.toInt()!!

    fun isValidBeaconId(uuid: String): Boolean {
        val regex =
            Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")
        return regex.matches(uuid)
    }

    fun newBeaconId(): String {
        val id = UUID.randomUUID().toString()
        if (!isValidBeaconId(id)) {
            throw IllegalStateException("Illegal state for id")
        }
        return id
    }

    enum class AdvertisementMode(val value: String) {
        ModeLowLatency("LOW_LATENCY"), ModeLowPower("LOW_POWER"), ModeBalanced("BALANCED");
    }

    fun getAdvertisementMode() = AdvertisementMode.valueOf(
        prefs.getString(
            "advertisementMode", AdvertisementMode.ModeLowLatency.name
        )!!
    )

    fun getId(): String {
        return getId(this.getBeaconId(), this.getBeaconMajor(), this.getBeaconMinor())
    }

    fun getId(beaconId: String, major: String, minor: String): String {
        return "iBeacon:${beaconId}-${major}-${minor}"
    }

    fun getDebugInfo(): String {
        return "${getId()} ${getAdvertisementMode()} on: ${isOn()} tx: ${getTxPower()} wifi:${isWifi()} ${getSsid()}"
    }
}