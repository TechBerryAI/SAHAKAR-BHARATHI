package com.example.sahakarbharati.data.remote

import com.google.gson.annotations.SerializedName

data class YatraDistrictDto(
    val id: String,
    val name: String,
    val code: String,
)

data class YatraStopDto(
    val id: String,
    val sequence: Int,
    val name: String,
    val address: String?,
    val active: Boolean,
)

data class YatraSummaryDto(
    val id: String,
    @SerializedName("public_code") val publicCode: String,
    val name: String,
    val district: YatraDistrictDto,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    val status: String,
    val published: Boolean,
)

data class YatraDetailDto(
    val id: String,
    @SerializedName("public_code") val publicCode: String,
    val name: String,
    val description: String?,
    val district: YatraDistrictDto,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    val status: String,
    val published: Boolean,
    val stops: List<YatraStopDto>,
)

data class YatraPageDto(
    val items: List<YatraSummaryDto>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)
