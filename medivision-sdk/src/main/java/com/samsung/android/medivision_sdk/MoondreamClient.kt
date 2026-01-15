package com.samsung.android.medivision_sdk

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.ByteArrayOutputStream

/**
 * Client for accessing Moondream API endpoints.
 * Supports visual question answering, object detection, captioning, and more.
 */
class MoondreamClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.moondream.ai/v1/"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val service: MoondreamService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MoondreamService::class.java)
    }

    /**
     * Visual Question Answering: Ask questions about images.
     *
     * @param bitmap The image to analyze
     * @param question The natural language question about the image
     * @return The answer to the question or null if failed
     */
    suspend fun query(
        bitmap: Bitmap,
        question: String
    ): String? {
        return try {
            val imageUrl = bitmapToDataUri(bitmap)
            val request = MoondreamQueryRequest(
                imageUrl = imageUrl,
                question = question,
                stream = false
            )

            val response = service.query(
                apiKey = apiKey,
                request = request
            )

            response.answer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Object Pointing: Return precise center coordinates for each object instance.
     *
     * @param bitmap The image to analyze
     * @param objectType The type of object to point to (e.g., "person", "car", "medicine")
     * @return List of point coordinates (normalized 0-1) or null if failed
     */
    suspend fun point(
        bitmap: Bitmap,
        objectType: String
    ): List<Point>? {
        return try {
            android.util.Log.d("MoondreamClient", "=== point() API call ===")
            android.util.Log.d("MoondreamClient", "Request object type: '$objectType'")
            android.util.Log.d("MoondreamClient", "Bitmap size: ${bitmap.width}x${bitmap.height}")

            val imageUrl = bitmapToDataUri(bitmap)
            android.util.Log.d("MoondreamClient", "Image data URI length: ${imageUrl.length} characters")

            val request = MoondreamPointRequest(
                imageUrl = imageUrl,
                `object` = objectType
            )

            android.util.Log.d("MoondreamClient", "Sending request to Moondream API...")

            val response = service.point(
                apiKey = apiKey,
                request = request
            )

            android.util.Log.d("MoondreamClient", "Response received: ${response.points?.size ?: 0} point(s)")
            response.points?.forEachIndexed { index, point ->
                android.util.Log.d("MoondreamClient", "  Point $index: (${point.x}, ${point.y})")
            }

            response.points
        } catch (e: Exception) {
            android.util.Log.e("MoondreamClient", "Error in point() API call", e)
            android.util.Log.e("MoondreamClient", "Exception message: ${e.message}")
            android.util.Log.e("MoondreamClient", "Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Image Captioning: Generate natural language descriptions of images.
     *
     * @param bitmap The image to caption
     * @param length Caption length: "short" or "normal" (default: "normal")
     * @return The generated caption or null if failed
     */
    suspend fun caption(
        bitmap: Bitmap,
        length: String = "normal"
    ): String? {
        return try {
            val imageUrl = bitmapToDataUri(bitmap)
            val request = MoondreamCaptionRequest(
                imageUrl = imageUrl,
                length = length,
                stream = false
            )

            val response = service.caption(
                apiKey = apiKey,
                request = request
            )

            response.caption
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Image Segmentation: Label and segment regions in images.
     *
     * @param bitmap The image to segment
     * @param objectType The type of object to segment (e.g., "person", "car", "medicine")
     * @return List of segments with bounding boxes or null if failed
     */
    suspend fun segment(
        bitmap: Bitmap,
        objectType: String
    ): List<Segment>? {
        return try {
            val imageUrl = bitmapToDataUri(bitmap)
            val request = MoondreamSegmentRequest(
                imageUrl = imageUrl,
                `object` = objectType
            )

            val response = service.segment(
                apiKey = apiKey,
                request = request
            )

            response.segments
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a Bitmap to a base64-encoded data URI string.
     * Automatically resizes large images to avoid request size limits.
     */
    private fun bitmapToDataUri(bitmap: Bitmap): String {
        val base64Image = bitmapToBase64(bitmap)
        return "data:image/jpeg;base64,$base64Image"
    }

    /**
     * Converts a Bitmap to base64 string.
     * Resizes bitmap if it's too large (max 1024px on longest side).
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if it's too large to avoid request size limits (max 10MB)
        val resizedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val scale = 1024f / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
