package com.example.trackingapp.ui

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
import android.location.Address
import android.location.Geocoder
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
import com.example.trackingapp.R
import com.example.trackingapp.adapter.LocationDataAdapter
import com.google.android.gms.location.*
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

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
    private lateinit var myPlace: TextView
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var currentDirectionDegrees: Float = 0.0f
    private var currentDirectionName: String = ""
    private var currentAzimuth: Float = 0f

    // Additional variables to track turning and speed
    private var lastAzimuth: Float = 0f
    private val turnThreshold = 30f // Angle in degrees considered as a turn
    private val speedLimit = 2f // Speed limit in km/h
    private var currentSpeedKmH = 0f
    private val accuracy = 0f
    private val lastAccuracy = 0f
    private val lastAccuracyTimestamp = 0L
    private val SPEED_LIMIT_THRESHOLD_KMH = 10f
    private val MAX_SPEED = 80f
    private val SUDDEEN_BREAK_THRESHOLD_KMH = 50f
    private val HARSE_ACCELERATION_THRESHOLD = 50f
    private val CORNERING_SPEED_THRESHOLD = 25f
    private val INTERVAL_TIME = 1f
    private var idleStartTime = 0L
    private val idleEndTime = 0L
    private var isOverspeed = false
    private var isSharpturn = false
    private var isSuddenBreak = false
    private var isHarseAcceleration = false
    private var isLateNight = false
    private var isOnCall = false
    private final val SAME_ACCURACY_THRESHOLD_MS = 5000 // 1 second
    private var onSpeedTimer: Timer? = null
    private var IdleSpeedTimer: Timer? = null
    private var isTripRequestSent = false
    private val IDLE_TIMER_TIMEOUT = 24 * 60 * 60 * 1000L
    private var isSpeedMonitoring = false
    private var LATE_NIGHT_TIME = "10:00"
    private var highSpeedStartTime: LocalDateTime? = null
    private var lastUpdateTime: Long = 0
    private var lastSpeedKmHForEvents = 0.0f
    private val speedHandler = Handler()
    private var speedRunnable: Runnable? = null
    private val SPEED_TIMER_TIMEOUT = 3000L
    private var lastSpeedExceededTime: Long =
        0L // Tracks the last time speed exceeded the threshold
    private var tripIdLastSentTime: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView and Adapter
        recyclerView = findViewById(R.id.recyclerView)
        adapter = LocationDataAdapter(locationData)
        mySpeed = findViewById(R.id.mySpeed)
        myDirection = findViewById(R.id.deviceTurn)
        myPlace = findViewById(R.id.PlaceName)

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize SensorManager and Rotation Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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

        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult == null) return

                lastLocation = locationResult.lastLocation
                for (location in locationResult.locations) {
                    val currentTime = System.currentTimeMillis()
                    lastLocation = location
                    currentSpeedKmH = location.speed * 3.6f

                    Log.d("LocationChanged", "Speed: $currentSpeedKmH km/h")
                    if (currentSpeedKmH > SPEED_LIMIT_THRESHOLD_KMH) {
                        if (idleStartTime > 0) {
                            val difference = currentTime - idleStartTime
                            Log.e("LocationService", "Idle time: $difference ms")
                            if (difference >= INTERVAL_TIME * 60 * 1000) {
                                sendTripRequest()
                                idleStartTime = 0
                                Log.e("LocationService", "Difference is greater the 2 minutes")
                            }else{
                                Log.e("LocationService", "Difference is less the 2 minutes")
                            }
                        }
                        handleSpeedAboveThreshold()
                    } else {
                        handleSpeedBelowThreshold()
                        if (idleStartTime == 0L) {
                            idleStartTime = currentTime
                        }
                    }
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
        handler.post(
            object : Runnable {
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

    private fun handleSpeedBelowThreshold() {
        stopTimerOnSpeedChange()
        startTimerOnIdleSpeed()
        stopSpeedMonitor()
        resetFlags()
    }

    private fun stopTimerOnSpeedChange() {
        if (this.onSpeedTimer != null) {
            this.onSpeedTimer!!.cancel()
            this.onSpeedTimer = null
        }
    }

    private fun startTimerOnIdleSpeed() {
        if (IdleSpeedTimer != null) {
            IdleSpeedTimer!!.cancel()
            IdleSpeedTimer!!.purge()
            IdleSpeedTimer = null
        }
        this.IdleSpeedTimer = Timer()
        this.IdleSpeedTimer!!.schedule(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                if (isTripRequestSent) {
//                    getAddress(lastLocation!!.latitude, lastLocation!!.longitude)
                    val lat = lastLocation!!.latitude.toString()
                    val lng = lastLocation!!.longitude.toString()
//                    val dateTimeUtils: DateTimeUtils = DateTimeUtils()
                    val currentTime = ZonedDateTime.now(ZoneOffset.UTC)
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    val formattedTime = currentTime.format(formatter)
                    Log.d("TripData", "TripId sent by IdleSpeed Timer")
                    isTripRequestSent = false
                }
            }
        }, IDLE_TIMER_TIMEOUT, IDLE_TIMER_TIMEOUT) // 30 minutes (1800000 milliseconds)
    }

    private fun stopSpeedMonitor() {
        if (!isSpeedMonitoring) return

        isSpeedMonitoring = false
        if (speedRunnable != null) {
            speedHandler.removeCallbacks(speedRunnable!!)
            speedRunnable = null
        }
    }

    private fun resetFlags() {
        isOverspeed = false
        isSharpturn = false
        isSuddenBreak = false
        isHarseAcceleration = false
        isLateNight = false
        isOnCall = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleSpeedAboveThreshold() {
        stopTimerOnIdleSpeed()
//        getAddress(lastLocation!!.latitude, lastLocation!!.longitude)

        if (!isTripRequestSent) {
            sendTripRequest()
        }


        startSpeedMonitor()
        checkForRiskEvents(currentSpeedKmH)

        if (currentSpeedKmH >= SPEED_LIMIT_THRESHOLD_KMH) {
//            checkPhoneUsage()
            Log.d("LocationService", "Speed limit exceeded, checking phone usage")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendTripRequest() {
        val lat = lastLocation!!.latitude.toString()
        val lng = lastLocation!!.longitude.toString()
        val currentTime = ZonedDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val formattedTime = currentTime.format(formatter)
        Log.e("LocationService", "TripId sent by sendTripRequest")
        isTripRequestSent = true
    }


    private fun stopTimerOnIdleSpeed() {
        if (this.IdleSpeedTimer != null) {
            IdleSpeedTimer!!.cancel()
            this.IdleSpeedTimer = null
        }
    }


    private fun startSpeedMonitor() {
        if (isSpeedMonitoring) return

        isSpeedMonitoring = true
        val lastSpeedChangeTime =
            longArrayOf(System.currentTimeMillis()) // Track last speed change timestamp

        speedRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()

                // Check if speed is above the threshold
                if (currentSpeedKmH > SPEED_LIMIT_THRESHOLD_KMH) {
                    // Check if the speed has remained unchanged for 10 seconds
                    if (currentSpeedKmH == lastLocation!!.speed * 3.6f &&
                        (currentTime - lastSpeedChangeTime[0]) >= 10000
                    ) { // 10 seconds in ms
                        Log.d(
                            "LocationService",
                            "Speed unchanged for 10 seconds, skipping data send"
                        )
                    } else {
                        // Update last speed change time if speed changes
                        lastSpeedChangeTime[0] = currentTime
                        Log.d("LocationService", "Data sent to server")

                    }

                    // Run the monitor again after the timeout
                    speedHandler.postDelayed(this, SPEED_TIMER_TIMEOUT) // e.g., 3 seconds
                } else {
                    // If speed drops below the threshold, continue monitoring but do not stop prematurely
                    Log.d("LocationService", "Speed below threshold, monitoring continues")
                    speedHandler.postDelayed(this, SPEED_TIMER_TIMEOUT)
                }
            }
        }

        speedHandler.post(speedRunnable as Runnable) // Start the Runnable immediately
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkForRiskEvents(currentSpeedKmH: Float) {
        val currentTime = System.currentTimeMillis()

        // Detect harsh acceleration
        if (currentTime - lastUpdateTime > 5000) {
            val speedDifference: Float = currentSpeedKmH - lastSpeedKmHForEvents

            // Detect sudden brake
            if (speedDifference < -SUDDEEN_BREAK_THRESHOLD_KMH) {
                resetFlags() // Ensure no other flags are interfering
                setFlagsForEvent(3, false, false, true, false, false, false)
                Log.d(
                    ">>>",
                    "Sudden Brake Detected: from $lastSpeedKmHForEvents to $currentSpeedKmH"
                )
            } else if (speedDifference > HARSE_ACCELERATION_THRESHOLD) {
                resetFlags() // Ensure no other flags are interfering
                setFlagsForEvent(4, false, false, false, true, false, false)
                Log.d(
                    ">>>",
                    "Harsh Acceleration Detected: from $lastSpeedKmHForEvents to $currentSpeedKmH"
                )
            }

            lastSpeedKmHForEvents = currentSpeedKmH
            lastUpdateTime = currentTime
        } else {
            resetFlags() // Reset if no events detected in the threshold period
        }

        // Detect overspeeding
        if (currentSpeedKmH >= MAX_SPEED) {
            if (highSpeedStartTime == null) {
                highSpeedStartTime = LocalDateTime.now()
            } else if (Duration.between(highSpeedStartTime, LocalDateTime.now()).seconds >= 10) {
                resetFlags() // Reset before setting a new event
                setFlagsForEvent(1, true, false, false, false, false, false)
                Log.d("riskEvent", "Risk event detected: Overspeed")
                highSpeedStartTime = null
            }
        } else {
            highSpeedStartTime = null
        }

        // Detect night driving
        try {
            val nightDrivingTime =
                LocalTime.parse(LATE_NIGHT_TIME, DateTimeFormatter.ofPattern("HH:mm"))
            if (LocalTime.now().isAfter(nightDrivingTime)) {
                resetFlags() // Reset before setting a new event
                setFlagsForEvent(5, false, false, false, false, true, false)
                Log.d(">>>", "Vehicle in use after 10 PM: $LATE_NIGHT_TIME")
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to parse night driving time: " + e.message)
        }
    }

    private fun setFlagsForEvent(
        eventNumber: Int, overspeed: Boolean, sharpturn: Boolean,
        suddenbreak: Boolean, harshacceleration: Boolean, lateNight: Boolean, onCall: Boolean
    ) {
        resetFlags()
        isOverspeed = overspeed
        isSharpturn = sharpturn
        isSuddenBreak = suddenbreak
        isHarseAcceleration = harshacceleration
        isLateNight = lateNight
        isOnCall = onCall
        Log.d("LocationService", "Event $eventNumber triggered")
    }


    private fun areAllFlagsFalse(): Boolean {
        return !isOverspeed && !isSharpturn && !isSuddenBreak && !isHarseAcceleration && !isLateNight && !isOnCall
    }

    // Method for reverse geocoding to get place name
    private fun getPlaceNameFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)!!
            if (addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                val placeName = address.getAddressLine(0)  // Full address
                val city = address.locality  // City
                val country = address.countryName  // Country

                myPlace.text = "Place: $placeName\nCity: $city\nCountry: $country"
                Log.d("TrackingApp", "Place: $placeName, City: $city, Country: $country")
            } else {
                myPlace.text = "Place: Unknown"
                Log.d("TrackingApp", "No address found for the location")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            myPlace.text = "Unable to get place name"
            Log.d("TrackingApp", "Geocoder failed", e)
        }
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
        rotationSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
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


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            currentAzimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            val azimuthDegrees = (currentAzimuth + 360) % 360

            val directionName = degreeToDirectionName(azimuthDegrees)
            myDirection.text = directionName
//            Log.d("TrackingApp", "Compass Direction: $directionName ($azimuthDegrees°)")

            // Detect turn and overspeed
            val turnAngle = Math.abs(azimuthDegrees - lastAzimuth)
//            lastAzimuth = azimuthDegrees

            lastLocation?.let {
                val speedKmH = it.speed * 3.6
                Log.d("TrackingApp", "Speed2: $speedKmH km/h")
                if (turnAngle > turnThreshold && speedKmH > speedLimit) {
                    Log.d("TrackingApp", "Turn detected while overspeeding!")
                    Log.d("TrackingApp", "Overspeed At: $turnAngle° at speed $speedKmH km/h")
                    showToast(
                        "Overspeed at : $turnAngle° at speed $speedKmH km/h",
                        Toast.LENGTH_LONG
                    )
                }
            }

            // Update the lastAzimuth value
            if (turnAngle > turnThreshold) {
                lastAzimuth = azimuthDegrees
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

    private fun Context.showToast(message: String, durationInMillis: Int) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.show()

        // Cancel the toast after the specified duration
        Handler(Looper.getMainLooper()).postDelayed({
            toast.cancel()
        }, durationInMillis.toLong())
    }
}
