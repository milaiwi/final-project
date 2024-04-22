package edu.cs371m.fcgooglemaps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            // User is signed in
            startActivity(Intent(this, MapsActivity::class.java))
            finish()
        } else {
            // No user is signed in
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
