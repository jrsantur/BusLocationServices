package pe.juabsa.buslocationservices

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import org.greenrobot.eventbus.EventBus


class MyBackgroundServices : Service() {

    private val EXTRA_STARTED_FROM_NOTIFICATION: String? = "pe.juabsa.buslocationservices.started_from_notification"
    private val CHANEL_ID = "my_chanel"
    val TAG = MyBackgroundServices::class.java.name

    private val UPDATE_INTERVAL_IN_MIL = 1000
    private val FASTTEST_UPDATE_INTERVAL_IN_MIL = UPDATE_INTERVAL_IN_MIL/2
    private val NOTI_ID = 1234
    private var mChangingConfiguration = false;

    lateinit var mServicesHandler: Handler
    lateinit var nNotificationManager:NotificationManager
    lateinit var locationRequest: LocationRequest
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationCallback: LocationCallback
    lateinit var mlocation: Location
    private  var context: Context? = null
    private val mBinder:IBinder  = LocalBinder()


    class LocalBinder : Binder() {
        fun getService(con: Context): MyBackgroundServices {
            MyBackgroundServices().context = con
            return MyBackgroundServices()
        }
    }

    override fun onCreate() {

        fusedLocationProviderClient =  LocationServices.getFusedLocationProviderClient(this)

        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }

        createLocationequest()
        getlastLocation()
        var  handlerThread:HandlerThread = HandlerThread("EDMTDev")
        handlerThread.start()
        mServicesHandler = Handler(handlerThread.getLooper())
        nNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
           var mChanel = NotificationChannel(
                   CHANEL_ID,
                   getString(R.string.app_name),
                   NotificationManager.IMPORTANCE_DEFAULT
           )
            nNotificationManager.createNotificationChannel(mChanel)

        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var starFormatNotificaction = intent!!.getBooleanExtra(
                EXTRA_STARTED_FROM_NOTIFICATION,
                false
        )
        if (starFormatNotificaction){
            removeLocationUpdate()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    fun removeLocationUpdate() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            Common.setRequestingLocationUpdate(this@MyBackgroundServices, false)
            stopSelf()

        }catch (ex: SecurityException){
            Common.setRequestingLocationUpdate(this@MyBackgroundServices, true)
            Log.e("EDMT_DEV", "Lost location permission. Cloud not remove update " + ex)
        }
    }

    private fun getlastLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    mlocation = task.result
                } else {
                    Log.e("EDMT DEV", "Error al obtener la ubicacion")
                }
            }



        }catch (ex: SecurityException){
            Log.e("EDMT DEV", "Lost Location permission  " + ex)
        }
    }

    private fun createLocationequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 10000
        locationRequest.smallestDisplacement = 10f
    }

    private fun onNewLocation(lastLocation: Location) {
        mlocation = lastLocation
        EventBus.getDefault().postSticky(SendLocationToActivity(mlocation));

        if(serviceIsRunningInForeground()){
            nNotificationManager.notify(NOTI_ID, getNotification())
        }
    }

    private fun getNotification(): Notification? {
        var intent = Intent(this, MyBackgroundServices::class.java)
        val text: String = Common.getLocationText(mlocation)

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
        var servicePending = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        var activityPendingIntent = PendingIntent.getActivity(
                this, 0, Intent(
                this,
                MainActivity::class.java
        ), 0
        )
        var builder: NotificationCompat.Builder = NotificationCompat.Builder(this)
            .addAction(R.drawable.ic_baseline_launch, "Abrir", activityPendingIntent)
            .addAction(R.drawable.ic_baseline_cancel, "Cerrar", servicePending)
            .setContentText(text)
            .setContentTitle(Common.getLocationTitle(this))
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(System.currentTimeMillis().toInt())
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            builder.setChannelId(CHANEL_ID)
        }
        return builder.build()
    }

    private fun serviceIsRunningInForeground(): Boolean {
        val manager:ActivityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if(TAG.equals(service.service.className)){
                if(service.foreground){
                    return true
                }
            }
        }

        return false;
    }


    override fun onBind(intent: Intent?): IBinder? {
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        mChangingConfiguration = true
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!mChangingConfiguration && Common.requestingLocationUpdate(this) ){
            startForeground(NOTI_ID, getNotification())
        }
        return true
    }

    override fun onDestroy() {
        mServicesHandler.removeCallbacks{null}
        super.onDestroy()
    }

    fun requestLocationUpdates(context: Context) {
        //Common.setRequestingLocationUpdate(this@MyBackgroundServices,true)
        startService(Intent(context, MyBackgroundServices::class.java))
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

        }catch (ex: SecurityException){
            Log.e("EDMT_DEV", "Permiso de ubicación perdido. No pude solicitarlo: " + ex)
        }
    }

}