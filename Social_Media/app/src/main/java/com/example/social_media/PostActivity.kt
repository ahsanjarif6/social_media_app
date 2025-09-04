package com.example.social_media

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class PostActivity : AppCompatActivity() {

    private lateinit var editTextCaption: EditText
    private lateinit var buttonUploadMedia: Button
    private lateinit var buttonPost: Button
    private var selectedMediaUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            Toast.makeText(this, "Media selected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        editTextCaption = findViewById(R.id.editTextCaption)
        buttonUploadMedia = findViewById(R.id.buttonUploadMedia)
        buttonPost = findViewById(R.id.buttonPost)

        buttonUploadMedia.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

        buttonPost.setOnClickListener {
            val caption = editTextCaption.text.toString().trim()
            val mediaUri = selectedMediaUri

            if (caption.isEmpty() || mediaUri == null) {
                Toast.makeText(this, "Please enter caption and select media", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadPost(mediaUri, caption)
        }

    }

    @kotlinx.serialization.Serializable
    data class Post(
        val caption: String,
        val media_url: String,
        val media_type: String,
        val user_id: String,
        val created_at: String
    )


    @OptIn(ExperimentalTime::class)
    private fun uploadPost(mediaUri: Uri, caption: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(mediaUri)
                val fileBytes = inputStream?.readBytes() ?: return@launch

                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val timestamp = System.currentTimeMillis()

                val mimeType = contentResolver.getType(mediaUri) ?: ""
                val mediaType = if (mimeType.startsWith("video")) "video" else "image"

                val fileExtension = if (mediaType == "video") ".mp4" else ".jpg"
                val fileName = "$userId/$timestamp$fileExtension"

                try {
                    SupabaseClient.client.storage.from("post-media").upload(fileName, fileBytes) {
                        upsert = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PostActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val mediaUrl = SupabaseClient.client.storage
                    .from("post-media")
                    .publicUrl(fileName)

                val post = Post(
                    caption = caption,
                    media_url = mediaUrl,
                    media_type = mediaType,
                    user_id = userId,
                    created_at = Clock.System.now().toString()
                )

                try {
                    SupabaseClient.client
                        .from("posts")
                        .insert(post)
                }catch (e: Exception){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PostActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "Post uploaded", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "Failed to upload", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}