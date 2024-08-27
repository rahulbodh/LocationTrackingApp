package com.example.trackingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.trackingapp.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var location: String? = null
    private var lat = "0.0"
    private var lon = "0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

         location = intent.getStringExtra("location")
        Log.d("MapsActivity", "Location: $location")


        val latLonRegex = "Lat: (\\d+\\.\\d+), Lon: (\\d+\\.\\d+)".toRegex()
        val matchResult = location?.let { latLonRegex.find(it) }

        lat = matchResult?.groupValues?.get(1) ?: "0.0"
        lon = matchResult?.groupValues?.get(2) ?: "0.0"

        Log.d("MapsActivity", "Lat: $lat, Lon: $lon")



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(lat.toDouble(), lon.toDouble())
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}