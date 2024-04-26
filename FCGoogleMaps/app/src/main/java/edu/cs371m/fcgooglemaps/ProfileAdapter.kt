package edu.cs371m.fcgooglemaps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import FirestoreHelper.User
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.Serializable

class ProfileAdapter(
    private val context: Context,
    private val userList: List<Map<String, Any>>?,
    private val currentUser: User,
    private val followClickListener: (Map<String, Any>?) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList?.get(position)
        val uid = user?.get("uid") as? String
        val userName = user?.get("name") as? String
        val favorites = user?.get("favorites") as? List<*>

        if (currentUser.following.contains(uid)) {
            holder.userNameTextView.text = userName
            holder.followButton.setOnClickListener {
                val intent = Intent(context, FavoritesActivity::class.java).apply {
                    putExtra("userName", userName)
                    putExtra("uid", uid)
                    // Put the entire favorites array as a serializable extra
                    putExtra("favorites", favorites as Serializable)
                }
                context.startActivity(intent)
            }
        } else {
            // Hide the row if the user is not being followed
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
    }


    override fun getItemCount(): Int {
        return userList?.size ?: 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val followButton: Button = itemView.findViewById(R.id.followButton)
    }
}
