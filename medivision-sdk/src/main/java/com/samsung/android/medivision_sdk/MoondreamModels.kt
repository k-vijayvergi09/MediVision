package com.samsung.android.medivision_sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request Models
@Serializable
data class MoondreamQueryRequest(
    @SerialName("image_url") val imageUrl: String,
    val question: String,
    val stream: Boolean = false
)

@Serializable
data class MoondreamDetectRequest(
    @SerialName("image_url") val imageUrl: String,
    val `object`: String
)

@Serializable
data class MoondreamPointRequest(
    @SerialName("image_url") val imageUrl: String,
    val `object`: String
)

@Serializable
data class MoondreamCaptionRequest(
    @SerialName("image_url") val imageUrl: String,
    val length: String = "normal", // "short" or "normal"
    val stream: Boolean = false
)

@Serializable
data class MoondreamSegmentRequest(
    @SerialName("image_url") val imageUrl: String,
    val `object`: String
)

// Response Models
@Serializable
data class MoondreamQueryResponse(
    @SerialName("request_id") val requestId: String,
    val answer: String
)

@Serializable
data class MoondreamDetectResponse(
    @SerialName("request_id") val requestId: String,
    val objects: List<DetectedObject>
)

@Serializable
data class DetectedObject(
    @SerialName("x_min") val xMin: Float,
    @SerialName("y_min") val yMin: Float,
    @SerialName("x_max") val xMax: Float,
    @SerialName("y_max") val yMax: Float
)

@Serializable
data class MoondreamPointResponse(
    val points: List<Point>
)

@Serializable
data class Point(
    val x: Float,
    val y: Float
)

@Serializable
data class MoondreamCaptionResponse(
    @SerialName("request_id") val requestId: String,
    val caption: String
)

@Serializable
data class MoondreamSegmentResponse(
    @SerialName("request_id") val requestId: String,
    val segments: List<Segment>
)

@Serializable
data class Segment(
    @SerialName("x_min") val xMin: Float,
    @SerialName("y_min") val yMin: Float,
    @SerialName("x_max") val xMax: Float,
    @SerialName("y_max") val yMax: Float,
    val mask: String? = null // Base64 encoded mask if available
)

// Streaming Response Models
@Serializable
data class MoondreamStreamChunk(
    val chunk: String? = null,
    val completed: Boolean = false
)
