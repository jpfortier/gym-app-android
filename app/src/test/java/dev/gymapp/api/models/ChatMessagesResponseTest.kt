package dev.gymapp.api.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessagesResponseTest {

    private val gson = Gson()

    @Test
    fun deserialize_chatMessages() {
        val json = """
            {
                "messages": [
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "role": "user",
                        "content": "bench press 135 for 8",
                        "created_at": "2025-03-08T14:30:00Z"
                    },
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440001",
                        "role": "assistant",
                        "content": "Logged bench press 135×8.",
                        "created_at": "2025-03-08T14:30:01Z"
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, ChatMessagesResponse::class.java)

        assertEquals(2, response.messages.size)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.messages[0].id)
        assertEquals("user", response.messages[0].role)
        assertEquals("bench press 135 for 8", response.messages[0].content)
        assertEquals("2025-03-08T14:30:00Z", response.messages[0].createdAt)
        assertEquals("assistant", response.messages[1].role)
        assertEquals("Logged bench press 135×8.", response.messages[1].content)
    }

    @Test
    fun deserialize_emptyMessages() {
        val json = """{"messages": []}"""
        val response = gson.fromJson(json, ChatMessagesResponse::class.java)
        assertEquals(0, response.messages.size)
    }
}
