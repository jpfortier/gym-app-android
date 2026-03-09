package dev.gymapp.api.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatResponseTest {

    private val gson = Gson()

    @Test
    fun deserialize_logIntent() {
        val json = """
            {
                "intent": "log",
                "message": "Logged.",
                "entries": [
                    {
                        "exercise_name": "Bench Press",
                        "variant_name": "standard",
                        "session_date": "2025-03-08",
                        "entry_id": "uuid-123"
                    }
                ],
                "prs": []
            }
        """.trimIndent()

        val response = gson.fromJson(json, ChatResponse::class.java)

        assertEquals("log", response.intent)
        assertEquals("Logged.", response.message)
        assertEquals(1, response.entries?.size)
        assertEquals("Bench Press", response.entries!![0].exerciseName)
        assertEquals("standard", response.entries!![0].variantName)
        assertEquals("2025-03-08", response.entries!![0].sessionDate)
        assertEquals("uuid-123", response.entries!![0].entryId)
    }

    @Test
    fun deserialize_queryIntent() {
        val json = """
            {
                "intent": "query",
                "message": null,
                "history": {
                    "exercise_name": "Bench Press",
                    "variant_name": "standard",
                    "entries": [
                        {
                            "session_date": "2025-03-08",
                            "raw_speech": "bench 135x8",
                            "sets": [
                                {"weight": 135, "reps": 8, "set_type": "working"}
                            ],
                            "created_at": "2025-03-08T14:30:00Z"
                        }
                    ]
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ChatResponse::class.java)

        assertEquals("query", response.intent)
        assertEquals("Bench Press", response.history?.exerciseName)
        assertEquals("standard", response.history?.variantName)
        assertEquals(1, response.history?.entries?.size)
        assertEquals(135.0, response.history!!.entries[0].sets[0].weight!!, 0.01)
        assertEquals(8, response.history!!.entries[0].sets[0].reps)
    }

    @Test
    fun deserialize_unknownIntent() {
        val json = """
            {
                "intent": "unknown",
                "message": "I didn't understand."
            }
        """.trimIndent()

        val response = gson.fromJson(json, ChatResponse::class.java)

        assertEquals("unknown", response.intent)
        assertEquals("I didn't understand.", response.message)
    }
}
