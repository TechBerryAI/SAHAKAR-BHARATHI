package com.example.sahakarbharati.data

import android.content.Context
import com.example.sahakarbharati.BuildConfig
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.ProfileRepository
import com.example.sahakarbharati.data.repository.RemoteAuthRepository
import com.example.sahakarbharati.data.repository.RemoteProfileRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val session: AppSession = AppSession.from(context)

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: SahakarApi = retrofit.create(SahakarApi::class.java)
    val authRepository: AuthRepository = RemoteAuthRepository(api, session)
    val profileRepository: ProfileRepository = RemoteProfileRepository(api, session)
}
