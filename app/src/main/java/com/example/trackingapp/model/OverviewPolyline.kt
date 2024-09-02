package com.example.trackingapp.model

import com.google.gson.annotations.SerializedName

data class OverviewPolyline(
    @SerializedName("points")
    val points: String
)
