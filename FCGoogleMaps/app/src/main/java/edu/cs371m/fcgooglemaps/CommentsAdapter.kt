package edu.cs371m.fcgooglemaps

import FirestoreHelper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CommentsAdapter(private val comments: MutableList<FirestoreHelper.Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var commentsListener: ListenerRegistration

//    init {
//        startListening()
//    }
//
    // Real-time listener for comments collection
    fun startListening(placeId: String) {
        Log.d("Adapter", "PlaceID: $placeId")
        commentsListener = firestore.collection("Locations")
            .document(placeId).collection("comments")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CommentsAdapter", "Listen failed", exception)
                    return@addSnapshotListener
                }
                Log.d("Adapter", "Updating list")

                val updatedComments = mutableListOf<FirestoreHelper.Comment>()
                snapshot?.documents?.forEach { doc ->
                    val comment = doc.toObject(FirestoreHelper.Comment::class.java)
                    Log.d("Adapter", "inside doc comment: $comment")
                    comment?.let {
                        if (it.timestamp != null) {
                            updatedComments.add(it)
                        }
                    }
                }
                // Update the comments list
                updateComments(updatedComments)
            }
    }


    // ViewHolder class to hold the item view
    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commentText: TextView = view.findViewById(R.id.commentText)
        val commentUser: TextView = view.findViewById(R.id.commentUser)
        val commentTimestamp: TextView = view.findViewById(R.id.commentTimestamp)
        val commentScore: TextView = view.findViewById(R.id.commentScore)
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        // Inflate the custom layout
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        // Get the data model based on position
        val comment = comments[position]

        Log.d("Adapter", "Comment reached $comment")
        holder.commentText.text = comment.text

        // Fetch user name based on user ID
        FirestoreHelper.fetchUserName(comment.userId) { userName ->
            if (holder.commentUser.text == null) {
                holder.commentUser.text = "Anonymous"
            } else {
                holder.commentUser.text = "User: $userName"
            }
        }


        // Convert Timestamp to LocalDateTime
        val instant = Instant.ofEpochSecond(comment.timestamp.seconds)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) // Convert instant to local date-time using the system's default timezone

        // Format LocalDateTime to string
        val formattedDate = localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        holder.commentTimestamp.text = formattedDate

        holder.commentScore.text = "Score: ${comment.score.toString()} / 5.0"
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<FirestoreHelper.Comment>) {
        Log.d("CommentsAdapter", "Notifying that dataset has changed!")
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    // Cleanup: Remove the listener when the adapter is no longer needed
    fun cleanup() {
        commentsListener.remove()

    }
}
