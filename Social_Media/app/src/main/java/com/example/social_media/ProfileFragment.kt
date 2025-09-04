package com.example.social_media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.social_media.Helper.getUserName
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.UploadOptionBuilder
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var textViewUserName: TextView
    private lateinit var buttonUploadPicture: Button
    private lateinit var profileImage: CircleImageView
    private lateinit var recyclerViewPosts: RecyclerView
    private var user: UserInfo?=null
    private lateinit var adapter: PostAdapter
    private lateinit var commentLauncher: ActivityResultLauncher<Intent>

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (user == null) return@launch
                val userId = user!!.id

                val inputStream = context?.contentResolver?.openInputStream(uri)
                if (inputStream == null) return@launch

                val fileBytes = inputStream.readBytes()

                val path = "$userId/$userId.jpg"

                try {
                    SupabaseClient.client
                        .storage
                        .from("profile-pic")
                        .upload(path, fileBytes) {
                            upsert = true
                        }


                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Upload successful", Toast.LENGTH_SHORT).show()
                        showProfilePic()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textViewUserName = view.findViewById(R.id.textViewUserName)
        buttonUploadPicture = view.findViewById(R.id.buttonUploadPicture)
        profileImage = view.findViewById(R.id.profileImage)
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts)
        user = SupabaseClient.client.auth.currentUserOrNull()

        setUserName()

        viewLifecycleOwner.lifecycleScope.launch {
            showProfilePic()
        }

        buttonUploadPicture.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            commentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val updatedPostId = result.data?.getStringExtra("UPDATED_POST_ID")
                    if (updatedPostId != null) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            adapter.refreshPost(updatedPostId)
                        }
                    }
                }
            }
        }

        loadUserPosts()
    }

    private suspend fun showProfilePic() {
        val userId = user?.id ?: return
        val timestamp = System.currentTimeMillis()
        val profilePicUrl = "https://${SupabaseClient.client.supabaseUrl}/storage/v1/object/public/profile-pic/$userId/$userId.jpg?t=$timestamp"

        try {
            withContext(Dispatchers.Main) {
                Glide.with(requireContext())
                    .load(profilePicUrl)
                    .placeholder(R.drawable.default_profile_pic)
                    .error(R.drawable.default_profile_pic)
                    .into(profileImage)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Couldn't load profile. Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
                profileImage.setImageResource(R.drawable.default_profile_pic)
            }
        }
    }

    private fun loadUserPosts() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val currentUserId = user?.id ?:  return@launch

            try {
                val result = Helper.fetchUserPosts(currentUserId)


                withContext(Dispatchers.Main) {
                    adapter = PostAdapter(
                        result,
                        viewLifecycleOwner.lifecycleScope,
                        currentUserId,
                        object : OnCommentClickListener {
                            override fun onCommentClicked(postId: String) {
                                val intent = Intent(requireContext(), CommentActivity::class.java)
                                intent.putExtra("POST_ID", postId)
                                commentLauncher.launch(intent)
                            }
                        }
                    )
                    recyclerViewPosts.adapter = adapter
                    recyclerViewPosts.layoutManager = LinearLayoutManager(requireContext())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setUserName() {
        if(user == null){
            textViewUserName.text = "Guest"
            return
        }
        val email = user!!.email
        textViewUserName.text = getUserName(email)
    }
}