package com.samsung.android.medivision.data.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.data.util.Prompts
import com.samsung.android.medivision.domain.model.PrescriptionData
import com.samsung.android.medivision.domain.repository.PrescriptionRepository
import com.samsung.android.medivision_sdk.OpenRouterClient

class PrescriptionRepositoryImpl(
    private val openRouterClient: OpenRouterClient
) : PrescriptionRepository {
    
    private val prescriptionPrompt = Prompts.READ_PRESCRIPTION
    
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
