package com.samsung.android.medivision.domain.usecase

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.MedicineIdentification
import com.samsung.android.medivision.domain.repository.MedicineRepository

class IdentifyMedicineUseCase(
    private val repository: MedicineRepository
) {
    suspend operator fun invoke(bitmap: Bitmap?, pdfBytes: ByteArray?, fileName: String, isPdf: Boolean): Result<MedicineIdentification> {
        return repository.identifyMedicine(bitmap, pdfBytes, fileName, isPdf)
    }
}
