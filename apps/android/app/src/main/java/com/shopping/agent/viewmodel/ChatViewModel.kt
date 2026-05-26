package com.shopping.agent.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.mock.mockProducts
import com.shopping.agent.data.model.*
import com.shopping.agent.data.remote.SseClient
import com.shopping.agent.data.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class GuideUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isRecording: Boolean = false,
    val selectedImageUri: Uri? = null,

    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val streamingCards: List<Product> = emptyList(),
    val searchStatus: String = "",

    val clarifyChips: List<String> = emptyList(),

    val screenState: ScreenState = ScreenState.Idle,
    val sessionId: String = UUID.randomUUID().toString(),
)

sealed class ScreenState {
    data object Idle : ScreenState()
    data object Loading : ScreenState()
    data class Streaming(val partialText: String = "") : ScreenState()
    data class Content(val hasProducts: Boolean = false) : ScreenState()
    data class Error(val message: String) : ScreenState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private val chatRepository = ChatRepository()
    private val visionClient = SseClient()
    private val userRepo = UserRepository(application)

    init {
        viewModelScope.launch {
            // 恢复持久化的 sessionId（安装后复用，保证跨重启同一会话）
            val savedId = userRepo.getSetting("chat_session_id")
            val convId = if (savedId.isNotEmpty()) savedId else _uiState.value.sessionId
            if (savedId.isEmpty()) {
                userRepo.setSetting("chat_session_id", convId)
            }
            _uiState.update { it.copy(sessionId = convId) }

            // 加载历史消息
            userRepo.createConversation(convId, "")
            val messages = userRepo.getMessages(convId)
            if (messages.isNotEmpty()) {
                _uiState.update {
                    it.copy(messages = messages, screenState = ScreenState.Content(true))
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendDailyGreeting() {
        val greeting = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.Assistant,
            content = "fujunye，早上好\n\n以下是今日推荐：",
            status = MessageStatus.Sent,
        )
        val products = mockProducts.take(3)
        val productMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.Assistant,
            content = "",
            productCards = products,
            status = MessageStatus.Sent,
        )
        _uiState.update {
            it.copy(messages = listOf(greeting, productMsg),
                screenState = ScreenState.Content(true))
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.User,
            content = text,
            status = MessageStatus.Sent,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                screenState = ScreenState.Streaming(""),
                isStreaming = true,
                streamingText = "",
                streamingCards = emptyList(),
                clarifyChips = emptyList(),
                searchStatus = "正在搜索中…",
            )
        }

        // ── 真实 SSE 流（替代 simulateStreamingResponse） ──
        viewModelScope.launch {
            val accText = StringBuilder()
            val accCards = mutableListOf<Product>()

            try {
                chatRepository.sendMessage(text, _uiState.value.sessionId)
                    .collect { event ->
                        when (event) {
                            is SSEEvent.Progress -> {
                                _uiState.update { it.copy(searchStatus = event.message) }
                            }
                            is SSEEvent.TextDelta -> {
                                accText.append(event.content)
                                _uiState.update { it.copy(streamingText = accText.toString()) }
                            }
                            is SSEEvent.ProductCard -> {
                                val product = Product(
                                    productId = event.productId,
                                    title = event.title,
                                    price = event.price,
                                    rating = event.rating.toFloat(),
                                    highlights = event.highlights,
                                    imageUrl = event.imageUrl,
                                    imageUrls = event.imageUrls,
                                    brand = event.brand,
                                    category = event.category,
                                    matchScore = event.matchScore,
                                )
                                accCards.add(product)
                                _uiState.update { it.copy(streamingCards = accCards.toList()) }
                            }
                            is SSEEvent.Done -> {
                                val finalText = accText.toString()
                                val finalCards = accCards.toList()

                                val aiMessage = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    role = MessageRole.Assistant,
                                    content = finalText,
                                    productCards = finalCards,
                                    status = MessageStatus.Sent,
                                )

                                _uiState.update {
                                    it.copy(
                                        messages = it.messages + aiMessage,
                                        isStreaming = false,
                                        streamingText = "",
                                        streamingCards = emptyList(),
                                        searchStatus = "",
                                        screenState = ScreenState.Content(finalCards.isNotEmpty()),
                                    )
                                }

                                // 持久化: 用户消息 + AI 回复
                                val convId = _uiState.value.sessionId
                                userRepo.saveMessage(userMessage, convId)
                                userRepo.saveMessage(aiMessage, convId)

                                // 生成反选 chips: 排除推荐商品中的品牌
                                if (finalCards.isNotEmpty()) {
                                    val brands = finalCards
                                        .mapNotNull { it.brand }
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                        .take(4)
                                    val excludeChips = brands.map { "排除 $it" }
                                    _uiState.update { it.copy(clarifyChips = excludeChips) }
                                }
                            }
                            is SSEEvent.Error -> {
                                _uiState.update {
                                    it.copy(
                                        searchStatus = "",
                                        screenState = ScreenState.Error(event.message),
                                        isStreaming = false,
                                    )
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchStatus = "",
                        screenState = ScreenState.Error("网络连接失败：${e.message}"),
                        isStreaming = false,
                    )
                }
            }
        }
    }

    fun onClarifyChipClick(chip: String) {
        _uiState.update { it.copy(inputText = chip, clarifyChips = emptyList()) }
        viewModelScope.launch {
            delay(50)
            sendMessage()
        }
    }

    fun clearImage() {
        _uiState.update { it.copy(selectedImageUri = null) }
    }

    fun retryLastMessage() {
        val lastUserMsg = _uiState.value.messages.lastOrNull { it.role == MessageRole.User }
        if (lastUserMsg != null) {
            _uiState.update { it.copy(inputText = lastUserMsg.content) }
            sendMessage()
        }
    }

    // ── 拍照找货 ──────────────────────────────────────────

    fun sendImage(imageFile: File) {
        _uiState.update {
            it.copy(
                screenState = ScreenState.Streaming(""),
                isStreaming = true,
                streamingText = "",
                streamingCards = emptyList(),
                searchStatus = "正在识别图片…",
            )
        }

        viewModelScope.launch {
            val accText = StringBuilder()
            val accCards = mutableListOf<Product>()

            try {
                visionClient.connectVision(imageFile)
                    .collect { event ->
                        when (event) {
                            is SSEEvent.Progress -> {
                                accText.append(event.message).append("\n")
                                _uiState.update {
                                    it.copy(
                                        searchStatus = event.message,
                                        streamingText = accText.toString()
                                    )
                                }
                            }
                            is SSEEvent.ProductCard -> {
                                val product = Product(
                                    productId = event.productId,
                                    title = event.title,
                                    price = event.price,
                                    rating = event.rating.toFloat(),
                                    highlights = event.highlights,
                                    imageUrl = event.imageUrl,
                                    imageUrls = event.imageUrls,
                                    brand = event.brand,
                                    category = event.category,
                                    matchScore = event.matchScore,
                                )
                                accCards.add(product)
                                _uiState.update { it.copy(streamingCards = accCards.toList()) }
                            }
                            is SSEEvent.Done -> {
                                val finalText = accText.toString().ifBlank {
                                    "根据图片找到 ${event.totalCards} 件相似商品"
                                }
                                val finalCards = accCards.toList()

                                // 添加为 ChatMessage
                                val aiMessage = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    role = MessageRole.Assistant,
                                    content = finalText,
                                    productCards = finalCards,
                                    status = MessageStatus.Sent,
                                )
                                _uiState.update {
                                    it.copy(
                                        messages = it.messages + aiMessage,
                                        isStreaming = false,
                                        streamingText = "",
                                        streamingCards = emptyList(),
                                        searchStatus = "",
                                        screenState = ScreenState.Content(finalCards.isNotEmpty()),
                                    )
                                }
                            }
                            is SSEEvent.Error -> {
                                _uiState.update {
                                    it.copy(
                                        searchStatus = "",
                                        screenState = ScreenState.Error(event.message),
                                        isStreaming = false,
                                    )
                                }
                            }
                            else -> {}  // TextDelta not expected from vision-search
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchStatus = "",
                        screenState = ScreenState.Error("拍照找货失败：${e.message}"),
                        isStreaming = false,
                    )
                }
            }
        }
    }
}
