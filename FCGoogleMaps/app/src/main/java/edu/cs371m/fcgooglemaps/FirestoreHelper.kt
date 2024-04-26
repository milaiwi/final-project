import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import edu.cs371m.fcgooglemaps.FavoritesAdapter

class FirestoreHelper {

    data class Comment(
        val userName: String = "Anonymous",
        val score: Double = 0.0,
        val text: String = "",
        val userId: String = "",
        val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now() // Default to current time
    )

    data class User (
        val email: String = "",
        var favorites: List<Map<String, Any>> = emptyList(),
        val following: List<String> = emptyList(),
        val name: String = "",
        val uid: String = ""
    )

    val db = FirebaseFirestore.getInstance()

    fun submitCommentToFirestore(placeId: String, commentText: String, commentScore: Double, onSuccess: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("Comments", "User not logged in")
            return
        }
        fetchUserName(userId) { name ->
            val userName = name ?: "Anonymous"
            val newComment = hashMapOf(
                "userId" to FirebaseAuth.getInstance().currentUser?.uid,
                "userName" to userName,
                "text" to commentText,
                "score" to commentScore,
                "timestamp" to com.google.firebase.Timestamp.now(),
            )

            FirebaseFirestore.getInstance().collection("Locations")
                .document(placeId)
                .collection("comments")
                .add(newComment)
                .addOnSuccessListener {
                    Log.d("Firestore", "Comment successfully written!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error writing comment", e)
                }
        }
    }

    fun fetchCommentsFromFirestore(placeId: String, successCallback: (List<Comment>) -> Unit, failureCallback: (Exception) -> Unit) {
        Log.d("MapsActivity", "PlaceID: $placeId")
        val commentsRef = db.collection("Locations")
            .document(placeId) // Using the Place ID as the document ID
            .collection("comments")

        commentsRef.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("MapsActivity", "No comments found for placeId: $placeId")
                } else {
                    Log.d("MapsActivity", "Fetched documents count: ${documents.size()}")
                    documents.forEach { document ->
                        Log.d("MapsActivity", "Document data: ${document.data}")
                    }
                }

                val comments = documents.mapNotNull { it.toObject(Comment::class.java) }
                Log.d("MapsActivity", "Comments: ${comments}")
                successCallback(comments)
            }
            .addOnFailureListener { exception ->
                failureCallback(exception)
            }
    }

    // Calculate total score from comments
    fun calculateTotalScore(comments: List<Comment>): Double {
        var totalScore = 0.0
        comments.forEach { comment ->
            totalScore += comment.score
        }
        return totalScore
    }

    fun fetchFollowingFromFirestore(onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("ProfileActivity", "Fetching for UserID: $userId")
        userId?.let { _ ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d("ProfileActivity", "Document exists: ${document.data}")
                        val followingList = document.get("following") as? List<String> ?: emptyList()
                        onSuccess(followingList)
                    } else {
                        onFailure(NoSuchElementException("User document not found"))
                    }
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        }
    }

    fun fetchAllUsers(onSuccess: (List<Map<String, Any>>?) -> Unit, onFailure: (Exception) -> Unit) {
        val cachedUsers = getUserListCache()
        if (cachedUsers?.isNotEmpty() == true) {
            onSuccess(cachedUsers)
        } else {
            db.collection("users")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val users = mutableListOf<Map<String, Any>>()
                    for (document in querySnapshot.documents) {
                        val userData = document.data
                        userData?.let { users.add(it) }
                    }

                    setUserListCache(users)
                    onSuccess(users)
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        }
    }

    fun addToFollowingList(followerUid: String) {
        // TODO: Fix this shit
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FindActivity", "Adding to followers list: $currentUserId")

        currentUserId?.let { uid ->
            // Fetch the current state of the followers array
            db.collection("users").document(uid)
                .update("following", FieldValue.arrayUnion(followerUid))
                .addOnSuccessListener {
                    Log.d("Firestore", "Successfully added follower to followers list")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding follower to followers list", e)
                }
        }
    }


    fun removeFromFollowingList(uid: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { currentUserUid ->
            db.collection("users").document(currentUserUid)
                .update("following", FieldValue.arrayRemove(uid))
                .addOnSuccessListener {
                    Log.d("Firestore", "Successfully removed user from following list")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error removing user from following list", e)
                }
        }
    }
    fun addToFavorites(currentUser: User, placeId: String, lat: Double, long: Double, name: String) {
        val userRef = db.collection("users").document(currentUser.uid)

        // Create a new map representing the favorite location
        val favoriteLocationMap = hashMapOf(
            "name" to name,
            "latitude" to lat,
            "longitude" to long,
            "id" to placeId
        )

        // Fetch the current favorites array
        val favorites = currentUser.favorites.toMutableList()

        // Add the new favorite location map to favorites
        favorites.add(favoriteLocationMap)

        // Update the user document in Firestore with the updated favorites
        userRef.update("favorites", favorites)
            .addOnSuccessListener {
                currentUser.favorites = favorites
                Log.d("FirestoreHelper", "Location added to favorites successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "Error adding location to favorites", e)
            }
    }



    fun removeFromFavorites(currentUser: User, updatedFavorites: List<Map<String, Any>>) {
        val userRef = db.collection("users").document(currentUser.uid)
        userRef.update("favorites", updatedFavorites)
            .addOnSuccessListener {
                currentUser.favorites = updatedFavorites
                Log.d("FirestoreHelper", "User favorites updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "Error updating user favorites", e)
            }
    }

        fun fetchCurrentUser(onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        Log.d("FindActivity", "Snapshot: $documentSnapshot")
                        if (documentSnapshot.exists()) {
                            val user = documentSnapshot.toObject(User::class.java)
                            if (user != null) {
                                onSuccess(user)
                            } else {
                                onFailure(Exception("Failed to parse user data"))
                            }
                        } else {
                            onFailure(Exception("No such document"))
                        }
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            } else {
                onFailure(Exception("No authorized user"))
            }
        }



    companion object {
        private var userListCache: List<Map<String, Any>>? = null

        fun getUserListCache(): List<Map<String, Any>>? {
            return userListCache
        }

        fun setUserListCache(users: List<Map<String, Any>>?) {
            userListCache = users
        }
        fun fetchUserName(userId: String, callback: (String?) -> Unit) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        callback(name)
                    } else {
                        Log.d("Firestore", "No such document")
                        callback(null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("Firestore", "get failed with ", exception)
                    callback(null)
                }
        }
    }

}