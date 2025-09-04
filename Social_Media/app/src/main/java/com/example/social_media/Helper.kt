package com.example.social_media

import android.graphics.Color
import android.util.Log
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.supabaseJson
import io.github.jan.supabase.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf
import kotlin.time.Clock

object Helper {

    fun getUserName(email: String?): String {
        if (email == null) return "Guest"
        var userName: String = ""
        for (char in email) {
            if (char == '@') break
            userName += char
        }
        return userName
    }

    suspend fun fetchPost(postId: String): Post? {
        return SupabaseClient.client
            .from("posts")
            .select{
                filter {
                    eq("id", postId)
                }
            }
            .decodeSingleOrNull<Post>()
    }

    suspend fun fetchUser(userId: String): User?{
        return SupabaseClient.client
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<User>()
    }

    suspend fun getVoteCount(postId: String): Int {
        return try {
            val votes = SupabaseClient.client
                .from("votes")
                .select {
                    filter {
                        eq("post_id", postId)
                    }
                }
                .decodeList<Vote>()

            votes.sumOf { it.vote }

        } catch (e: Exception) {
            Log.e("Helper-getVoteCount", "Error getting vote count", e)
            0
        }
    }

    suspend fun getUserVote(postId: String, userId: String): Int? {
        return try {
            val response = SupabaseClient.client
                .from("votes")
                .select {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<Vote>()

            response?.vote
        } catch (e: Exception) {
            Log.e("Helper-getUserVote", "Failed to get user vote", e)
            null
        }
    }

    suspend fun handleUpvote(userId: String, postId: String) {
        val existingVote = getExistingVote(userId, postId)
        val vote = Vote(postId, userId, 1)

        when (existingVote) {
            null -> {
                SupabaseClient.client.from("votes").insert(
                    vote
                )
            }

            1 -> {
                SupabaseClient.client.from("votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }
            }

            -1 -> {
                SupabaseClient.client.from("votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }
                SupabaseClient.client.from("votes").insert(
                    vote
                )
            }
        }
    }

    suspend fun getExistingVote(userId: String, postId: String): Int? {
        return try {
            val result = SupabaseClient.client
                .from("votes")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<Vote>()
            result?.vote
        } catch (e: Exception) {
            Log.e("Helper-getExistingVote", "Error fetching vote", e)
            null
        }
    }

    suspend fun handleDownvote(userId: String, postId: String) {
        val existingVote = getExistingVote(userId, postId)
        val vote = Vote(postId, userId, -1)

        when (existingVote) {
            null -> {
                SupabaseClient.client.from("votes").insert(
                    vote
                )
            }

            -1 -> {
                SupabaseClient.client.from("votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }
            }

            1 -> {
                SupabaseClient.client.from("votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }
                SupabaseClient.client.from("votes").insert(
                    vote
                )
            }
        }
    }

    suspend fun getEmail(userId: String): String?{
        try {
            val result = SupabaseClient.client.from("profiles").select {
                filter { eq("id",userId) }
            }.decodeSingleOrNull<User>()
            return result?.email
        }
        catch (e: Exception){
            return null
        }
    }

    suspend fun postComment(postId: String, userId: String, content: String) {
        val comment = mapOf(
            "post_id" to postId,
            "user_id" to userId,
            "content" to content
        )

        SupabaseClient.client
            .from("comments")
            .insert(comment)
    }

    suspend fun fetchComments(postId: String): List<Comment> {
        return SupabaseClient.client
            .from("comments")
            .select {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()
    }

    suspend fun fetchUserPosts(userId: String): List<Post> {
        return SupabaseClient.client
            .from("posts")
            .select {
                filter {
                    eq("user_id",userId)
                }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<Post>()
    }

    suspend fun getCommentVoteCount(commentId: String): Int {
        return try {
            val votes = SupabaseClient.client
                .from("comment_votes")
                .select {
                    filter {
                        eq("comment_id", commentId)
                    }
                }
                .decodeList<CommentVote>()

            votes.sumOf { it.vote }

        } catch (e: Exception) {
            Log.e("Helper-getCommentVoteCount", "Error getting vote count", e)
            0
        }
    }

    suspend fun getUserCommentVote(commentId: String, userId: String): Int? {
        return try {
            val response = SupabaseClient.client
                .from("comment_votes")
                .select {
                    filter {
                        eq("comment_id", commentId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<CommentVote>()

            response?.vote
        } catch (e: Exception) {
            Log.e("Helper-getUserCommentVote", "Failed to get user vote", e)
            null
        }
    }

    suspend fun handleCommentUpvote(userId: String, commentId: String) {
        val existingVote = getUserCommentVote(commentId, userId)
        val vote = CommentVote(userId,commentId,1)

        when (existingVote) {
            null -> {
                SupabaseClient.client.from("comment_votes").insert(
                    vote
                )
            }

            1 -> {
                SupabaseClient.client.from("comment_votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("comment_id", commentId)
                    }
                }
            }

            -1 -> {
                SupabaseClient.client.from("comment_votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("comment_id", commentId)
                    }
                }
                SupabaseClient.client.from("comment_votes").insert(
                    vote
                )
            }
        }
    }

    suspend fun handleCommentDownvote(userId: String, commentId: String) {
        val existingVote = getUserCommentVote(commentId, userId)
        val vote = CommentVote(userId, commentId, -1)

        when (existingVote) {
            null -> {
                SupabaseClient.client.from("comment_votes").insert(
                    vote
                )
            }

            -1 -> {
                SupabaseClient.client.from("comment_votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("comment_id", commentId)
                    }
                }
            }

            1 -> {
                SupabaseClient.client.from("comment_votes").delete {
                    filter {
                        eq("user_id", userId)
                        eq("comment_id", commentId)
                    }
                }
                SupabaseClient.client.from("comment_votes").insert(
                    vote
                )
            }
        }
    }


    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        val result = SupabaseClient.client
            .from("follows")
            .select{
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
                limit(1)
            }.decodeSingleOrNull<Follows>()

        return result != null
    }

    suspend fun deleteFollower(currentUserId: String, profileUserId: String){
        SupabaseClient.client
            .from("follows")
            .delete{
                filter {
                    eq("follower_id", currentUserId)
                    eq("following_id", profileUserId)
                }
            }
    }

    suspend fun insertFollower(currentUserId: String, profileUserId: String){
        val data = Follows(currentUserId, profileUserId)
        SupabaseClient.client
            .from("follows")
            .insert(data)
    }

    suspend fun getFollowedPosts(userId: String): List<Post> {
        val followingIds = SupabaseClient.client
            .from("follows")
            .select {
                filter {
                    eq("follower_id", userId)
                }
            }
            .decodeList<Follows>()
            .map { it.following_id }

        if (followingIds.isEmpty()) return emptyList()

        try {
            val list = SupabaseClient.client
                .from("posts")
                .select {
                    filter {
                        isIn("user_id", followingIds)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Post>()
            return list
        } catch (e: Exception) {
            Log.e("Helper", "Error fetching followed posts", e)
            return emptyList()
        }
    }

    @Serializable
    data class ChatInsert(
        val user1_id: String,
        val user2_id: String
    )

    suspend fun getOrCreateChat(otherUserId: String, currentUserId: String): String? {
        val user1_id = minOf(otherUserId, currentUserId)
        val user2_id = maxOf(otherUserId, currentUserId)

        val existingChat = SupabaseClient.client
            .from("chats")
            .select {
                filter {
                    eq("user1_id", user1_id)
                    eq("user2_id", user2_id)
                }
            }
            .decodeSingleOrNull<Chat>()

        return if (existingChat != null) {
            existingChat.id
        } else {
            val newChat = SupabaseClient.client
                .from("chats")
                .insert(
                    ChatInsert(user1_id = user1_id, user2_id = user2_id)
                )
                .decodeSingleOrNull<Chat>()

            newChat?.id
        }
    }


}