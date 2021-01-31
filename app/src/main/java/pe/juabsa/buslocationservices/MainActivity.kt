package pe.juabsa.buslocationservices


import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var locationRequest: LocationRequest
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    var db = FirebaseFirestore.getInstance()


    companion object{
        var instance:MainActivity?=null

        fun getMainInstance(): MainActivity{
            return instance!!
        }
    }


    fun updateTextView(value: String){
        this@MainActivity.runOnUiThread(){
            memory_ava_text.text = value
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        instance = this

        val button = findViewById<View>(R.id.toggleButton) as ToggleButton

        // Setear escucha de acción
        button.setOnCheckedChangeListener { buttonView, isChecked ->
            val busLocationServices = Intent(
                    applicationContext, BusLocationServices::class.java
            )
            if (isChecked) {
                //startService(busLocationServices) //Iniciar servicio
                Dexter.withActivity(this)
                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(object : PermissionListener {

                            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                updateLocation()
                            }

                            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                TODO("Not yet implemented")
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                    p0: PermissionRequest?,
                                    p1: PermissionToken?
                            ) {
                                Toast.makeText(
                                        this@MainActivity,
                                        "Tu deberias aceptar este permiso",
                                        Toast.LENGTH_LONG
                                ).show()
                            }

                        }).check()
            } else {
                fusedLocationProviderClient.removeLocationUpdates(getPendindInten())
                memory_ava_text.text = "Posicion"
            }
        }
    }

    private fun updateLocation() {
        buildLocationRequest()
        fusedLocationProviderClient =  LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendindInten())

    }

    private fun getPendindInten(): PendingIntent? {
        val intent = Intent(this@MainActivity, BusLocationServices::class.java)
        intent.action = BusLocationServices.ACTION_PROCESS_UPDATE
        return PendingIntent.getBroadcast(
                this@MainActivity,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private fun buildLocationRequest(){
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        locationRequest.smallestDisplacement = 10f
    }

    private fun rellenarSpinnerEmpresa(){


    }



}