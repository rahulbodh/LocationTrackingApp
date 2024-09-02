package com.example.trackingapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.trackingapp.R
import com.example.trackingapp.R.string.maps_api_key
import com.example.trackingapp.Retrofit.RoutesApi

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.trackingapp.databinding.ActivityMapsBinding
import com.example.trackingapp.model.RouteResponse
import com.example.trackingapp.utils.TrackApp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var location: String? = null
    private var lat = "0.0"
    private var lon = "0.0"

    @Inject
     lateinit var routesApi : RoutesApi

    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inject dependencies
        (application as TrackApp).appComponent.inject(this)

        val apiKey = getString(maps_api_key)

        getRoutes("Saharanpur", "Delhi", apiKey)



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

    private  fun getRoutes(origin: String, destination: String, apiKey: String) {
        routesApi.getRoutes(origin, destination, apiKey).enqueue(object : Callback<RouteResponse> {
            override fun onResponse(call: Call<RouteResponse>, response: Response<RouteResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Handle the response
                    Log.d("MainActivity", "Route: ${response.body()}")

                } else {
                    Log.e("MainActivity", "Response failed")
                }
            }

            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Log.e("MainActivity", "Error: ${t.message}")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(lat.toDouble(), lon.toDouble())
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}