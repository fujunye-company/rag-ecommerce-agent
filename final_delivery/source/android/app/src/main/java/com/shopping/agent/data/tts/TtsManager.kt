package com.shopping.agent.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var enabled = false

    /** 已播报过的字符数，用于增量播报 */
    private var spokenLength = 0

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
        if (!enabled) {
            stop()
        }
    }

    fun toggleEnabled(): Boolean {
        enabled = !enabled
        if (!enabled) stop() else spokenLength = 0
        return enabled
    }

    /** 增量播报：只播报新增的文本部分 */
    fun speakIncremental(fullText: String) {
        if (!isEnabled() || fullText.isEmpty()) return
        val newText = fullText.substring(spokenLength)
        if (newText.isBlank()) return
        spokenLength = fullText.length

        tts?.speak(newText, TextToSpeech.QUEUE_ADD, null, "tts_${System.currentTimeMillis()}")
    }

    /** 播报完整文本（用于消息重播） */
    fun speakFull(text: String) {
        if (!isEnabled() || text.isEmpty()) return
        stop()
        spokenLength = text.length
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    /** 流式开始/结束标记管理 */
    fun resetForNewMessage() {
        stop()
        spokenLength = 0
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
