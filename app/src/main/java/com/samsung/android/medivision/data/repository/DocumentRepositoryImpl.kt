package com.samsung.android.medivision.data.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.PrescriptionData
import com.samsung.android.medivision.domain.repository.DocumentRepository
import com.samsung.android.medivision_sdk.OpenRouterClient

class DocumentRepositoryImpl(
    private val openRouterClient: OpenRouterClient
) : DocumentRepository {
    
    private val prescriptionPrompt = """
        Please extract all medicine information from this medical prescription. 
        For each medicine, include:
        1. Name of the medicine
        2. Dosage instructions (e.g., 500mg, 1 tablet)
        3. Frequency (e.g., twice a day, before meals)
        4. Duration (if mentioned, e.g., for 5 days)
        
        Format the output in a json format. If no medicines are found, return empty json.
        Structure of json should only contain following fields:
        {
            "medicines": {
                "name": <Medicine name>,
                "dosage": <Dosage instructions>,
                "frequency": <Frequency>,
                "duration": <Duration>
            } 
        }
        Duration should hold one of three values - "Morning", "Afternoon", "Evening".
        Whatever information is present about duration in the prescription, try to map it to
        the above three categories.
    """.trimIndent()
    
    override suspend fun processPrescription(bitmap: Bitmap): Result<PrescriptionData> {
        return try {
            val result = openRouterClient.generateTextFromImage(
                model = "google/gemini-3-flash-preview",
                prompt = prescriptionPrompt,
                bitmap = bitmap
            )
            
            if (result != null) {
                Result.success(
                    PrescriptionData(
                        extractedText = result,
                        isSuccess = true
                    )
                )
            } else {
                Result.failure(Exception("Failed to process prescription."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
