package edu.cs371m.fcgooglemaps

import FirestoreHelper
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
    private val firestoreHelper = FirestoreHelper()

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

        // Set title
        val userName = intent.getStringExtra("userName") ?: "User"
        val uid      = intent.getStringExtra("uid") ?: ""
        titleTextView.text = "$userName Favorites"

        // Fetch favorites from Firestore
        fetchFavoritesFromFirestore(uid)
    }


    fun fetchFavoritesFromFirestore(uid: String) {
        firestoreHelper.db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val favorites = document.get("favorites") as? List<Map<String, Any>> ?: emptyList()
                    // Initialize RecyclerView and adapter with fetched favorites
                    favoritesAdapter = FavoritesAdapter(this@FavoritesActivity, favorites)
                    favoritesRecyclerView.adapter = favoritesAdapter
                } else {
                    Log.d("FavoritesActivity", "No such document")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FavoritesActivity", "Error fetching favorites", e)
            }
    }
}
