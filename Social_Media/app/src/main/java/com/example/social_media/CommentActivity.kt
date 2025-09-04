package com.example.social_media

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommentActivity : AppCompatActivity() {

    private lateinit var imageUserProfile: ImageView
    private lateinit var textUserName: TextView
    private lateinit var textPostCaption : TextView
    private lateinit var imageMedia: ImageView
    private lateinit var videoMedia: PlayerView
    private lateinit var buttonPostUpvote: ImageButton
    private lateinit var textPostVoteCount: TextView
    private lateinit var buttonPostDownvote: ImageButton
    private lateinit var editTextComment: EditText
    private lateinit var buttonSendComment: ImageButton
    private lateinit var recyclerViewComments: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var postId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        imageUserProfile = findViewById(R.id.imageUserProfile)
        textUserName = findViewById(R.id.textUserName)
        textPostCaption = findViewById(R.id.textPostCaption)
        imageMedia = findViewById(R.id.imageMedia)
        videoMedia = findViewById(R.id.videoMedia)
        buttonPostUpvote = findViewById(R.id.buttonPostUpvote)
        textPostVoteCount = findViewById(R.id.textPostVoteCount)
        buttonPostDownvote = findViewById(R.id.buttonPostDownvote)
        editTextComment = findViewById(R.id.editTextComment)
        buttonSendComment = findViewById(R.id.buttonSendComment)
        recyclerViewComments = findViewById(R.id.recyclerViewComments)
        postId = intent.getStringExtra("POST_ID") ?: run {
            finish()
            return
        }

        loadPostDetails(postId)

        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

        buttonPostUpvote.setOnClickListener {
            buttonPostUpvote.isEnabled = false
            buttonPostDownvote.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                if (currentUserId != null) {
                    Helper.handleUpvote(currentUserId, postId)
                    updatePostUI(postId, currentUserId)
                }
                withContext(Dispatchers.Main) {
                    buttonPostUpvote.isEnabled = true
                    buttonPostDownvote.isEnabled = true
                    val resultIntent = Intent()
                    resultIntent.putExtra("UPDATED_POST_ID", postId)
                    setResult(Activity.RESULT_OK, resultIntent) 
                }
            }
        }

        buttonPostDownvote.setOnClickListener {
            buttonPostUpvote.isEnabled = false
            buttonPostDownvote.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                if (currentUserId != null) {
                    Helper.handleDownvote(currentUserId, postId)
                    updatePostUI(postId, currentUserId)
                }
                withContext(Dispatchers.Main) {
                    buttonPostUpvote.isEnabled = true
                    buttonPostDownvote.isEnabled = true
                    val resultIntent = Intent()
                    resultIntent.putExtra("UPDATED_POST_ID", postId)
                    setResult(Activity.RESULT_OK, resultIntent)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            val resultIntent = Intent()
            resultIntent.putExtra("UPDATED_POST_ID", postId)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val initialComments = Helper.fetchComments(postId)

            withContext(Dispatchers.Main) {
                commentAdapter = CommentAdapter(initialComments, lifecycleScope, currentUserId!!)
                recyclerViewComments.adapter = commentAdapter
                recyclerViewComments.layoutManager = LinearLayoutManager(this@CommentActivity)
            }
        }


        buttonSendComment.setOnClickListener {
            val content = editTextComment.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            } else {
                if (currentUserId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            Helper.postComment(postId, currentUserId, content)

                            val updatedComments = Helper.fetchComments(postId)

                            withContext(Dispatchers.Main) {
                                commentAdapter.updateComments(updatedComments)
                                editTextComment.text.clear()
                                Toast.makeText(this@CommentActivity, "Comment posted", Toast.LENGTH_SHORT).show()
                            }
                        }catch (e: Exception){
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@CommentActivity, "Error posting comment", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun updatePostUI(postId: String, userId: String) {
        val voteCount = Helper.getVoteCount(postId)
        val userVote = Helper.getUserVote(postId, userId)

        withContext(Dispatchers.Main) {
            textPostVoteCount.text = voteCount.toString()

            when (userVote) {
                1 -> {
                    buttonPostUpvote.setColorFilter(Color.GREEN)
                    buttonPostDownvote.setColorFilter(Color.GRAY)
                }
                -1 -> {
                    buttonPostUpvote.setColorFilter(Color.GRAY)
                    buttonPostDownvote.setColorFilter(Color.RED)
                }
                else -> {
                    buttonPostUpvote.setColorFilter(Color.GRAY)
                    buttonPostDownvote.setColorFilter(Color.GRAY)
                }
            }
        }
    }


    private fun loadPostDetails(postId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val post = Helper.fetchPost(postId)
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (post == null || currentUserId == null) return@launch

            val author = Helper.fetchUser(post.user_id)
            val voteCount = Helper.getVoteCount(post.id)
            val userVote = Helper.getUserVote(post.id, currentUserId)
            if(author==null) return@launch

            withContext(Dispatchers.Main) {
                textPostCaption.text = post.caption

                textUserName.text = Helper.getUserName(author.email)
                val timestamp = System.currentTimeMillis()
                val profilePicUrl = "https://${SupabaseClient.client.supabaseUrl}/storage/v1/object/public/profile-pic/${author.id}/${author.id}.jpg?t=$timestamp"
                Glide.with(this@CommentActivity)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.default_profile_pic)
                    .into(imageUserProfile)

                textPostVoteCount.text = voteCount.toString()

                when (userVote) {
                    1 -> {
                        buttonPostUpvote.setColorFilter(Color.GREEN)
                        buttonPostDownvote.setColorFilter(Color.GRAY)
                    }
                    -1 -> {
                        buttonPostUpvote.setColorFilter(Color.GRAY)
                        buttonPostDownvote.setColorFilter(Color.RED)
                    }
                    else -> {
                        buttonPostUpvote.setColorFilter(Color.GRAY)
                        buttonPostDownvote.setColorFilter(Color.GRAY)
                    }
                }

                imageMedia.visibility = View.GONE
                videoMedia.visibility = View.GONE
                videoMedia.player?.release()
                videoMedia.player = null

                if (post.media_type == "image") {
                    imageMedia.visibility = View.VISIBLE
                    Glide.with(this@CommentActivity)
                        .load(post.media_url + "?t=${System.currentTimeMillis()}")
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageMedia)
                } else if (post.media_type == "video") {
                    videoMedia.visibility = View.VISIBLE
                    val player = ExoPlayer.Builder(this@CommentActivity).build()
                    videoMedia.player = player
                    val mediaItem = MediaItem.fromUri(post.media_url)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = false
                }
            }
        }
    }
}