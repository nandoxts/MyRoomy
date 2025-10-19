package com.example.myroomy.data.api

import com.example.myroomy.data.models.CulqiTokenRequest
import com.example.myroomy.data.models.CulqiTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface CulqiService {

    @POST("v2/tokens")
    suspend fun createToken(
        @Header("Authorization") authorization: String,
        @Body request: CulqiTokenRequest
    ): Response<CulqiTokenResponse>
}