package com.example.social_media

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import io.github.jan.supabase.auth.auth

object NavigationUtils {

    fun handleUserClick(context: Context, userId: String) {
        val intent = Intent(context, OtherUserProfileActivity::class.java)
        intent.putExtra("USER_ID", userId)
        context.startActivity(intent)
    }

}