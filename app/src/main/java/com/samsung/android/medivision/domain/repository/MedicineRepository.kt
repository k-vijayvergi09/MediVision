package com.samsung.android.medivision.domain.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.MedicineIdentification

interface MedicineRepository {
    suspend fun identifyMedicine(bitmap: Bitmap?, pdfBytes: ByteArray?, fileName: String, isPdf: Boolean): Result<MedicineIdentification>
}
