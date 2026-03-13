package dev.gymapp.api

import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException

/**
 * Polls GET /prs/{id}/image until the image is ready.
 * - 404: Image not ready (DALL-E still generating) → retry
 * - 302/200: Image ready → OkHttp follows redirect, returns image bytes
 * - 304: Not Modified → use cached bytes (when If-None-Match sent)
 *
 * Interval: 4 seconds, Timeout: 60 seconds (DALL-E usually ~30 sec).
 */
class PrImageLoader(private val api: GymApi) {

    companion object {
        private const val POLL_INTERVAL_MS = 4_000L
        private const val TIMEOUT_MS = 60_000L
        private const val HTTP_NOT_MODIFIED = 304
    }

    internal val cache = ConcurrentHashMap<String, CacheEntry>()

    internal data class CacheEntry(val etag: String, val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CacheEntry
            if (etag != other.etag) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = 31 * etag.hashCode() + bytes.contentHashCode()
    }

    suspend fun loadPrImage(prId: String): Result<ByteArray> {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < TIMEOUT_MS) {
            val cached = cache[prId]
            val response = api.prImage(prId, cached?.etag)

            when {
                response.code() == HTTP_NOT_MODIFIED -> {
                    val bytes = cached?.bytes
                    return if (bytes != null && bytes.isNotEmpty()) {
                        Result.success(bytes)
                    } else {
                        Result.failure(IllegalStateException("304 but no cached bytes for $prId"))
                    }
                }
                response.isSuccessful -> {
                    val bytes = response.body()?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val etag = response.raw().priorResponse?.header("ETag")
                            ?: response.raw().header("ETag")
                        if (etag != null) {
                            cache[prId] = CacheEntry(etag, bytes)
                        }
                        return Result.success(bytes)
                    } else {
                        return Result.failure(IllegalStateException("Empty image body"))
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
