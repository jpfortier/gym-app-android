package dev.gymapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dev.gymapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

data class UpdateInfo(val versionCode: Int, val versionName: String)

sealed class UpdateResult {
    data object UpToDate : UpdateResult()
    data class Available(val info: UpdateInfo) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class UpdateHelper(private val context: Context) {

    private val baseUrl = BuildConfig.UPDATE_BASE_URL.trimEnd('/')
    private val versionUrl = "$baseUrl/version.json"
    private val apkUrl = "$baseUrl/apk/gym-app-debug.apk"

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val json = URL(versionUrl).readText()
            val info = parseVersionJson(json) ?: return@withContext UpdateResult.Error("Invalid version.json")
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                UpdateResult.Available(info)
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Check failed")
        }
    }

    suspend fun downloadAndInstall(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.cacheDir, "gym-app-update.apk")
            URL(apkUrl).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                installApk(file)
            }
        }
    }

    private fun installApk(file: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun parseVersionJson(json: String): UpdateInfo? {
        return try {
            val obj = org.json.JSONObject(json)
            UpdateInfo(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.optString("versionName", "?")
            )
        } catch (_: Exception) {
            null
        }
    }
}
