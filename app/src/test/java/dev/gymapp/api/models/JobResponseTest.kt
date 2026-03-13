package dev.gymapp.api.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobResponseTest {

    private val gson = Gson()

    @Test
    fun deserialize_processing() {
        val json = """
            {
                "job_id": "job-abc-123",
                "text": "bench press 135 for 8",
                "status": "processing",
                "result": null,
                "error": null
            }
        """.trimIndent()

        val response = gson.fromJson(json, JobResponse::class.java)

        assertEquals("job-abc-123", response.jobId)
        assertEquals("bench press 135 for 8", response.text)
        assertEquals("processing", response.status)
        assertNull(response.result)
        assertNull(response.error)
    }

    @Test
    fun deserialize_complete() {
        val json = """
            {
                "job_id": "job-xyz",
                "text": "deadlift 200",
                "status": "complete",
                "result": {
                    "intent": "log",
                    "message": "Logged.",
                    "entries": [],
                    "prs": []
                },
                "error": null
            }
        """.trimIndent()

        val response = gson.fromJson(json, JobResponse::class.java)

        assertEquals("job-xyz", response.jobId)
        assertEquals("complete", response.status)
        assertEquals("log", response.result?.intent)
        assertEquals("Logged.", response.result?.message)
    }

    @Test
    fun deserialize_failed() {
        val json = """
            {
                "job_id": "job-fail",
                "text": "something",
                "status": "failed",
                "result": null,
                "error": "Transcription failed"
            }
        """.trimIndent()

        val response = gson.fromJson(json, JobResponse::class.java)

        assertEquals("failed", response.status)
        assertEquals("Transcription failed", response.error)
    }
}
