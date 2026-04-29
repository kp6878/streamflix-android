package com.example.streamfilx_androidtv.core.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiError(message: String) : Exception(message) {
    class NetworkError(cause: Throwable) : ApiError("Network error: ${cause.message}")
    class HttpError(val code: Int, url: String) : ApiError("HTTP $code for $url")
    class EmptyBody(url: String) : ApiError("Empty response body for $url")
    class DecodingError(cause: Throwable) : ApiError("JSON decode error: ${cause.message}")
    class NotFound(url: String) : ApiError("Not found: $url")
    class RateLimited(url: String) : ApiError("Rate limited: $url")
}

@Singleton
class ApiClient @Inject constructor(private val okHttpClient: OkHttpClient) {

    private val gson = Gson()

    suspend fun <T> get(url: String, type: Type): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> throw ApiError.NotFound(url)
                    response.code == 429 -> throw ApiError.RateLimited(url)
                    !response.isSuccessful -> throw ApiError.HttpError(response.code, url)
                    else -> {
                        val body = response.body?.string()
                            ?: throw ApiError.EmptyBody(url)
                        try {
                            gson.fromJson<T>(body, type)
                        } catch (e: Exception) {
                            throw ApiError.DecodingError(e)
                        }
                    }
                }
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: IOException) {
            throw ApiError.NetworkError(e)
        }
    }

    suspend fun <T> get(url: String, clazz: Class<T>): T = get(url, clazz as Type)
}

// ── Retry interceptor (wired in NetworkModule) ────────────────────────────────

class RetryInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        var response: okhttp3.Response? = null
        var tries = 0

        while (tries < MAX_RETRIES) {
            try {
                response?.close()
                response = chain.proceed(request)
                if (response.code !in RETRY_CODES) return response
            } catch (e: IOException) {
                if (tries == MAX_RETRIES - 1) throw e
            }
            tries++
            Thread.sleep(BACKOFF_MS * (1L shl (tries - 1)))
        }
        return response ?: chain.proceed(request)
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val BACKOFF_MS = 1000L
        private val RETRY_CODES = setOf(429, 502, 503, 504)
    }
}
