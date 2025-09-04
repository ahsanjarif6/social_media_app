package com.example.social_media
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
class ChatsFragment : Fragment(R.layout.fragment_chats) {

    private lateinit var repo: ChatRepository
    private lateinit var adapter: ChatsAdapter
    private var myUserId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        repo = ChatRepository(SupabaseClient.client)

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerChats)
        adapter = ChatsAdapter(
            onChatClicked = { item ->
                lifecycleScope.launch {
                    if (item.unreadCount > 0) {
                        repo.markChatRead(item.chat.id, myUserId)
                        UnreadCoordinator.decrementBy(item.unreadCount)
                    }
                }

                val ctx = requireContext()
                val intent = android.content.Intent(ctx, ChatActivity::class.java).apply {
                    putExtra("chat_id", item.chat.id)
                    putExtra("other_user_id", item.otherUserId)
                }
                startActivity(intent)
            }
        )
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        loadChatList()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WebSocketManager.events.collectLatest { evt ->
                    if (evt is WebSocketManager.MessageEvent.Incoming) {
                        val message = evt.message

                        if (message.receiver_id == myUserId || message.sender_id == myUserId) {
                            loadChatList()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadChatList()
    }

    private fun loadChatList() {
        lifecycleScope.launch {
            try {
                val items = repo.listChatsWithLastMessage(myUserId)
                adapter.submit(items)
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error loading chats: ${e.message}")
            }
        }
    }
}