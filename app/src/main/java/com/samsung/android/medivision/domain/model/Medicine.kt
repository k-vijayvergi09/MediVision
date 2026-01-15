package com.samsung.android.medivision.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Medicine(
    @SerialName("name")
    val name: String,
    @SerialName("when_to_take")
    val whenToTake: String, // "Morning", "Evening", or "Both"
    @SerialName("frequency")
    val frequency: Int // 1 or 2
)

@Serializable
data class MedicineResponse(
    @SerialName("medicines")
    val medicines: List<Medicine> = emptyList()
)
