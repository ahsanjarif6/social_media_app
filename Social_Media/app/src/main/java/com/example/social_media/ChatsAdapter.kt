package com.example.social_media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class ChatsAdapter(
    private val onChatClicked: (ChatListItem) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.VH>() {

    private val items = mutableListOf<ChatListItem>()

    fun submit(newItems: List<ChatListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.textTitle)
        private val subtitle = view.findViewById<TextView>(R.id.textSubtitle)
        private val badge = view.findViewById<TextView>(R.id.textUnread)

        fun bind(item: ChatListItem) {
            val username = if (item.otherUserEmail.contains("@")) {
                item.otherUserEmail.substringBefore("@")
            } else {
                item.otherUserEmail
            }

            title.text = username
            subtitle.text = item.lastMessage?.content ?: "No messages yet"

            if (item.unreadCount > 0) {
                badge.visibility = View.VISIBLE
                badge.text = item.unreadCount.toString()
            } else {
                badge.visibility = View.GONE
            }

            itemView.setOnClickListener { onChatClicked(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}