package com.samsung.android.medivision.data.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * ML Kit OCR client for on-device text recognition.
 * Provides fast, offline text extraction from images.
 */
class MlKitOcrClient {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts text from a bitmap image using ML Kit Text Recognition.
     *
     * @param bitmap The image to extract text from
     * @return The extracted text, or null if no text was found or an error occurred
     */
    suspend fun recognizeText(bitmap: Bitmap): String? = suspendCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                if (extractedText.isNotBlank()) {
                    continuation.resume(extractedText)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    /**
     * Extracts text with detailed block/line/element information including bounding boxes.
     * Useful for structured text analysis and visual overlays.
     *
     * @param bitmap The image to extract text from
     * @return OcrResult containing the full text and structured blocks with bounding boxes
     */
    suspend fun recognizeTextWithDetails(bitmap: Bitmap): OcrResult = suspendCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    OcrBlock(
                        text = block.text,
                        boundingBox = block.boundingBox?.toNormalizedBoundingBox(imageWidth, imageHeight),
                        lines = block.lines.map { line ->
                            OcrLine(
                                text = line.text,
                                boundingBox = line.boundingBox?.toNormalizedBoundingBox(imageWidth, imageHeight),
                                elements = line.elements.map { element ->
                                    OcrElement(
                                        text = element.text,
                                        boundingBox = element.boundingBox?.toNormalizedBoundingBox(imageWidth, imageHeight)
                                    )
                                }
                            )
                        }
                    )
                }
                continuation.resume(
                    OcrResult(
                        fullText = visionText.text,
                        blocks = blocks,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight
                    )
                )
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    /**
     * Closes the text recognizer and releases resources.
     * Call this when the client is no longer needed.
     */
    fun close() {
        recognizer.close()
    }

    private fun Rect.toNormalizedBoundingBox(imageWidth: Int, imageHeight: Int): NormalizedBoundingBox {
        return NormalizedBoundingBox(
            left = left.toFloat() / imageWidth,
            top = top.toFloat() / imageHeight,
            right = right.toFloat() / imageWidth,
            bottom = bottom.toFloat() / imageHeight
        )
    }
}

/**
 * Result from OCR containing structured text data.
 */
data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

/**
 * A block of text detected in the image.
 */
data class OcrBlock(
    val text: String,
    val boundingBox: NormalizedBoundingBox? = null,
    val lines: List<OcrLine>
)

/**
 * A line of text within a block.
 */
data class OcrLine(
    val text: String,
    val boundingBox: NormalizedBoundingBox? = null,
    val elements: List<OcrElement>
)

/**
 * A single element (usually a word) within a line.
 */
data class OcrElement(
    val text: String,
    val boundingBox: NormalizedBoundingBox? = null
)

/**
 * Normalized bounding box with coordinates in 0-1 range.
 * (0,0) is top-left, (1,1) is bottom-right.
 */
data class NormalizedBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}
