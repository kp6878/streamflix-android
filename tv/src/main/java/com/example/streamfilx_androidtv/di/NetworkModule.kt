package com.example.streamfilx_androidtv.di

import com.example.streamfilx_androidtv.core.network.ApiClient
import com.example.streamfilx_androidtv.core.network.ResponseCache
import com.example.streamfilx_androidtv.core.network.RetryInterceptor
import com.example.streamfilx_androidtv.core.network.StremioClient
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import com.example.streamfilx_androidtv.services.RealDebridApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ── Shared interceptors ───────────────────────────────────────────────────

    @Provides @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    // ── Stremio OkHttpClient (shared / default) ───────────────────────────────

    @Provides @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(retryInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideApiClient(okHttpClient: OkHttpClient): ApiClient = ApiClient(okHttpClient)

    @Provides @Singleton
    fun provideStremioClient(
        apiClient: ApiClient,
        responseCache: ResponseCache,
    ): StremioClient = StremioClient(apiClient, responseCache)

    // ── Real-Debrid OkHttpClient (with Bearer auth interceptor) ──────────────

    @Provides @Singleton @Named("rdClient")
    fun provideRealDebridOkHttpClient(
        appPreferences: AppPreferences,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val apiKey = runBlocking { appPreferences.realDebridApiKey.first() }
            val request = chain.request().newBuilder()
                .apply { if (!apiKey.isNullOrBlank()) addHeader("Authorization", "Bearer $apiKey") }
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRealDebridApi(@Named("rdClient") client: OkHttpClient): RealDebridApi =
        Retrofit.Builder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RealDebridApi::class.java)
}
