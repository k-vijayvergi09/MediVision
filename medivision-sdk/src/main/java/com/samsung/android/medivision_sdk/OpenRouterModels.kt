package com.samsung.android.medivision_sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: List<MessageContent>
)

@Serializable
@JsonClassDiscriminator("type")
sealed class MessageContent {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : MessageContent()

    @Serializable
    @SerialName("image_url")
    data class ImageUrl(
        @SerialName("image_url") val imageUrl: ImageUrlData
    ) : MessageContent()
}

@Serializable
data class ImageUrlData(
    val url: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ResponseMessage
)

@Serializable
data class ResponseMessage(
    val content: String?
)
