package com.samsung.android.medivision.domain.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.PrescriptionData

interface DocumentRepository {
    suspend fun processPrescription(bitmap: Bitmap): Result<PrescriptionData>
}
