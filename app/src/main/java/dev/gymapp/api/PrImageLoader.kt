package dev.gymapp.api

import kotlinx.coroutines.delay
import java.util.concurrent.TimeoutException

/**
 * Polls GET /prs/{id}/image until the image is ready.
 * - 404: Image not ready (DALL-E still generating) → retry
 * - 302/200: Image ready → OkHttp follows redirect, returns image bytes
 *
 * Interval: 4 seconds, Timeout: 30 seconds (DALL-E usually ~30 sec; if not ready, show "Image not available").
 *
 * Note: Images are generated when the PR is created (via chat). If generation failed or was never
 * triggered, 404 is returned until timeout. There is no endpoint to trigger generation on demand;
 * the backend would need POST /prs/{id}/generate-image (or similar) to support that.
 */
class PrImageLoader(private val api: GymApi) {

    companion object {
        private const val POLL_INTERVAL_MS = 4_000L
        private const val TIMEOUT_MS = 30_000L
    }

    suspend fun loadPrImage(prId: String): Result<ByteArray> {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < TIMEOUT_MS) {
            val response = api.prImage(prId)
            when {
                response.isSuccessful -> {
                    val bytes = response.body()?.bytes()
                    return if (bytes != null && bytes.isNotEmpty()) {
                        Result.success(bytes)
                    } else {
                        Result.failure(IllegalStateException("Empty image body"))
                    }
                }
                response.code() == 404 -> {
                    delay(POLL_INTERVAL_MS)
                }
                else -> {
                    return Result.failure(
                        IllegalStateException("prs/$prId/image: ${response.code()} ${response.message()}")
                    )
                }
            }
        }
        return Result.failure(
            TimeoutException("PR image not ready after ${TIMEOUT_MS / 1000}s")
        )
    }
}
