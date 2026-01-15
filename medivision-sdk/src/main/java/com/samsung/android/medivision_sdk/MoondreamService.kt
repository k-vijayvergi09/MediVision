package com.samsung.android.medivision_sdk

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MoondreamService {
    @POST("query")
    suspend fun query(
        @Header("X-Moondream-Auth") apiKey: String,
        @Body request: MoondreamQueryRequest
    ): MoondreamQueryResponse

    @POST("detect")
    suspend fun detect(
        @Header("X-Moondream-Auth") apiKey: String,
        @Body request: MoondreamDetectRequest
    ): MoondreamDetectResponse

    @POST("point")
    suspend fun point(
        @Header("X-Moondream-Auth") apiKey: String,
        @Body request: MoondreamPointRequest
    ): MoondreamPointResponse

    @POST("caption")
    suspend fun caption(
        @Header("X-Moondream-Auth") apiKey: String,
        @Body request: MoondreamCaptionRequest
    ): MoondreamCaptionResponse

    @POST("segment")
    suspend fun segment(
        @Header("X-Moondream-Auth") apiKey: String,
        @Body request: MoondreamSegmentRequest
    ): MoondreamSegmentResponse
}
