package com.example.trackingapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationUpdateInterval: Long = 1000 // 1 second
    private val handler = Handler(Looper.getMainLooper())
    private var lastLocation: Location? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LocationDataAdapter
    private val locationData = mutableListOf<String>()
    private lateinit var mySpeed: TextView
    private lateinit var myDirection: TextView

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var currentDirectionDegrees: Float = 0.0f
    private var currentDirectionName: String = "N"

    private val turnThreshold = 30f // threshold

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView and Adapter
        recyclerView = findViewById(R.id.recyclerView)
        adapter = LocationDataAdapter(locationData)
        mySpeed = findViewById(R.id.mySpeed)
        myDirection = findViewById(R.id.deviceTurn)

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize SensorManager and Rotation Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Location permission request handler
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                    startLocationUpdates()
                }

                else -> {
                    showToast("Location access denied", Toast.LENGTH_SHORT)
                    Log.d("TrackingApp", "Location access denied")
                }
            }
        }

        // Launch the permission request
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("TrackingApp", "Notification permission granted")
        } else {
            showToast("Notification permission denied", Toast.LENGTH_SHORT)
            Log.d("TrackingApp", "Notification permission denied")
        }
    }

    private fun sendNotification(latitude: Double, longitude: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "Channel_ID")
            .setContentTitle("TrackingApp")
            .setContentText("Lat: $latitude, Lon: $longitude")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = "TrackingApp"
        val descriptionText = "TrackingApp notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("Channel_ID", name, importance).apply {
            description = descriptionText
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun startLocationUpdates() {
        requestNotificationPermission()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Create location request
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationUpdateInterval)
                .setMinUpdateDistanceMeters(0f) // Optional: Update location if the device moves 0 meters
                .build()

        // Initialize LocationCallback
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                lastLocation = locationResult.lastLocation
                for (location in locationResult.locations) {
                    val velocity = location.speed * 3.6 // Convert m/s to km/h
                    mySpeed.text = String.format("%.2f km/h", velocity)
                    Log.d("TrackingApp", "Speed: $velocity km/h")

                    val directionDegree = location.bearing
                    val directionName = degreeToDirectionName(directionDegree)
                    myDirection.text = directionName
                    Log.d("TrackingApp", "Direction: $directionName")

                    updateDirectionFromGPS(location)
                }
                Log.d(
                    "TrackingApp",
                    "Lat: ${lastLocation?.latitude}, Lon: ${lastLocation?.longitude}"
                )
                lastLocation?.let {
                    // Add the location to the RecyclerView
                    adapter.addLocation(it)
                    // Send notification with latitude and longitude
                    sendNotification(it.latitude, it.longitude)
                }
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Show toast every second
        handler.post(object : Runnable {
            override fun run() {
                lastLocation?.let {
                    val velocity = it.speed * 3.6 // Convert m/s to km/h
                    mySpeed.text = String.format("%.2f km/h", velocity)
                    Log.d("TrackingApp", "Updated Speed: $velocity km/h")
                } ?: run {
                    showToast("No location available", Toast.LENGTH_SHORT)
                    Log.d("TrackingApp", "No location available")
                }
                handler.postDelayed(this, 1000) // 1 second
            }
        })
    }

    private fun updateDirectionFromGPS(location: Location?) {
        location?.let {
            val bearing = it.bearing
            currentDirectionDegrees = bearing
            currentDirectionName = degreeToDirectionName(bearing)
            myDirection.text = currentDirectionName
            Log.d(
                "TrackingApp",
                "GPS Direction Updated: $currentDirectionName ($currentDirectionDegrees°)"
            )
        }
    }

    private fun degreeToDirectionName(degrees: Float): String {
        // Use standard compass directions with finer granularity
        return when {
            degrees >= 337.5 || degrees < 22.5 -> "N"
            degrees >= 22.5 && degrees < 67.5 -> "NE"
            degrees >= 67.5 && degrees < 112.5 -> "E"
            degrees >= 112.5 && degrees < 157.5 -> "SE"
            degrees >= 157.5 && degrees < 202.5 -> "S"
            degrees >= 202.5 && degrees < 247.5 -> "SW"
            degrees >= 247.5 && degrees < 292.5 -> "W"
            degrees >= 292.5 && degrees < 337.5 -> "NW"
            else -> "N" // Fallback case, should not reach here
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener when the activity is visible
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener when the activity is not visible to save battery
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove location updates to prevent memory leaks
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacksAndMessages(null) // Remove all callbacks
    }

    private fun showToast(message: String, duration: Int) {
        Toast.makeText(this, message, duration).show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            val directionName = degreeToDirectionName(azimuthDegrees)

            if (Math.abs(currentDirectionDegrees - azimuthDegrees) > turnThreshold) {
                currentDirectionDegrees = azimuthDegrees
                currentDirectionName = directionName
                myDirection.text = directionName

                Log.d("TrackingApp", "Device Turned: $directionName ($azimuthDegrees°)")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        val velocity = location.speed * 3.6 // Convert m/s to km/h
        mySpeed.text = String.format("%.2f km/h", velocity)
        Log.d("TrackingApp", "Updated Speed: $velocity km/h")

        val directionDegree = location.bearing
        val directionName = degreeToDirectionName(directionDegree)
        myDirection.text = directionName
        Log.d("TrackingApp", "Updated Direction: $directionName")

        updateDirectionFromGPS(location)
    }
}
