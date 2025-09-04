package com.example.social_media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val feedPosts = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter
    private var recyclerViewPosts: RecyclerView? = null
    private lateinit var commentLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        commentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedPostId = result.data?.getStringExtra("UPDATED_POST_ID")
                if (!updatedPostId.isNullOrEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        postAdapter.refreshPost(updatedPostId)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts)

        postAdapter = PostAdapter(
            posts = emptyList(),
            scope = viewLifecycleOwner.lifecycleScope,
            currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "",
            commentClickListener = object : OnCommentClickListener {
                override fun onCommentClicked(postId: String) {
                    val intent = Intent(requireContext(), CommentActivity::class.java)
                    intent.putExtra("POST_ID", postId)
                    commentLauncher.launch(intent)
                }
            }
        )

        recyclerViewPosts?.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val currentUserId = user?.id

                if (currentUserId == null) {
                    postAdapter.updatePosts(emptyList())
                    return@repeatOnLifecycle
                }

                loadInitialFeed()

                val postsChannel = SupabaseClient.client.channel("posts-changes")
                val postsChanges = postsChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "posts"
                }

                val followsChannel = SupabaseClient.client.channel("follows-changes")
                val followsChanges = followsChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                    table = "follows"
                }

                try {
                    postsChanges.onEach { change ->
                        try {
                            val jsonElem = change.record
                            val newPost = Json.decodeFromJsonElement<Post>(jsonElem)
                            val myId = currentUserId
                            if (!myId.isNullOrEmpty() && Helper.isFollowing(myId, newPost.user_id)) {
                                withContext(Dispatchers.Main) {
                                    postAdapter.addPost(newPost)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error handling new post change", e)
                        }
                    }.launchIn(this)

                    followsChanges.onEach { change ->
                        try {
                            val oldRecord = change.oldRecord
                            val unfollowedUserId = oldRecord["following_id"]?.jsonPrimitive?.content
                            val followerId = oldRecord["follower_id"]?.jsonPrimitive?.content
                            val myId = currentUserId
                            if (!myId.isNullOrEmpty() && followerId == myId && !unfollowedUserId.isNullOrEmpty()) {
                                withContext(Dispatchers.Main) {
                                    postAdapter.removePostsByUser(unfollowedUserId)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error handling unfollow change", e)
                        }
                    }.launchIn(this)

                    postsChannel.subscribe()
                    followsChannel.subscribe()

                    awaitCancellation()
                } finally {
                    try { postsChannel.unsubscribe() } catch (ignored: Exception) {}
                    try { followsChannel.unsubscribe() } catch (ignored: Exception) {}
                }
            }
        }
    }

    private fun loadInitialFeed() {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val posts = Helper.getFollowedPosts(uid)
                withContext(Dispatchers.Main) {
                    feedPosts.clear()
                    feedPosts.addAll(posts)
                    postAdapter.updatePosts(posts)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to load initial feed", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerViewPosts?.adapter = null
        recyclerViewPosts = null
    }
}