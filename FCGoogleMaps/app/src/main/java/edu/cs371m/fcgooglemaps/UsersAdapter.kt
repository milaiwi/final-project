package edu.cs371m.fcgooglemaps

import FirestoreHelper
import FirestoreHelper.User
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class UsersAdapter(
    private var userList: List<Map<String, Any>>?,
    private var User: User,
    private val followClickListener: (List<Map<String, Any>>?) -> Unit) :
    RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

        private val firestoreHelper = FirestoreHelper()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_find, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val followButton: Button = itemView.findViewById(R.id.followButton)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList?.get(position)
        Log.d("UsersAdapter", "User: $user")
        val userName = user?.get("name") as? String
        val uid = user?.get("uid") as? String
        val isCurrentUser = uid == User.uid
        Log.d("UsersAdapter", "CurrentUser: $User")
        val followers = User.following as? List<String> ?: emptyList()

        if (!isCurrentUser) {
            holder.userNameTextView.text = userName
            if (uid in followers) {
                holder.followButton.text = "FOLLOWING"
            } else {
                holder.followButton.text = "FOLLOW"
            }
            holder.followButton.setOnClickListener {
                if (holder.followButton.text == "FOLLOW") {
                    holder.followButton.text = "FOLLOWING"
                    if (userName != null && uid != null) {
                        firestoreHelper.addToFollowingList(uid)
                    }
                } else {
                    holder.followButton.text = "FOLLOW"
                    if (uid != null) {
                        firestoreHelper.removeFromFollowingList(uid)
                    }
                }
            }
        } else {
            // Hide the follow button for the current user
            holder.followButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return userList?.size ?: 0
    }

    fun updateUserList(newUsers: List<Map<String, Any>>?) {
        userList = newUsers
        notifyDataSetChanged() // Notify the adapter that the dataset has changed
    }
}
