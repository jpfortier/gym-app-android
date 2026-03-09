package dev.gymapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            outputFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioChannels(1)
                setAudioEncodingBitRate(BIT_RATE)
                prepare()
                start()
            }
            mediaRecorder = recorder
        }
    }

    suspend fun stopAndGetBase64(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val recorder = mediaRecorder ?: throw IllegalStateException("Not recording")
            val file = outputFile ?: throw IllegalStateException("No output file")
            try {
                recorder.stop()
            } catch (e: Exception) {
            }
            recorder.release()
            mediaRecorder = null
            val bytes = file.readBytes()
            file.delete()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    fun cancel() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
    }

    fun isRecording(): Boolean = mediaRecorder != null

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128000
    }
}
