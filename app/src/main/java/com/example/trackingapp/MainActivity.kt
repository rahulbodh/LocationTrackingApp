package com.example.trackingapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val locationUpdateInterval: Long = 10 * 1000 // 1 minute in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Set padding to handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Location permission request handler
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    Toast.makeText(this, "Precise location access granted", Toast.LENGTH_SHORT).show()
                    Log.d("TrackingApp", "Precise location access granted")
                    startLocationUpdates()
                }
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                    Toast.makeText(this, "Approximate location access granted", Toast.LENGTH_SHORT).show()
                    Log.d("TrackingApp", "Approximate location access granted")
                    startLocationUpdates()
                }
                else -> {
                    Toast.makeText(this, "Location access denied", Toast.LENGTH_SHORT).show()
                    Log.e("TrackingApp", "Location access denied")
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

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS is enabled", Toast.LENGTH_SHORT).show()
            Log.d("TrackingApp", "GPS is enabled")
            handler.post(locationUpdateRunnable) // Start the location updates
        } else {
            Toast.makeText(this, "GPS is disabled", Toast.LENGTH_SHORT).show()
            Log.e("TrackingApp", "GPS is disabled")
        }
    }

    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val lastKnownLocation: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                Toast.makeText(
                    this@MainActivity,
                    "Lat: ${lastKnownLocation.latitude}, Lon: ${lastKnownLocation.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("TrackingApp", "Lat: ${lastKnownLocation.latitude}, Lon: ${lastKnownLocation.longitude}")
            } else {
                Toast.makeText(this@MainActivity, "No last known location available", Toast.LENGTH_SHORT).show()
                Log.e("TrackingApp", "No last known location available")
            }

            // Schedule the next update
            handler.postDelayed(this, locationUpdateInterval)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacks(locationUpdateRunnable)
    }
}
