package com.samsung.android.medivision.data.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.data.util.Prompts
import com.samsung.android.medivision.domain.model.MedicineIdentification
import com.samsung.android.medivision.domain.model.MedicineResponse
import com.samsung.android.medivision.domain.repository.MedicineRepository
import com.samsung.android.medivision_sdk.OpenRouterClient
import kotlinx.serialization.json.Json

class MedicineRepositoryImpl(
    private val openRouterClient: OpenRouterClient
) : MedicineRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    override suspend fun identifyMedicine(bitmap: Bitmap?, pdfBytes: ByteArray?, fileName: String, isPdf: Boolean): Result<MedicineIdentification> {
        return try {
            val prompt = Prompts.EXTRACT_MEDICINE_INFO

            val result = if (isPdf && pdfBytes != null) {
                openRouterClient.generateTextFromPdf(
                    model = "openai/gpt-4o",
                    prompt = prompt,
                    pdfBytes = pdfBytes,
                    fileName = fileName
                )
            } else if (bitmap != null) {
                openRouterClient.generateTextFromImage(
                    model = "openai/gpt-4o",
                    prompt = prompt,
                    bitmap = bitmap
                )
            } else {
                null
            }
            
            if (result != null) {
                // Try to parse the JSON response
                val parsedResponse = try {
                    // Clean up the response - remove markdown code blocks if present
                    val cleanedResult = result
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    json.decodeFromString<MedicineResponse>(cleanedResult)
                } catch (e: Exception) {
                    // If parsing fails, return null to indicate fallback to raw text
                    null
                }

                Result.success(
                    MedicineIdentification(
                        description = result,
                        isSuccess = true,
                        medicines = parsedResponse?.medicines ?: emptyList()
                    )
                )
            } else {
                Result.failure(Exception("Failed to identify medicine."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
