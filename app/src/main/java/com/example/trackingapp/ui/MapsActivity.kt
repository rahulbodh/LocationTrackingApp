package com.example.trackingapp.ui

import android.graphics.Color
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
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

        getRoutes("Kashmir", "Bengaluru,", apiKey)



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
                    val routeResponse = response.body()!!
                    // Extract polyline points from the response
                    val routePoints = routeResponse.routes.firstOrNull()?.overviewPolyline?.points
                    if (routePoints != null) {
                        drawRouteOnMap(routePoints)
                    } else {
                        Log.e("MapsActivity", "No route found")
                    }

                } else {
                    Log.e("MainActivity", "Response failed")
                }
            }

            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Log.e("MainActivity", "Error: ${t.message}")
            }
        })
    }

    private fun drawRouteOnMap(encodedPolyline: String) {
        val polyline = PolylineOptions().addAll(decodePolyline(encodedPolyline)).width(10f).color(Color.BLUE)
        mMap.addPolyline(polyline)

        // Move camera to show the entire route
        val boundsBuilder = LatLngBounds.builder()
        decodePolyline(encodedPolyline).forEach {
            boundsBuilder.include(it)
        }
        val bounds = boundsBuilder.build()
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker at the initial location (Saharanpur)
        val initialLocation = LatLng(lat.toDouble(), lon.toDouble())
        mMap.addMarker(MarkerOptions().position(initialLocation).title("Starting Point"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 10f))
    }
}