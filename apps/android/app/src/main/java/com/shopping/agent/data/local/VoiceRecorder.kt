package com.shopping.agent.data.local

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {
    companion object {
        private const val TAG = "VoiceRecorder"
        private const val AUDIO_FILE_EXT = "m4a"
        const val MIME_TYPE = "audio/mp4"
        private const val MAX_RECORD_SECONDS = 60
        private const val AUDIO_SAMPLING_RATE = 16000
        private const val AUDIO_BIT_RATE = 64000
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var recordStartTime: Long = 0L

    interface RecordCallback {
        fun onStart() {}
        fun onVolume(volume: Float) {}
        fun onResult(audioFile: File) {}
        fun onError(message: String) {}
    }

    fun start(callback: RecordCallback) {
        if (isRecording) return
        try {
            outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.$AUDIO_FILE_EXT")
            outputFile?.createNewFile()
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(AUDIO_SAMPLING_RATE)
                setAudioEncodingBitRate(AUDIO_BIT_RATE)
                setOutputFile(outputFile!!.absolutePath)
                setMaxDuration(MAX_RECORD_SECONDS * 1000)
                prepare()
                start()
            }
            isRecording = true
            recordStartTime = System.currentTimeMillis()
            Log.i(TAG, "录音开始: ${outputFile?.absolutePath}")
            callback.onStart()
            startVolumeMonitor(callback)
        } catch (e: IOException) {
            cleanup()
            callback.onError("录音启动失败: ${e.localizedMessage ?: "麦克风可能被占用"}")
        } catch (e: SecurityException) {
            cleanup()
            callback.onError("缺少录音权限")
        } catch (e: Exception) {
            cleanup()
            callback.onError("录音异常: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    fun stop(): File? {
        if (!isRecording) return null
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { reset() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止录音异常: ${e.message}", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        val file = outputFile
        val sizeBytes = file?.length() ?: 0L
        Log.i(TAG, "录音结束: ${file?.absolutePath}, size=${sizeBytes / 1024}KB")
        if (sizeBytes < 500) { file?.delete(); return null }
        return file
    }

    fun cancel() {
        try { mediaRecorder?.apply { try { stop() } catch (_: Exception) {}; try { reset() } catch (_: Exception) {}; try { release() } catch (_: Exception) {} } } catch (_: Exception) {}
        mediaRecorder = null; isRecording = false
        outputFile?.delete(); outputFile = null
    }

    fun isRecording(): Boolean = isRecording

    private fun startVolumeMonitor(callback: RecordCallback) {
        val recorder = mediaRecorder ?: return
        Thread {
            try {
                while (isRecording && mediaRecorder != null) {
                    val maxAmplitude = try { recorder.maxAmplitude } catch (_: Exception) { 0 }
                    val volume = if (maxAmplitude > 0) (20.0 * Math.log10(maxAmplitude.toDouble()) / 6.0).coerceIn(0.0, 10.0).toFloat() else 0f
                    callback.onVolume(volume)
                    Thread.sleep(200L)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun cleanup() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null; isRecording = false
        outputFile?.delete(); outputFile = null
    }
}
