package com.example.trackingapp

import android.icu.util.LocaleData
import android.location.Location
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalTime

class LocationDataAdapter(private val data: MutableList<String>) : RecyclerView.Adapter<LocationDataAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.data_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = data[position]
        holder.textView.text = location
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addLocation(location: Location) {
        val currentTime = LocalTime.now().atDate(LocalDate.now())
        val locationString = "Lat: ${location.latitude}, Lon: ${location.longitude} at $currentTime"
        data.add(locationString)
        notifyItemInserted(data.size - 1)
    }
}
