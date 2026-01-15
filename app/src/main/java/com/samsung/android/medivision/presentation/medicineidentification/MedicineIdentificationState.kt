package com.samsung.android.medivision.presentation.medicineidentification

import android.graphics.Bitmap

data class MedicineIdentificationState(
    val selectedBitmap: Bitmap? = null,
    val pdfBytes: ByteArray? = null,
    val fileName: String = "",
    val isPdf: Boolean = false,
    val identificationResult: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MedicineIdentificationState

        if (selectedBitmap != other.selectedBitmap) return false
        if (pdfBytes != null) {
            if (other.pdfBytes == null || !pdfBytes.contentEquals(other.pdfBytes)) return false
        } else if (other.pdfBytes != null) return false
        if (fileName != other.fileName) return false
        if (isPdf != other.isPdf) return false
        if (identificationResult != other.identificationResult) return false
        if (isLoading != other.isLoading) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedBitmap?.hashCode() ?: 0
        result = 31 * result + (pdfBytes?.contentHashCode() ?: 0)
        result = 31 * result + fileName.hashCode()
        result = 31 * result + isPdf.hashCode()
        result = 31 * result + identificationResult.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}
