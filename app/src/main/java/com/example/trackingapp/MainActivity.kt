package com.example.trackingapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationUpdateInterval: Long = 10 * 1000 // 10 seconds
    private val handler = Handler(Looper.getMainLooper())
    private var lastLocation: Location? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LocationDataAdapter
    private val locationData = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView and Adapter
        recyclerView = findViewById(R.id.recyclerView)
        adapter = LocationDataAdapter(locationData)
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

    private fun startLocationUpdates() {
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationUpdateInterval)
            .setMinUpdateDistanceMeters(0f) // Optional: Update location if the device moves 0 meters
            .build()

        // Initialize LocationCallback
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                lastLocation = locationResult.lastLocation
                Log.d("TrackingApp", "Lat: ${lastLocation?.latitude}, Lon: ${lastLocation?.longitude}")
//                showToast("Lat: ${lastLocation?.latitude}, Lon: ${lastLocation?.longitude}", Toast.LENGTH_SHORT)
                lastLocation?.let {
                    // Add the location to the RecyclerView
                    adapter.addLocation(it)
                }
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // Show toast every minute
        handler.post(object : Runnable {
            override fun run() {
                lastLocation?.let {
                    showToast("Lat: ${it.latitude}, Lon: ${it.longitude}", Toast.LENGTH_SHORT)
                } ?: run {
                    showToast("No location available", Toast.LENGTH_SHORT)
                    Log.d("TrackingApp", "No location available")
                }
                handler.postDelayed(this, 60 * 1000) // 60 seconds
            }
        })
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
}
