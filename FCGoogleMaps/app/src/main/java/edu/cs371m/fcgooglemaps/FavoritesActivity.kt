package edu.cs371m.fcgooglemaps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favlocations)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        titleTextView = findViewById(R.id.titleTextView)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Retrieve data from intent
        val userName = intent.getStringExtra("userName") ?: "User"
//        val favoriteLocations = intent.getStringArrayListExtra("favorites") ?: ArrayList()
        val favoriteLocations = intent.getSerializableExtra("favorites") as? List<*>
        val typedFavoriteLocations = favoriteLocations?.filterIsInstance<Map<String, Any>>() ?: emptyList()
        // Set title
        titleTextView.text = "$userName Favorites"

        Log.d("FavoritesActivity", "Locations: $favoriteLocations")

        // Initialize RecyclerView and adapter
        favoritesAdapter = FavoritesAdapter(this@FavoritesActivity, typedFavoriteLocations as List<Map<String, Any>>)
        favoritesRecyclerView.adapter = favoritesAdapter
    }
}
