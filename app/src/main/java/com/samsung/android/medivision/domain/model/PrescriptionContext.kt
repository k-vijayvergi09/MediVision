package com.samsung.android.medivision.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PrescriptionContext(
    val id: String,
    val fileName: String,
    val extractedText: String, // Keep raw response as fallback
    val medicines: List<Medicine> = emptyList(), // Structured medicine data
    val timestamp: Long,
    val isPdf: Boolean
)
