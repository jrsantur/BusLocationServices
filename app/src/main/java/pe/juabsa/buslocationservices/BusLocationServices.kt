package pe.juabsa.buslocationservices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.LocationResult
import java.lang.StringBuilder

class BusLocationServices: BroadcastReceiver() {

    companion object{
        val ACTION_PROCESS_UPDATE = "pe.juabsa.buslocationservices.UPDATE_LOCATION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent!=null){
            val action = intent!!.action
            if (action.equals(ACTION_PROCESS_UPDATE)){
                val result = LocationResult.extractResult(intent)

                if (result!=null){
                    val location = result.lastLocation
                    val location_string = StringBuilder(location.latitude.toString())
                        .append(" - ").append(location.longitude.toString()).toString()
                    try {
                        MainActivity.getMainInstance().updateTextView(location_string)
                    }catch (e:Exception) {
                        Toast.makeText(context, location_string, Toast.LENGTH_LONG).show()
                        Log.d("BusLocationServices", "Locacion: "+location_string)
                    }
                }
            }
        }
    }
}