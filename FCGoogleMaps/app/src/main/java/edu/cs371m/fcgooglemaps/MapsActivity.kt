package edu.cs371m.fcgooglemaps

import FirestoreHelper
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import edu.cs371m.fcgooglemaps.databinding.ActivityMapsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private var locationPermissionGranted = false
    private lateinit var binding: ActivityMapsBinding
    private val firestoreHelper = FirestoreHelper()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                signOut()
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_find -> {
                startActivity(Intent(this, FindActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()  // Sign out from Firebase

        // Start LoginActivity and clear the task stack to prevent returning to this activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Map Navigator"

        // Set up the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFrag) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        geocoder = Geocoder(this, Locale.getDefault())

        binding.goBut.setOnClickListener {
            val locationName = binding.mapET.text.toString()
            handleAddressSearch(locationName)
        }

        binding.clearBtn.setOnClickListener {
            binding.mapET.text = null
        }
    }

    private fun moveCameraToLocation(locationName: String, latLng: LatLng) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20.0f))
    }

    private fun handleMapSetup() {
        // Check if the activity was started with location details
        val locationName = intent.getStringExtra("locationName")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        Log.d("MapsActivity", "Location: $locationName")
        if (locationName != null && latitude != 0.0 && longitude != 0.0) {
            moveCameraToLocation(locationName, LatLng(latitude, longitude))
        }
    }

    private fun handleAddressSearch(locationName: String) {
        Log.d("Geocoding", locationName)
        MainScope().launch {
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocationName(locationName, 1)
            }
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    processAddresses(addresses)
                } else {
                    Toast.makeText(this@MapsActivity, "Address not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun processAddresses(addresses: List<Address>) {
        val firstAddress = addresses.first()
        val latLng = LatLng(firstAddress.latitude, firstAddress.longitude)
        withContext(Dispatchers.Main) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (locationPermissionGranted) {
            enableMyLocation()
        }

        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        val austin = LatLng(30.2672, -97.7431)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(austin, 10f))

        map.setOnPoiClickListener { poi ->
            // Use firestoreHelper to fetch comments
            Log.d("MapsActivity", "Fetching comments")
            val lat = poi.latLng.latitude
            val long = poi.latLng.longitude
            val name = poi.name
            firestoreHelper.fetchCommentsFromFirestore(
                placeId = poi.placeId,
                successCallback = { comments ->
                    runOnUiThread {
                        showCommentsPopup(poi.placeId, comments.toMutableList(), lat, long, name)
                    }
                },
                failureCallback = { exception ->
                    runOnUiThread {
                        showCommentsPopup(poi.placeId, mutableListOf(), lat, long, name)
                        Log.e("MapsActivity", "Error fetching comments", exception)
                    }
                }
            )
        }

        handleMapSetup();
    }



    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
    }

    private fun addMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(
                "Lat: ${String.format("%.3f", latLng.latitude)}, Lng: ${
                    String.format(
                        "%.3f",
                        latLng.longitude
                    )
                }"
            )
        map.addMarker(markerOptions)
    }

    @SuppressLint("SetTextI18n")
    private fun showCommentsPopup(placeId: String, comments: MutableList<FirestoreHelper.Comment>,
                                  latitude: Double, longitude: Double, name: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_comments, null)
        val totalScoreText = dialogView.findViewById<TextView>(R.id.totalScore)
        val commentsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.commentsRecyclerView)
        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)
        val commentEditScore = dialogView.findViewById<EditText>(R.id.commentEditScore)
        val submitButton = dialogView.findViewById<Button>(R.id.submitCommentButton)
        val favoriteLocation = dialogView.findViewById<ImageView>(R.id.actionFavorite)

        val adapter = CommentsAdapter(comments)
        commentsRecyclerView.adapter = adapter
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)

        // If there are comments, show them, else show the hint text
        if (comments.isEmpty()) {
            commentEditText.hint = "Be the first to comment!"
        }

        val totalScore = (firestoreHelper.calculateTotalScore(comments) / comments.size)
        totalScoreText.text = "Total Score: ${String.format("%.2f", totalScore)}"

        firestoreHelper.fetchCurrentUser(
            onSuccess = { user ->
                Log.d("Favorites", "Current List: ${user.favorites}")
                var locationExists = user.favorites.any { it["id"] == placeId }
                if (locationExists) {
                    favoriteLocation.setImageResource(R.drawable.ic_favorite_black_24dp)
                } else {
                    favoriteLocation.setImageResource(R.drawable.ic_favorite_border_black)
                }

                favoriteLocation.setOnClickListener {
                    locationExists = user.favorites.any { it["id"] == placeId }
                    Log.d("Favorites", "Current List: ${user.favorites}")

                    if (!locationExists) {
                        // add to favorites
                        Log.d("Favorites", "Adding to map")
                        favoriteLocation.setImageResource(R.drawable.ic_favorite_black_24dp)
                        firestoreHelper.addToFavorites(user, placeId, latitude, longitude, name)
                    } else {
                        // remove from favorites
                        Log.d("Favorites", "Removing from map")
                        favoriteLocation.setImageResource(R.drawable.ic_favorite_border_black)
                        val updatedFavorites = user.favorites.filter { it["id"] != placeId }
                        firestoreHelper.removeFromFavorites(user, updatedFavorites)
                    }
                }
            },
            onFailure = { exception ->
                Log.d("FindActivity", "Error fetching user: ${exception.message}")
            }
        )


        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        submitButton.setOnClickListener {
            val commentText = commentEditText.text.toString()
            val commentScoreText = commentEditScore.text.toString()

            if (commentText.isNotBlank()) {
                try {
                    val commentScore = commentScoreText.toDouble()
                    if (commentScore in 0.0..5.0) {
                        firestoreHelper.submitCommentToFirestore(placeId, commentText, commentScore) {
                            alertDialog.dismiss() // Dismiss dialog after successful submission
                        }
                    } else {
                        Toast.makeText(this, "Please enter a score between 0 and 5", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid score", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }


        adapter.startListening(placeId)
        alertDialog.show()
    }
}