package com.shopping.agent.data.local

import android.content.Context
import android.util.Log
import java.io.File

object AudioCompressor {
    private const val TAG = "AudioCompressor"
    private const val WARN_SIZE = 512 * 1024

    fun compress(context: Context, sourceFile: File): File {
        val fileLen = sourceFile.length()
        if (!sourceFile.exists() || fileLen < 500) {
            Log.w(TAG, "源文件无效: $fileLen bytes")
            return sourceFile
        }
        val sizeKb = sourceFile.length() / 1024
        if (sizeKb > WARN_SIZE / 1024) {
            Log.w(TAG, "音频文件较大: ${sizeKb}KB")
        }
        return sourceFile
    }
}
