package com.samsung.android.medivision_sdk

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterService {
    @POST("chat/completions")
    suspend fun getChatCompletions(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String?,
        @Header("X-Title") title: String?,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
