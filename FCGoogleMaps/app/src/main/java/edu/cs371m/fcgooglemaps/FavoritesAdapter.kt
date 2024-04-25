package edu.cs371m.fcgooglemaps

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.common.reflect.TypeToken

class FavoritesAdapter(
    private val context: Context,
    private val favoriteLocations: List<Map<String, Any>>) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = favoriteLocations[position]
        Log.d("FavoritesAdapter:", "Location: $location")
        val name = location["name"] as? String
        val latitude = location["latitude"] as? Double
        val longitude = location["longitude"] as? Double

        holder.locationTextView.text = name
        holder.locationTextView.setOnClickListener {
            val intent = Intent(context, MapsActivity::class.java).apply {
                putExtra("locationName", name)
                putExtra("latitude", latitude)
                putExtra("longitude", longitude)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return favoriteLocations.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
    }
}
