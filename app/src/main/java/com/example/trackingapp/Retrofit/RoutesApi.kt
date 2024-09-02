package com.example.trackingapp.Retrofit

import com.example.trackingapp.model.RouteResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RoutesApi {
    @GET("directions/json")
     fun getRoutes(@Query ("origin") origin: String,
                          @Query("destination") destination: String,
                          @Query("key") apiKey: String): Call<RouteResponse>


}