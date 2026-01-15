package com.samsung.android.medivision.data.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.MedicineIdentification
import com.samsung.android.medivision.domain.repository.MedicineRepository
import com.samsung.android.medivision_sdk.OpenRouterClient

class MedicineRepositoryImpl(
    private val openRouterClient: OpenRouterClient
) : MedicineRepository {
    
    override suspend fun identifyMedicine(bitmap: Bitmap?, pdfBytes: ByteArray?, fileName: String, isPdf: Boolean): Result<MedicineIdentification> {
        return try {
            val result = if (isPdf && pdfBytes != null) {
                // For PDFs, send the PDF file as base64-encoded data URL
                val prompt = "Please identify the medicine(s) described in this PDF document and provide a brief description of their common uses. If you cannot identify any medicine, please say so."
                openRouterClient.generateTextFromPdf(
                    model = "openai/gpt-4o",
                    prompt = prompt,
                    pdfBytes = pdfBytes,
                    fileName = fileName
                )
            } else if (bitmap != null) {
                // For images, use the existing image-based identification
                val prompt = "Identify the medicine in this image and provide a brief description of its common uses. If you cannot identify it, please say so."
                openRouterClient.generateTextFromImage(
                    model = "openai/gpt-4o",
                    prompt = prompt,
                    bitmap = bitmap
                )
            } else {
                null
            }
            
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
