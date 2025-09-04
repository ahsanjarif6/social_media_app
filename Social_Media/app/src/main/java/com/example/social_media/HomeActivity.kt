package com.example.social_media

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
class HomeActivity : AppCompatActivity() {

    private var currentChatId: String? = null
    private lateinit var repo: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        repo = ChatRepository(SupabaseClient.client)

        val homeFragment = HomeFragment()
        val searchFragment = SearchFragment()
        val postFragment = PostFragment()
        val chatsFragment = ChatsFragment()
        val profileFragment = ProfileFragment()

        setCurrentFragment(homeFragment)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.myHome -> {
                    currentChatId = null
                    setCurrentFragment(homeFragment)
                }
                R.id.mySearch -> {
                    currentChatId = null
                    setCurrentFragment(searchFragment)
                }
                R.id.myPost -> {
                    startActivity(Intent(this, PostActivity::class.java))
                    return@setOnItemSelectedListener false
                }
                R.id.myMessage -> {
                    currentChatId = null
                    setCurrentFragment(chatsFragment)
                }
                R.id.myProfile -> {
                    currentChatId = null
                    setCurrentFragment(profileFragment)
                }
            }
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val myId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@repeatOnLifecycle

                WebSocketManager.connect(myId)

                val totalUnread = repo.countAllUnread(myId)
                UnreadCoordinator.setInitial(totalUnread)

                UnreadCoordinator.unread.collectLatest { count ->
                    updateMessageBadge(count)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val myId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@repeatOnLifecycle

                WebSocketManager.events.collectLatest { evt ->
                    if (evt is WebSocketManager.MessageEvent.Incoming) {
                        val message = evt.message

                        if (message.receiver_id == myId) {
                            val isInChatActivity = isUserInChatActivity()

                            if (!isInChatActivity) {
                                UnreadCoordinator.increment()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateMessageBadge(count: Int) {
        try {
            val nav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            val badge = nav.getOrCreateBadge(R.id.myMessage)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
                badge.badgeGravity = BadgeDrawable.TOP_END
            } else {
                badge.isVisible = false
                badge.clearNumber()
            }
        } catch (e: Exception) {
            Log.e("HomeActivity-updateMessageBadge", "Error updating badge: ${e.message}")
        }
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }

    private fun isUserInChatActivity(): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            val myId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val totalUnread = repo.countAllUnread(myId)
            UnreadCoordinator.setInitial(totalUnread)
        }
    }
}