package com.samsung.android.medivision.presentation.medicineidentification

import android.graphics.Bitmap

data class MedicineIdentificationState(
    val selectedBitmap: Bitmap? = null,
    val identificationResult: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
