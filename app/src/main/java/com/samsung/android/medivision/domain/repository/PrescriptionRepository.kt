package com.samsung.android.medivision.domain.repository

import android.graphics.Bitmap
import com.samsung.android.medivision.domain.model.PrescriptionData

interface PrescriptionRepository {
    suspend fun processPrescription(bitmap: Bitmap): Result<PrescriptionData>
}
