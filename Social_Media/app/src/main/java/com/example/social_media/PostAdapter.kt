package com.example.social_media

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

interface OnCommentClickListener {
    fun onCommentClicked(postId: String)
}

class PostAdapter(
    posts: List<Post>,
    private val scope: CoroutineScope,
    private val currentUserId: String,
    private val commentClickListener: OnCommentClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    private var posts: MutableList<Post> = posts.toMutableList()

    suspend fun refreshPost(postId: String) {
        val index = posts.indexOfFirst { it.id == postId }
        if (index != -1) {
            val updatedPost = SupabaseClient.client
                .from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingle<Post>()
            posts[index] = updatedPost
            withContext(Dispatchers.Main) {
                notifyItemChanged(index)
            }
        }
    }

    inner class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imageUserProfile: ImageView = itemView.findViewById(R.id.imageUserProfile)
        val textUserName: TextView = itemView.findViewById(R.id.textUserName)
        val textCaption: TextView = itemView.findViewById(R.id.textCaption)
        val imageMedia: ImageView = itemView.findViewById(R.id.imageMedia)
        val videoMedia: PlayerView = itemView.findViewById(R.id.videoMedia)
        val buttonUpvote:ImageButton = itemView.findViewById(R.id.buttonUpvote)
        val buttonDownvote: ImageButton = itemView.findViewById(R.id.buttonDownvote)
        val buttonComment: ImageButton = itemView.findViewById(R.id.buttonComment)
        val textVoteCount: TextView = itemView.findViewById(R.id.textVoteCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent,false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val userId = post.user_id

        holder.imageMedia.visibility = View.GONE
        holder.videoMedia.visibility = View.GONE
        holder.videoMedia.player?.release()
        holder.videoMedia.player = null

        scope.launch {
            try {
                val user = SupabaseClient.client
                    .from("profiles")
                    .select {
                        filter { eq("id", post.user_id) }
                        limit(1)
                    }.decodeSingle<User>()

                val timestamp = System.currentTimeMillis()
                val profilePicUrl = "https://${SupabaseClient.client.supabaseUrl}/storage/v1/object/public/profile-pic/${user.id}/${user.id}.jpg?t=$timestamp"

                withContext(Dispatchers.Main) {
                    holder.textUserName.text = Helper.getUserName(user.email)

                    Glide.with(holder.itemView.context)
                        .load(profilePicUrl)
                        .placeholder(R.drawable.default_profile_pic)
                        .circleCrop()
                        .into(holder.imageUserProfile)
                }
                updateUI(holder, post.id, currentUserId)

            } catch (e: Exception) {
                Log.e("PostAdapter-onBindViewHolder", "Failed to load user", e)
            }
        }

        holder.textCaption.text = post.caption

        if (post.media_type == "image") {
            holder.imageMedia.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(post.media_url + "?t=${System.currentTimeMillis()}")
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.imageMedia)

        } else if (post.media_type == "video") {
            holder.videoMedia.visibility = View.VISIBLE

            val context = holder.itemView.context
            val player = ExoPlayer.Builder(context).build()
            holder.videoMedia.player = player
            val mediaItem = MediaItem.fromUri(post.media_url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = false
        }

        holder.buttonUpvote.setOnClickListener {
            holder.buttonUpvote.isEnabled = false
            holder.buttonDownvote.isEnabled = false
            scope.launch {
                Helper.handleUpvote(currentUserId, post.id)
                updateUI(holder, post.id, currentUserId)
                withContext(Dispatchers.Main) {
                    holder.buttonUpvote.isEnabled = true
                    holder.buttonDownvote.isEnabled = true
                }
            }
        }

        holder.buttonDownvote.setOnClickListener {
            holder.buttonUpvote.isEnabled = false
            holder.buttonDownvote.isEnabled = false
            scope.launch {
                Helper.handleDownvote(currentUserId, post.id)
                updateUI(holder, post.id, currentUserId)
                withContext(Dispatchers.Main) {
                    holder.buttonUpvote.isEnabled = true
                    holder.buttonDownvote.isEnabled = true
                }
            }
        }

        holder.buttonComment.setOnClickListener {
            commentClickListener.onCommentClicked(post.id)
        }

        holder.textUserName.setOnClickListener{
            NavigationUtils.handleUserClick(holder.itemView.context, userId)
        }

        holder.imageUserProfile.setOnClickListener{
            NavigationUtils.handleUserClick(holder.itemView.context, userId)
        }
    }

    private suspend fun updateUI(holder: PostViewHolder, postId: String, userId: String){
        val updatedCount = Helper.getVoteCount(postId)
        val userVoted = Helper.getUserVote(postId, userId)
        withContext(Dispatchers.Main) {
            holder.textVoteCount.text = updatedCount.toString()

            when (userVoted) {
                1 -> {
                    holder.buttonUpvote.setColorFilter(Color.GREEN)
                    holder.buttonDownvote.setColorFilter(Color.GRAY)
                }
                -1 -> {
                    holder.buttonUpvote.setColorFilter(Color.GRAY)
                    holder.buttonDownvote.setColorFilter(Color.RED)
                }
                else -> {
                    holder.buttonUpvote.setColorFilter(Color.GRAY)
                    holder.buttonDownvote.setColorFilter(Color.GRAY)
                }
            }
        }
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.videoMedia.player?.release()
        holder.videoMedia.player = null
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts.toMutableList()
        notifyDataSetChanged()
    }

    fun addPost(post: Post) {
        posts.add(0, post)
        notifyItemInserted(0)
    }

    fun setPosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun removePostsByUser(userId: String?) {
        if (userId == null) return
        val iterator = posts.iterator()
        var removed = false

        while (iterator.hasNext()) {
            if (iterator.next().user_id == userId) {
                iterator.remove()
                removed = true
            }
        }

        if (removed) {
            notifyDataSetChanged()
        }
    }

}