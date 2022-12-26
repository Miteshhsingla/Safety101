package com.example.practicum.shakeServices


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.example.practicum.contacts.AppDatabase
import com.example.practicum.contacts.Contact
import com.example.practicum.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SensorService : Service() {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mShakeDetector: ShakeDetector

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var list = listOf<Contact>()
    private lateinit var db : AppDatabase
    private val smsManager = SmsManager.getDefault()

    override fun onBind(intent: Intent): IBinder? {

        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startMyOwnForeground() else startForeground(
            1,
            Notification()
        )
        db = AppDatabase.getInstance(this@SensorService)


        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager
            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mShakeDetector = ShakeDetector()
        mShakeDetector.setOnShakeListener(object : ShakeDetector.OnShakeListener {
            override fun onShake(count: Int) {
                if (count == 3) {

                    vibrate()


                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(
                        applicationContext
                    )

                    if (ActivityCompat.checkSelfPermission(
                            this@SensorService,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@SensorService,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient
                            .getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,null)
                            .addOnSuccessListener { location ->




                                scope.launch {
                                    list = db.contactDao().getAllContacts()
                                }


                                if (location != null) {

                                    for (c in list) {
                                        val message =
                                            """Hey, ${c.name}I am in DANGER, i need help. Please urgently reach me out. Here are my coordinates.
 http://maps.google.com/?q=${location.latitude},${location.longitude}"""
                                        smsManager.sendTextMessage(c.phoneNo, null, message, null, null)
                                    }
                                } else {
                                    val message = """
                            I am in DANGER, i need help. Please urgently reach me out.
                            GPS was turned off.Couldn't find location. Call your nearest Police Station.
                            """.trimIndent()

                                    for (c in list) {
                                        smsManager.sendTextMessage(c.phoneNo, null, message, null, null)
                                    }
                                }

                            }
                            .addOnFailureListener {
                                Log.d("Check: ", "OnFailure")
                                val message = """
                        I am in DANGER, i need help. Please urgently reach me out.
                        GPS was turned off.Couldn't find location. Call your nearest Police Station.
                        """.trimIndent()
                                for (c in list) {
                                    smsManager.sendTextMessage(c.phoneNo, null, message, null, null)
                                }
                            }
                    }

                }
            }
        })

        mSensorManager.registerListener(
            mShakeDetector,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val vibEff: VibrationEffect

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibEff = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            vibrator.cancel()
            vibrator.vibrate(vibEff)
        } else {
            vibrator.vibrate(500)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val NOTIFICATION_CHANNEL_ID = "example.permanence"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        )
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("You are protected.")
            .setContentText("We are there for you")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    override fun onDestroy() {


        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, ReactivateService::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
        job.cancel()
    }
}