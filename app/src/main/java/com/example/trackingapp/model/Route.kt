package com.example.trackingapp.model

import com.google.gson.annotations.SerializedName

data class Route(
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline
)
