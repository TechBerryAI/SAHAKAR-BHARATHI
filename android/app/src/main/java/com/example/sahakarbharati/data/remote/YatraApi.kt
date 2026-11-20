package com.example.sahakarbharati.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface YatraApi {
    @GET("api/v1/yatras")
    suspend fun listYatras(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("published") published: Boolean,
    ): YatraPageDto

    @GET("api/v1/yatras/{yatraId}")
    suspend fun getYatra(
        @Header("Authorization") authorization: String,
        @Path("yatraId") yatraId: String,
    ): YatraDetailDto
}
