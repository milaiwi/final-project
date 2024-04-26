package edu.cs371m.fcgooglemaps

import FirestoreHelper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FindActivity : AppCompatActivity() {

    private val firestoreHelper = FirestoreHelper()
    private lateinit var usersAdapter: UsersAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFind)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }


        Log.d("FindActivity", "Fetching users!")
        firestoreHelper.fetchAllUsers(
            onSuccess = { userList ->
                // Handle the retrieved user list here
                Log.d("FindActivity", "User list retrieved: $userList")
                firestoreHelper.fetchCurrentUser(
                    onSuccess = { user ->
                        val filteredUserList = userList?.filterNot { it["uid"] == user.uid }
                        usersAdapter = UsersAdapter(filteredUserList, user) { selectedUser ->
                            Log.d("FindActivity", "Following user: $selectedUser")
                        }
                        recyclerView.adapter = usersAdapter
                    },
                    onFailure = { exception ->
                        Log.d("FindActivity", "Error fetching user: ${exception.message}")
                    }
                )
            },
            onFailure = { exception ->
                // Handle failure to fetch user list
                Log.e("FindActivity", "Failed to fetch user list", exception)
            }
        )
    }
}
