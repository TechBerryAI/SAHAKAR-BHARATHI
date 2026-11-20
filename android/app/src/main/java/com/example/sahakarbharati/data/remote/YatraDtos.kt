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
    @SerializedName("has_more") val hasMore: Boolean = false,
)

data class YatraAttendanceScanDto(
    @SerializedName("qr_token") val qrToken: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("accuracy_m") val accuracyM: Double,
    @SerializedName("captured_at") val capturedAt: String,
)

data class YatraPositionBatchDto(
    val positions: List<YatraPositionCreateDto>,
)

data class YatraPositionBatchItemDto(
    @SerializedName("client_position_id") val clientPositionId: String,
    val status: String,
    val error: String? = null,
)

data class YatraPositionBatchResponseDto(
    val results: List<YatraPositionBatchItemDto>,
    val accepted: Int,
    val duplicates: Int,
    val rejected: Int,
)

data class YatraAttendanceRecordDto(
    val id: String,
    @SerializedName("yatra_id") val yatraId: String,
    @SerializedName("counter_id") val counterId: String,
    @SerializedName("counter_name") val counterName: String,
    @SerializedName("attendance_date") val attendanceDate: String,
    @SerializedName("checked_in_at") val checkedInAt: String,
    @SerializedName("already_recorded") val alreadyRecorded: Boolean,
)

data class YatraAttendancePageDto(
    val items: List<YatraAttendanceRecordDto>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    @SerializedName("has_more") val hasMore: Boolean = false,
)

data class YatraPositionCreateDto(
    @SerializedName("client_position_id") val clientPositionId: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("accuracy_m") val accuracyM: Double,
    @SerializedName("captured_at") val capturedAt: String,
)
