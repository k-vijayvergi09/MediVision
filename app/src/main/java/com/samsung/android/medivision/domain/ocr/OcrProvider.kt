package com.samsung.android.medivision.domain.ocr

import android.graphics.Bitmap

/**
 * Interface for OCR (Optical Character Recognition) providers.
 * Allows abstraction over different OCR implementations (ML Kit, cloud APIs, etc.)
 */
interface OcrProvider {

    /**
     * Extracts text from a bitmap image.
     *
     * @param bitmap The image to extract text from
     * @return Result containing the extracted text, or failure if extraction failed
     */
    suspend fun extractText(bitmap: Bitmap): Result<String>

    /**
     * The name/identifier of this OCR provider for logging and debugging.
     */
    val providerName: String
}
