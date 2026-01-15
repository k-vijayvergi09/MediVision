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

class OpenRouterClient(
    private val apiKey: String,
    private val baseUrl: String = "https://openrouter.ai/api/v1/",
    private val siteUrl: String? = null,
    private val siteName: String? = null
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    private val service: OpenRouterService by lazy {
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
            .create(OpenRouterService::class.java)
    }

    /**
     * Generates text from an image using Open Router.
     * 
     * @param model The model to use (e.g., "google/gemini-pro-1.5")
     * @param prompt The text prompt to accompany the image
     * @param bitmap The image to process
     * @return The generated text or null if failed
     */
    suspend fun generateTextFromImage(
        model: String,
        prompt: String,
        bitmap: Bitmap
    ): String? {
        return try {
            val base64Image = bitmapToBase64(bitmap)
            val imageUrl = "data:image/jpeg;base64,$base64Image"

            val request = OpenRouterRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            MessageContent.Text(prompt),
                            MessageContent.ImageUrl(ImageUrlData(imageUrl))
                        )
                    )
                )
            )

            val response = service.getChatCompletions(
                authorization = "Bearer $apiKey",
                referer = siteUrl,
                title = siteName,
                request = request
            )
            
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates text from a text prompt using Open Router (text-only, no image).
     * 
     * @param model The model to use (e.g., "openai/gpt-4o")
     * @param prompt The text prompt
     * @return The generated text or null if failed
     */
    suspend fun generateTextFromText(
        model: String,
        prompt: String
    ): String? {
        return try {
            val request = OpenRouterRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            MessageContent.Text(prompt)
                        )
                    )
                )
            )

            val response = service.getChatCompletions(
                authorization = "Bearer $apiKey",
                referer = siteUrl,
                title = siteName,
                request = request
            )
            
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates text from a PDF file using Open Router.
     * 
     * @param model The model to use (e.g., "openai/gpt-4o")
     * @param prompt The text prompt to accompany the PDF
     * @param pdfBytes The PDF file as byte array
     * @param fileName The name of the PDF file
     * @return The generated text or null if failed
     */
    suspend fun generateTextFromPdf(
        model: String,
        prompt: String,
        pdfBytes: ByteArray,
        fileName: String
    ): String? {
        return try {
            val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
            val pdfDataUrl = "data:application/pdf;base64,$base64Pdf"

            val request = OpenRouterRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            MessageContent.Text(prompt),
                            MessageContent.File(
                                file = FileData(
                                    filename = fileName,
                                    fileData = pdfDataUrl
                                )
                            )
                        )
                    )
                )
            )

            val response = service.getChatCompletions(
                authorization = "Bearer $apiKey",
                referer = siteUrl,
                title = siteName,
                request = request
            )
            
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if it's too large to avoid request size limits
        val resizedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val scale = 1024f / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
