package com.example.sahakarbharati.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

    @POST("api/v1/yatras/{yatraId}/attendance")
    suspend fun scanAttendance(
        @Header("Authorization") authorization: String,
        @Path("yatraId") yatraId: String,
        @Body body: YatraAttendanceScanDto,
    ): YatraAttendanceRecordDto

    @POST("api/v1/yatras/{yatraId}/positions")
    suspend fun postPosition(
        @Header("Authorization") authorization: String,
        @Path("yatraId") yatraId: String,
        @Body body: YatraPositionCreateDto,
    ): Response<Unit>
}
