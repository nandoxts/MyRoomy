package com.example.myroomy.data.models

import com.google.gson.annotations.SerializedName

data class CulqiTokenRequest(
    @SerializedName("card_number")
    val cardNumber: String,

    @SerializedName("cvv")
    val cvv: String,

    @SerializedName("expiration_month")
    val expirationMonth: String,

    @SerializedName("expiration_year")
    val expirationYear: String,

    @SerializedName("email")
    val email: String
)