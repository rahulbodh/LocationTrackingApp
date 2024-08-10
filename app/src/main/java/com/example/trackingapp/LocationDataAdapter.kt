package com.example.trackingapp

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationDataAdapter(private val data: MutableList<String>) : RecyclerView.Adapter<LocationDataAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = data[position]
        holder.textView.text = location
    }

    fun addLocation(location: Location) {
        val locationString = "Lat: ${location.latitude}, Lon: ${location.longitude} at ${location.time}"
        data.add(locationString)
        notifyItemInserted(data.size - 1)
    }
}
