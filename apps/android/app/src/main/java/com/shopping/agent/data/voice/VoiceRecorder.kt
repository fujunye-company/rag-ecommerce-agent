package com.shopping.agent.data.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0L

    val isRecording: Boolean get() = recorder != null
    val durationMs: Long get() = if (startedAt == 0L) 0L else System.currentTimeMillis() - startedAt

    fun start(context: Context, prefix: String = "voice"): File {
        check(recorder == null) { "Recorder is already running" }
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.m4a")
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(64000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        outputFile = file
        startedAt = System.currentTimeMillis()
        return file
    }

    fun amplitude(): Int = try {
        recorder?.maxAmplitude ?: 0
    } catch (_: Exception) {
        0
    }

    fun stop(): RecordingResult {
        val active = recorder ?: throw IllegalStateException("Recorder is not running")
        val file = outputFile ?: throw IllegalStateException("Recording file is missing")
        val duration = durationMs
        try {
            active.stop()
        } finally {
            active.release()
            recorder = null
            outputFile = null
            startedAt = 0L
        }
        return RecordingResult(file = file, durationMs = duration)
    }

    fun cancel() {
        val file = outputFile
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile = null
        startedAt = 0L
        file?.delete()
    }
}

data class RecordingResult(
    val file: File,
    val durationMs: Long,
) {
    val durationSec: Int = (durationMs / 1000L).toInt().coerceAtLeast(1)
}
