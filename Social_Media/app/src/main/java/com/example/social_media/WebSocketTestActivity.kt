package com.example.social_media

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TestWebSocketActivity : AppCompatActivity() {

    private lateinit var logText: TextView
    private lateinit var connectButton: Button
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logText = TextView(this).apply {
            text = "WebSocket Test Logs\n"
            textSize = 16f
            setPadding(20, 20, 20, 20)
        }
        connectButton = Button(this).apply { text = "Connect" }
        sendButton = Button(this).apply { text = "Send Test Message" }

        val layout = androidx.appcompat.widget.LinearLayoutCompat(this).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            addView(logText)
            addView(connectButton)
            addView(sendButton)
        }

        setContentView(layout)

        connectButton.setOnClickListener {
            log("Connecting...")
            WebSocketManager.connect("dummyUser123")
        }

        sendButton.setOnClickListener {
            log("Sending test message...")
            WebSocketManager.sendMessage(
                chatId = "testChat",
                senderId = "dummyUser123",
                receiverId = "dummyUser456",
                content = "Hello from Android!"
            )
        }

        lifecycleScope.launch {
            WebSocketManager.events.collect { event ->
                when (event) {
                    is WebSocketManager.MessageEvent.Connected -> log("Connected as ${event.userId}")
                    is WebSocketManager.MessageEvent.Disconnected -> log("Disconnected: ${event.reason}")
                    is WebSocketManager.MessageEvent.Incoming -> log("Incoming: ${event.message.content}")
                    is WebSocketManager.MessageEvent.Error -> log("Error: ${event.error.message}")
                }
            }
        }
    }

    private fun log(msg: String) {
        Log.e("TestWebSocketActivity", msg)
        runOnUiThread {
            logText.append("\n$msg")
        }
    }
}
