package com.soilcarbon.engine.data.remote

import com.google.gson.JsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SoilGridsApi {
    @GET("soilgrids/v2.0/properties/query")
    suspend fun query(
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("property") property: List<String>,
        @Query("depth") depth: List<String>,
        @Query("value") value: String = "mean"
    ): JsonObject
}

interface OpenMeteoApi {
    @GET("v1/archive")
    suspend fun archive(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "temperature_2m_mean,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): JsonObject
}

interface SentinelStatisticsApi {
    @POST("api/v1/statistics")
    suspend fun statistics(
        @Header("Authorization") bearerToken: String,
        @Body request: JsonObject
    ): JsonObject
}

class RateLimiter(private val minimumIntervalMillis: Long) {
    private val mutex = Mutex()
    private var lastAcquiredAt = 0L

    suspend fun acquire() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val wait = minimumIntervalMillis - (now - lastAcquiredAt)
            if (wait > 0) kotlinx.coroutines.delay(wait)
            lastAcquiredAt = System.currentTimeMillis()
        }
    }
}

object ApiClientFactory {

    fun retrofit(baseUrl: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.soilcarbon.engine.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "SoilCarbonEngine/2.4.0 (offline-first field client)")
                        .build()
                )
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun soilGrids(baseUrl: String = "https://rest.isric.org/"): SoilGridsApi =
        retrofit(baseUrl).create(SoilGridsApi::class.java)

    fun openMeteo(baseUrl: String = "https://archive-api.open-meteo.com/"): OpenMeteoApi =
        retrofit(baseUrl).create(OpenMeteoApi::class.java)

    fun sentinel(baseUrl: String = "https://services.sentinel-hub.com/"): SentinelStatisticsApi =
        retrofit(baseUrl).create(SentinelStatisticsApi::class.java)
}
