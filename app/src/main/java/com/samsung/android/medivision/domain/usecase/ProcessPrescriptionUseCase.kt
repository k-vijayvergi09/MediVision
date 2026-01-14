package com.samsung.android.medivision.domain.usecase

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.PrescriptionData
import com.samsung.android.medivision.domain.repository.DocumentRepository

class ProcessPrescriptionUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<PrescriptionData> {
        return repository.processPrescription(bitmap)
    }
}
