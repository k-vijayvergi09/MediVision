package com.samsung.android.medivision.data.ocr

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.ocr.OcrProvider

/**
 * ML Kit implementation of OcrProvider.
 * Uses on-device text recognition for fast, offline OCR.
 */
class MlKitOcrProvider(
    private val mlKitOcrClient: MlKitOcrClient
) : OcrProvider {

    override val providerName: String = "ML Kit Text Recognition"

    override suspend fun extractText(bitmap: Bitmap): Result<String> {
        return try {
            val text = mlKitOcrClient.recognizeText(bitmap)
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(NoTextDetectedException("No text detected in image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Exception thrown when no text is detected in the image.
 */
class NoTextDetectedException(message: String) : Exception(message)
