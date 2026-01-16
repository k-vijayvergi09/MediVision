package com.samsung.android.medivision.domain.usecase

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.PrescriptionData
import com.samsung.android.medivision.domain.repository.PrescriptionRepository

class ProcessPrescriptionUseCase(
    private val repository: PrescriptionRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<PrescriptionData> {
        return repository.processPrescription(bitmap)
    }
}
