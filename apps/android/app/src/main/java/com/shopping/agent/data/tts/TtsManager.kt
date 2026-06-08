package com.shopping.agent.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var enabled = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINESE) ?: TextToSpeech.LANG_MISSING_DATA
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                }
            }
        }
    }

    fun isEnabled(): Boolean = enabled && isInitialized

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) stop()
    }

    fun toggleEnabled(): Boolean {
        enabled = !enabled
        if (!enabled) stop()
        return enabled
    }

    /** 播报增量文本片段，由调用方负责追踪已播报进度 */
    fun speak(text: String) {
        if (!isEnabled() || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "tts_${System.currentTimeMillis()}")
    }

    /** 播报完整文本，先清空队列再播报 */
    fun speakFull(text: String) {
        if (!isEnabled() || text.isEmpty()) return
        stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
