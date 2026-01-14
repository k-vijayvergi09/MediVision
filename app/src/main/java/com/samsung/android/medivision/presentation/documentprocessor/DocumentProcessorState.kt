package com.samsung.android.medivision.presentation.documentprocessor

import android.graphics.Bitmap

data class DocumentProcessorState(
    val selectedBitmap: Bitmap? = null,
    val fileName: String = "",
    val extractedText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
