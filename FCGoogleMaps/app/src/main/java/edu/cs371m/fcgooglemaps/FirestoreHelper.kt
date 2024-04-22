import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue

class FirestoreHelper {

    data class Comment(
        val userName: String = "Anonymous",
        val score: Double = 0.0,
        val text: String = "",
        val userId: String = "",
        val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now() // Default to current time
    )

    private val db = FirebaseFirestore.getInstance()

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


    companion object {
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