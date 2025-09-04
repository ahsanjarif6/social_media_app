package com.example.social_media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SearchAdapter(
    private val users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<SearchAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.textUserName)
        val profilePic: ImageView = view.findViewById(R.id.imageUserProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val userId = user.id
        val timestamp = System.currentTimeMillis()
        val profilePicUrl = "https://${SupabaseClient.client.supabaseUrl}/storage/v1/object/public/profile-pic/$userId/$userId.jpg?t=$timestamp"

        holder.username.text = Helper.getUserName(user.email)

        Glide.with(holder.itemView.context)
            .load(profilePicUrl ?: R.drawable.default_profile_pic)
            .into(holder.profilePic)

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }
}
