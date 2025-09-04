package com.example.social_media

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentAdapter(
    comments: List<Comment>,
    private val scope: CoroutineScope,
    private val currentUserId: String
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val comments: MutableList<Comment> = comments.toMutableList()


    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textUserName: TextView = view.findViewById(R.id.textUserName)
        val textContent: TextView = view.findViewById(R.id.textComment)
        val buttonCommentUpvote: ImageButton = view.findViewById(R.id.buttonCommentUpvote)
        val buttonCommentDownvote: ImageButton = view.findViewById(R.id.buttonCommentDownvote)
        val textCommentVoteCount: TextView = view.findViewById(R.id.textCommentVoteCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val userId = comment.user_id
        holder.textContent.text = comment.content

        scope.launch {
            holder.textUserName.text = Helper.getUserName(Helper.getEmail(userId))
            val voteCount = Helper.getCommentVoteCount(comment.id)
            val userVote = Helper.getUserCommentVote(comment.id, currentUserId)

            withContext(Dispatchers.Main) {
                holder.textCommentVoteCount.text = voteCount.toString()

                when (userVote) {
                    1 -> {
                        holder.buttonCommentUpvote.setColorFilter(Color.GREEN)
                        holder.buttonCommentDownvote.setColorFilter(Color.GRAY)
                    }
                    -1 -> {
                        holder.buttonCommentUpvote.setColorFilter(Color.GRAY)
                        holder.buttonCommentDownvote.setColorFilter(Color.RED)
                    }
                    else -> {
                        holder.buttonCommentUpvote.setColorFilter(Color.GRAY)
                        holder.buttonCommentDownvote.setColorFilter(Color.GRAY)
                    }
                }
            }
        }

        holder.buttonCommentUpvote.setOnClickListener {
            holder.buttonCommentUpvote.isEnabled = false
            holder.buttonCommentDownvote.isEnabled = false
            scope.launch {
                Helper.handleCommentUpvote(currentUserId, comment.id)
                withContext(Dispatchers.Main) {
                    notifyItemChanged(position)
                    holder.buttonCommentUpvote.isEnabled = true
                    holder.buttonCommentDownvote.isEnabled = true
                }
            }
        }

        holder.buttonCommentDownvote.setOnClickListener {
            holder.buttonCommentUpvote.isEnabled = false
            holder.buttonCommentDownvote.isEnabled = false
            scope.launch {
                Helper.handleCommentDownvote(currentUserId, comment.id)
                withContext(Dispatchers.Main) {
                    notifyItemChanged(position)
                    holder.buttonCommentUpvote.isEnabled = true
                    holder.buttonCommentDownvote.isEnabled = true
                }
            }
        }

        holder.textUserName.setOnClickListener{
            NavigationUtils.handleUserClick(holder.itemView.context, userId)
        }
    }

    override fun getItemCount(): Int = comments.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
}