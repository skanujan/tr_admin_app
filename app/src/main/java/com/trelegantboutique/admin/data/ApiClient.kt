package com.trelegantboutique.admin.data

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds (and rebuilds, if the server address ever changes) the Retrofit
 * client used for every API call. Kept as a small holder class rather than a
 * global singleton so SessionStore - the one source of truth for the token
 * and base URL - stays the only thing that has to be wired in from AdminApp.
 */
class ApiClient(private val session: SessionStore) {

    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cachedService: ApiService? = null

    val service: ApiService
        get() {
            val baseUrl = session.apiBaseUrl
            val existing = cachedService
            if (existing != null && cachedBaseUrl == baseUrl) return existing
            synchronized(this) {
                val again = cachedService
                if (again != null && cachedBaseUrl == baseUrl) return again
                val built = build(baseUrl)
                cachedBaseUrl = baseUrl
                cachedService = built
                return built
            }
        }

    private fun build(baseUrl: String): ApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = session.token
            val request = if (!token.isNullOrBlank()) {
                original.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else {
                original
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            // BASIC, not BODY: admin data (customer names/phones/addresses)
            // shouldn't be written to device logs in full.
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().create()

        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBase)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
