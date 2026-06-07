package com.shopping.agent.data.voice

import java.io.File

object AudioCompressor {
    private const val MAX_BYTES = 25L * 1024L * 1024L

    fun prepareForUpload(file: File): File {
        require(file.exists() && file.length() > 0L) { "录音文件为空" }
        require(file.length() <= MAX_BYTES) { "录音超过 25MB，请缩短后重试" }
        return file
    }
}
