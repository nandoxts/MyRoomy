package com.example.myroomy.data.models

import com.google.gson.annotations.SerializedName

data class CulqiTokenResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("object")
    val objectType: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("card_number")
    val cardNumber: String?,

    @SerializedName("creation_date")
    val creationDate: Long?,

    @SerializedName("merchant_message")
    val merchantMessage: String?,

    @SerializedName("user_message")
    val userMessage: String?,

    @SerializedName("iin")
    val iin: Map<String, Any>?
)