package com.samsung.android.medivision.data.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.MedicineIdentification
import com.samsung.android.medivision.domain.repository.MedicineRepository
import com.samsung.android.medivision_sdk.OpenRouterClient

class MedicineRepositoryImpl(
    private val openRouterClient: OpenRouterClient
) : MedicineRepository {
    
    override suspend fun identifyMedicine(bitmap: Bitmap): Result<MedicineIdentification> {
        return try {
            val result = openRouterClient.generateTextFromImage(
                model = "openai/gpt-4o",
                prompt = "Identify the medicine in this image and provide a brief description of its common uses. If you cannot identify it, please say so.",
                bitmap = bitmap
            )
            
            if (result != null) {
                Result.success(
                    MedicineIdentification(
                        description = result,
                        isSuccess = true
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
