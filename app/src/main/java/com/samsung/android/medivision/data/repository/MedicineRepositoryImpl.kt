package com.samsung.android.medivision.data.repository

import android.graphics.Bitmap
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
            val prompt = """
                Extract medicine information from this prescription and return ONLY a valid JSON response in the following exact format:

                {
                  "medicines": [
                    {
                      "name": "Medicine name",
                      "when_to_take": "Morning",
                      "frequency": 1
                    }
                  ]
                }

                CRITICAL RULES:
                1. "when_to_take" must be EXACTLY one of: "Morning", "Evening", or "Both"
                2. "frequency" must be a number: 1 (once daily) or 2 (twice daily)
                3. If dosage timing is not mentioned or unclear, use "Morning" as default
                4. If frequency is not mentioned or unclear, use 1 as default
                5. Return ONLY the JSON object, no additional text or explanation

                Common prescription patterns to recognize:
                - "OD" / "Once daily" / "1 time" = frequency: 1
                - "BD" / "BID" / "Twice daily" / "2 times" = frequency: 2
                - "Morning" / "AM" / "Before breakfast" = when_to_take: "Morning"
                - "Evening" / "PM" / "Before dinner" / "Night" = when_to_take: "Evening"
                - "Morning and evening" / "AM & PM" / "Twice" = when_to_take: "Both"

                If no medicines are found, return: {"medicines": []}
            """.trimIndent()

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
