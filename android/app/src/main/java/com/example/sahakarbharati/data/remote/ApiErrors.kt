package com.example.sahakarbharati.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

class ApiException(message: String) : Exception(message)

fun mapApiError(error: Throwable, fallback: String): String {
    return when (error) {
        is ApiException -> error.message ?: fallback
        is HttpException -> parseHttpError(error, fallback)
        is IOException -> "The server could not be reached. Check your connection."
        else -> fallback
    }
}

private fun parseHttpError(error: HttpException, fallback: String): String {
    val raw = error.response()?.errorBody()?.string().orEmpty()
    if (raw.isBlank()) return fallback
    return runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        val detail = root.get("detail") ?: return fallback
        when {
            detail.isJsonPrimitive -> detail.asString
            detail.isJsonArray && detail.asJsonArray.size() > 0 -> {
                val first = detail.asJsonArray.first().asJsonObject
                // Pydantic prefixes custom validator messages with "Value error, ".
                first.get("msg")?.asString?.removePrefix("Value error, ") ?: fallback
            }
            else -> fallback
        }
    }.getOrDefault(fallback)
}
