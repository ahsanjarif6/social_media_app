package com.example.social_media

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebSocketManager {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var userId: String? = null
    private var serverUrl: String = "ws://10.0.2.2:8080"
    private var scope: CoroutineScope? = null

    private val _events = MutableSharedFlow<MessageEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MessageEvent> = _events
    private var isOpen = false
    private val pendingMessages = mutableListOf<String>()

    sealed class MessageEvent {
        data class Incoming(val message: Message) : MessageEvent()
        data class Connected(val userId: String) : MessageEvent()
        data class Disconnected(val reason: String?) : MessageEvent()
        data class Error(val error: Throwable) : MessageEvent()
    }

    fun connect(currentUserId: String) {
        if (webSocket != null && userId == currentUserId) return
        userId = currentUserId
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        try {
            val request = Request.Builder().url(serverUrl).build()
            webSocket = client.newWebSocket(request, listener)
        } catch (e: Exception) {
            Log.e("WebSocketManager-connect", "connect failed: ${e.message}")
        }
    }

    fun isConnected(): Boolean = webSocket != null

    fun disconnect() {
        try {
            webSocket?.close(1000, "client closing")
        } catch (_: Exception) {
        }
        webSocket = null
        userId = null
        scope?.cancel()
        scope = null
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            isOpen = true
            val id = userId ?: return
            ws.send("""{"type":"register","userId":"$id"}""")

            synchronized(pendingMessages) {
                for (msg in pendingMessages) {
                    ws.send(msg)
                }
                pendingMessages.clear()
            }

            scope?.launch { _events.emit(MessageEvent.Connected(id)) }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                when (json.optString("type")) {
                    "message" -> {
                        val message = Message(
                            id = json.optString("id", ""),
                            chat_id = json.getString("chatId"),  // Now matches server output
                            sender_id = json.getString("senderId"),
                            receiver_id = json.getString("receiverId"),
                            content = json.getString("content"),
                            created_at = json.optString("createdAt", ""),
                            read = false
                        )
                        scope?.launch { _events.emit(MessageEvent.Incoming(message)) }
                    }

                    "register_success" -> {

                    }
                }
            } catch (t: Throwable) {
                scope?.launch { _events.emit(MessageEvent.Error(t)) }
            }
        }

        override fun onMessage(ws: WebSocket, bytes: ByteString) {}

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            scope?.launch { _events.emit(MessageEvent.Error(t)) }
            disconnect()
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            isOpen = false
            disconnect()
        }
    }

    fun sendMessage(chatId: String, senderId: String, receiverId: String, content: String) {
        val payload = """
    {
      "type":"message",
      "chatId":"$chatId",
      "senderId":"$senderId",
      "receiverId":"$receiverId",
      "content":${JSONObject.quote(content)}
    }
""".trimIndent()

        if (isOpen && webSocket != null) {
            webSocket!!.send(payload)
        } else {
            synchronized(pendingMessages) {
                pendingMessages.add(payload)
            }
        }
    }
}
