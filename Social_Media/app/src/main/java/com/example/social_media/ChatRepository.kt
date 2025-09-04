package com.example.social_media

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

class ChatRepository(
    private val supabase: io.github.jan.supabase.SupabaseClient
) {

    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): Chat {
        val u1 = minOf(currentUserId, otherUserId)
        val u2 = maxOf(currentUserId, otherUserId)

        val existing = supabase.from("chats")
            .select {
                filter {
                    eq("user1_id", u1)
                    eq("user2_id", u2)
                }
                limit(1)
            }
            .decodeList<Chat>()
            .firstOrNull()

        if (existing != null) return existing

        @Serializable
        data class ChatInsert(val user1_id: String, val user2_id: String)

        return supabase.from("chats")
            .insert(ChatInsert(user1_id = u1, user2_id = u2))
            .decodeSingle<Chat>()
    }

    suspend fun loadMessages(chatId: String): List<Message> =
        supabase.from("messages")
            .select {
                filter { eq("chat_id", chatId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()

    suspend fun insertMessage(insert: MessageInsert): Message =
        supabase.from("messages")
            .insert(insert)
            .decodeSingle()

    suspend fun markChatRead(chatId: String, readerId: String) {
        supabase.from("messages")
            .update(
                mapOf("read" to true)
            ) {
                filter {
                    eq("chat_id", chatId)
                    eq("receiver_id", readerId)
                    eq("read", false)
                }
            }
    }

    suspend fun listChatsWithLastMessage(currentUserId: String): List<ChatListItem> {
        val chats = supabase.from("chats")
            .select {
                filter {
                    or {
                        eq("user1_id", currentUserId)
                        eq("user2_id", currentUserId)
                    }
                }
            }
            .decodeList<Chat>()

        return chats.map { chat ->
            val otherId = if (chat.user1_id == currentUserId) chat.user2_id else chat.user1_id

            val otherUserEmail = try {
                supabase.from("profiles")
                    .select {
                        filter { eq("id", otherId) }
                        limit(1)
                    }
                    .decodeSingle<UserProfile>()
                    .email
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error fetching user email: ${e.message}")
                "Unknown User"
            }

            val lastMessage = supabase.from("messages")
                .select {
                    filter { eq("chat_id", chat.id) }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<Message>()
                .firstOrNull()

            val unread = try {
                supabase.from("messages")
                    .select {
                        filter {
                            eq("chat_id", chat.id)
                            eq("receiver_id", currentUserId)
                            eq("read", false)
                        }
                    }
                    .decodeList<Message>()
                    .size
            } catch (e: Exception) {
                0
            }

            ChatListItem(
                chat = chat,
                otherUserId = otherId,
                otherUserEmail = otherUserEmail,
                lastMessage = lastMessage,
                unreadCount = unread
            )
        }.sortedByDescending { it.lastMessage?.created_at }
    }

    suspend fun countAllUnread(currentUserId: String): Int =
        try {
            supabase.from("messages")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("read", false)
                    }
                }
                .decodeList<Message>()
                .size
        }catch (e: Exception){
            0
        }
}


@Serializable
data class UserProfile(
    val id: String,
    val email: String
)
@Serializable
data class ChatListItem(
    val chat: Chat,
    val otherUserId: String,
    val otherUserEmail: String,
    val lastMessage: Message?,
    val unreadCount: Int
)
@Serializable
data class MessageInsert(
    val chat_id: String,
    val sender_id: String,
    val receiver_id: String,
    val content: String
)