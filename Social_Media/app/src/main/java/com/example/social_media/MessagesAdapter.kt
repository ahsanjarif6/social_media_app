package com.example.social_media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessagesAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()

    fun setMessages(list: List<Message>) {
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    fun addMessage(m: Message) {
        messages.add(m)
        notifyItemInserted(messages.lastIndex)
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].sender_id == currentUserId) 1 else 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == 1) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.row_message_outgoing, parent, false)
            OutVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.row_message_incoming, parent, false)
            InVH(v)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = messages[position]
        when (holder) {
            is OutVH -> holder.bind(m)
            is InVH  -> holder.bind(m)
        }
    }

    override fun getItemCount(): Int = messages.size

    class OutVH(v: View) : RecyclerView.ViewHolder(v) {
        private val body = v.findViewById<TextView>(R.id.textMessage)
        fun bind(m: Message) { body.text = m.content }
    }

    class InVH(v: View) : RecyclerView.ViewHolder(v) {
        private val body = v.findViewById<TextView>(R.id.textMessage)
        fun bind(m: Message) { body.text = m.content }
    }
}