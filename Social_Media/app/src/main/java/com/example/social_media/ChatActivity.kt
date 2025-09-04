package com.example.social_media
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : ComponentActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: ImageButton
    private lateinit var adapter: MessagesAdapter

    private lateinit var repo: ChatRepository
    private var chatId: String = ""
    private var myUserId: String = ""
    private var otherUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent.getStringExtra("chat_id") ?: ""
        otherUserId = intent.getStringExtra("other_user_id") ?: ""
        myUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

        repo = ChatRepository(SupabaseClient.client)

        recycler = findViewById(R.id.recyclerViewMessages)
        input = findViewById(R.id.editTextMessage)
        send = findViewById(R.id.buttonSendMessage)

        adapter = MessagesAdapter(currentUserId = myUserId)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        WebSocketManager.connect(myUserId)

        lifecycleScope.launch {
            val history = repo.loadMessages(chatId)
            withContext(Dispatchers.Main) {
                adapter.setMessages(history)
                recycler.scrollToPosition(adapter.itemCount - 1)
            }
            val unreadForMe = history.count { !it.read && it.receiver_id == myUserId }
            if (unreadForMe > 0) {
                repo.markChatRead(chatId, myUserId)
                UnreadCoordinator.decrementBy(unreadForMe)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WebSocketManager.events.collectLatest { evt ->
                    when (evt) {
                        is WebSocketManager.MessageEvent.Incoming -> {
                            val m = evt.message
                            if (m.chat_id == chatId) {
                                withContext(Dispatchers.Main) {
                                    adapter.addMessage(m)
                                    recycler.scrollToPosition(adapter.itemCount - 1)
                                }
                                if (m.receiver_id == myUserId) {
                                    repo.markChatRead(chatId, myUserId)
                                }
                            } else if (m.receiver_id == myUserId) {
                                UnreadCoordinator.increment()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        send.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            WebSocketManager.sendMessage(
                chatId = chatId,
                senderId = myUserId,
                receiverId = otherUserId,
                content = text
            )
            input.text.clear()
        }
    }
}