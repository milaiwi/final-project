package edu.cs371m.fcgooglemaps

import FirestoreHelper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProfileActivity : AppCompatActivity() {

    private val firestoreHelper = FirestoreHelper()
    private lateinit var followingAdapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFollowing)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }

        firestoreHelper.fetchAllUsers(
            onSuccess = { userList ->
                // Handle the retrieved user list here
                Log.d("ProfileActivity", "User list retrieved: $userList")
                firestoreHelper.fetchCurrentUser(
                    onSuccess = { user ->
                        followingAdapter = ProfileAdapter(this@ProfileActivity, userList, user) { selectedUser ->
                            Log.d("ProfileActivity", "Following user: $selectedUser")
                        }
                        recyclerView.adapter = followingAdapter
                    },
                    onFailure = { exception ->
                        println("Error fetching user: ${exception.message}")
                    }
                )
            },
            onFailure = { exception ->
                // Handle failure to fetch user list
                Log.e("ProfileActivity", "Failed to fetch user list", exception)
            }
        )
    }
}
