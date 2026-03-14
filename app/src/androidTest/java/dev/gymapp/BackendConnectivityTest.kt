package dev.gymapp

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Diagnostic tests: prove backend connectivity and certificate trust.
 * Run on emulator with backend at base.url. Pass = backend reachable.
 *
 * - backendConnectivity: raw OkHttp GET /health. Proves cert trust.
 * - backendChatViaAppApi: app's Retrofit POST /chat. Proves app API client works.
 */
@RunWith(AndroidJUnit4::class)
class BackendConnectivityTest {

    @Before
    fun signOut() {
        (ApplicationProvider.getApplicationContext() as PrTracksApplication).authRepository.signOut()
    }

    @Test
    fun backendConnectivity_capturesExactError() {
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/health"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        try {
            val response = client.newCall(
                Request.Builder().url(url).get().build()
            ).execute()
            Assert.assertTrue(
                "Backend reachable: ${response.code} ${response.body?.string()}",
                response.isSuccessful
            )
        } catch (e: SSLHandshakeException) {
            Assert.fail(
                "CERTIFICATE TRUST FAILURE: SSLHandshakeException - ${e.message}\n" +
                "The emulator does not trust the backend's TLS certificate.\n" +
                "Fix: (1) Ensure backend cert includes 10.0.2.2: mkcert localhost 127.0.0.1 10.0.2.2\n" +
                "     (2) Ensure app's res/raw/mkcert_root_ca.pem matches mkcert -CAROOT/rootCA.pem\n" +
                "     (3) Or use HTTP: base.url=http://10.0.2.2:8081 with backend running without TLS"
            )
        } catch (e: SSLPeerUnverifiedException) {
            Assert.fail(
                "CERTIFICATE TRUST FAILURE: SSLPeerUnverifiedException - ${e.message}\n" +
                "Hostname verification or cert chain issue. See SSLHandshakeException fix above."
            )
        } catch (e: javax.net.ssl.SSLException) {
            Assert.fail(
                "CERTIFICATE/SSL FAILURE: SSLException - ${e.message}\n" +
                "Cause: ${e.cause?.message}"
            )
        } catch (e: ConnectException) {
            Assert.fail(
                "CONNECTION FAILURE: Backend not reachable at $url - ${e.message}\n" +
                "Ensure backend is running (make run in ../gym) and base.url in local.properties is correct."
            )
        } catch (e: SocketTimeoutException) {
            Assert.fail(
                "TIMEOUT: Could not reach $url - ${e.message}\n" +
                "Firewall or backend not responding."
            )
        } catch (e: UnknownHostException) {
            Assert.fail(
                "DNS/HOST FAILURE: Unknown host for $url - ${e.message}"
            )
        } catch (e: Exception) {
            Assert.fail(
                "UNEXPECTED: ${e.javaClass.simpleName} - ${e.message}\n" +
                "Cause: ${e.cause?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "none"}"
            )
        }
    }

    /**
     * Uses the app's Retrofit client to POST /chat with sample audio.
     * Proves the app's API client (with auth) can reach the backend.
     */
    @Test
    fun backendChatViaAppApi() = runBlocking {
        val app = ApplicationProvider.getApplicationContext() as PrTracksApplication
        val devTokenResp = app.api.devToken()
        Assert.assertTrue("Dev token failed: ${devTokenResp.code()}", devTokenResp.isSuccessful)
        val token = devTokenResp.body()?.token
        Assert.assertNotNull("No token in response", token)
        app.authRepository.setTokenForTesting(token!!)

        val sampleBytes = app.assets.open("samples/20260306_133927.m4a").use { it.readBytes() }
        val audioBase64 = Base64.encodeToString(sampleBytes, Base64.NO_WRAP)
        val result = app.chatRepository.postChat(
            dev.gymapp.api.models.ChatRequest(audioBase64 = audioBase64, audioFormat = "m4a")
        )

        Assert.assertTrue(
            "POST /chat failed: ${result.exceptionOrNull()?.message}",
            result.isSuccess
        )
        val postResult = result.getOrThrow()
        when (postResult) {
            is dev.gymapp.api.ChatPostResult.Sync -> {
                Assert.assertNotNull("Expected message in sync response", postResult.response.message)
            }
            is dev.gymapp.api.ChatPostResult.Async -> {
                Assert.assertNotNull("Expected text in async response", postResult.job.text)
                val pollResult = app.chatRepository.pollUntilComplete(postResult.job.jobId)
                Assert.assertTrue("Poll failed: ${pollResult.exceptionOrNull()?.message}", pollResult.isSuccess)
            }
        }
    }
}
