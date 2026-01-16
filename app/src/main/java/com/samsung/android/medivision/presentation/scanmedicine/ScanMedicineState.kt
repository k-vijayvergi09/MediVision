package com.samsung.android.medivision.presentation.scanmedicine

import android.graphics.Bitmap
import com.samsung.android.medivision_sdk.Point

data class ScanMedicineState(
    val selectedBitmap: Bitmap? = null,
    val fileName: String = "",
    val extractedText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val medicineCoordinates: List<Point>? = null,
    val isDetectingCoordinates: Boolean = false,
    val applicableMedicines: List<com.samsung.android.medivision.domain.model.Medicine> = emptyList()
)
