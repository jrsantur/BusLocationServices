package pe.juabsa.buslocationservices

import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import java.text.DateFormat
import java.util.*
import java.util.prefs.Preferences

class Common {

    companion object {

        val KEY_RESTQUESTING_LOCATION_UPDATES = "LocationUpdateEnable"

        fun  getLocationText(mLocation: Location):String {
            if (mLocation!=null){
                val location_string = StringBuilder(mLocation.latitude.toString())
                    .append(" - ").append(mLocation.longitude.toString()).toString()
                return location_string
            }
            return "Unknow Location"
        }

        fun getLocationTitle(myBackgroundServices: MyBackgroundServices): CharSequence? {
            return String.format(
                "Locacion actualizada: %1\$s",
                DateFormat.getDateInstance().format(Date())
            )
        }

        fun  setRequestingLocationUpdate(context: Context, value: Boolean) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_RESTQUESTING_LOCATION_UPDATES, value)
                .apply()
        }

        fun requestingLocationUpdate(context: Context ): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_RESTQUESTING_LOCATION_UPDATES, false)
        }
    }


}
