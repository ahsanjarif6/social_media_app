package com.example.social_media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtherUserProfileActivity : AppCompatActivity() {

    private lateinit var adapter: PostAdapter
    private lateinit var textViewUserName: TextView
    private lateinit var profileImage: CircleImageView
    private lateinit var recyclerViewPosts: RecyclerView
    private lateinit var buttonFollow: Button
    private lateinit var buttonMessage: Button
    private lateinit var commentLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_user_profile)

        recyclerViewPosts = findViewById(R.id.recyclerViewPosts)
        textViewUserName = findViewById(R.id.textViewUserName)
        profileImage = findViewById(R.id.profileImage)
        buttonFollow = findViewById(R.id.buttonFollow)
        buttonMessage = findViewById(R.id.buttonMessage)

        val userId = intent.getStringExtra("USER_ID") ?: return
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return

        if(userId == currentUserId){
            buttonFollow.visibility = View.GONE
            buttonMessage.visibility = View.GONE
        }
        else{
            buttonFollow.visibility = View.VISIBLE
            buttonMessage.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            val userName = Helper.getUserName(Helper.getEmail(userId))

            withContext(Dispatchers.Main){
                textViewUserName.text = userName
            }
        }

        lifecycleScope.launch(Dispatchers.IO){
            showProfilePic(userId)
        }


        loadUserPosts(userId, currentUserId)

        lifecycleScope.launch {
            val isFollowing = withContext(Dispatchers.IO) {
                Helper.isFollowing(currentUserId, userId)
            }
            buttonFollow.text = if (isFollowing) "Unfollow" else "Follow"
        }

        buttonFollow.setOnClickListener {
            lifecycleScope.launch {
                val currentlyFollowing = buttonFollow.text == "Unfollow"

                withContext(Dispatchers.IO) {
                    if (currentlyFollowing) {
                        Helper.deleteFollower(currentUserId, userId)
                    } else {
                        Helper.insertFollower(currentUserId, userId)
                    }
                }

                buttonFollow.text = if (currentlyFollowing) "Follow" else "Unfollow"
            }
        }

        buttonMessage.setOnClickListener {
            val me = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@setOnClickListener
            val otherId = intent.getStringExtra("USER_ID") ?: return@setOnClickListener

            lifecycleScope.launch {
                val repo = ChatRepository(SupabaseClient.client)
                val chat = repo.getOrCreateChat(me, otherId)
                val intent = Intent(this@OtherUserProfileActivity, ChatActivity::class.java).apply {
                    putExtra("chat_id", chat.id)
                    putExtra("other_user_id", otherId)
                }
                startActivity(intent)
            }
        }


        commentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedPostId = result.data?.getStringExtra("UPDATED_POST_ID")
                if (updatedPostId != null) {
                    lifecycleScope.launch {
                        adapter.refreshPost(updatedPostId)
                    }
                }
            }
        }
    }

    private suspend fun showProfilePic(userId: String) {
        val timestamp = System.currentTimeMillis()
        val profilePicUrl = "https://${SupabaseClient.client.supabaseUrl}/storage/v1/object/public/profile-pic/$userId/$userId.jpg?t=$timestamp"

        try {
            withContext(Dispatchers.Main) {
                Glide.with(this@OtherUserProfileActivity)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.default_profile_pic)
                    .error(R.drawable.default_profile_pic)
                    .into(profileImage)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@OtherUserProfileActivity,
                    "Couldn't load profile. Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
                profileImage.setImageResource(R.drawable.default_profile_pic)
            }
        }
    }

    private fun loadUserPosts(userId: String, currentUserId: String) {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val result = Helper.fetchUserPosts(userId)


                withContext(Dispatchers.Main) {
                    adapter = PostAdapter(
                        result,
                        lifecycleScope,
                        currentUserId,
                        object : OnCommentClickListener {
                            override fun onCommentClicked(postId: String) {
                                val intent = Intent(this@OtherUserProfileActivity, CommentActivity::class.java)
                                intent.putExtra("POST_ID", postId)
                                commentLauncher.launch(intent)
                            }
                        }
                    )
                    recyclerViewPosts.adapter = adapter
                    recyclerViewPosts.layoutManager = LinearLayoutManager(this@OtherUserProfileActivity)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("OtherUserProfileActivity-loadUserPosts","Failed to load posts",e)
                    Toast.makeText(this@OtherUserProfileActivity, "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}