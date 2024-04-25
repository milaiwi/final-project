package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import edu.cs371m.fcgooglemaps.databinding.ActivitySignupBinding
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    data class UserProfile(
        val uid: String = "",
        val name: String = "",
        val email: String = "",
        val followers: List<String> = emptyList(),
        val favorites: List<String> = emptyList()
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password, name)
            } else {
                Toast.makeText(this, "Email and Password cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginLink.setOnClickListener {
            // Navigate to SignupActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    fun registerUser(email: String, password: String, name: String) {
        Log.d("Signup", "Registering account $name")
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Signup", "Task was successful!")
                    // User is successfully registered and logged in
                    // Now store additional details in Firestore
                    val user = FirebaseAuth.getInstance().currentUser
                    val userProfile = UserProfile(user!!.uid, name, email, emptyList(), emptyList())

                    FirebaseFirestore.getInstance().collection("users")
                        .document(user.uid)
                        .set(userProfile)
                        .addOnSuccessListener {
                            Log.d("RegisterActivity", "User profile saved in Firestore")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.w("RegisterActivity", "Error writing document", e)
                        }
                } else {
                    // Handle failures
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
