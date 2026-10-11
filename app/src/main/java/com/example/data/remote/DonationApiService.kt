package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RemoteDonor(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String?,
    @Json(name = "monthly_pledge") val monthlyPledge: Double
)

@JsonClass(generateAdapter = true)
data class RemoteDonorCreate(
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String?,
    @Json(name = "monthly_pledge") val monthlyPledge: Double
)

@JsonClass(generateAdapter = true)
data class RemoteStatusUpdate(
    @Json(name = "donor_id") val donorId: String,
    @Json(name = "fiscal_year") val fiscalYear: Int,
    @Json(name = "month_index") val monthIndex: Int,
    val status: String
)

@JsonClass(generateAdapter = true)
data class RemoteStatusResponse(
    val success: Boolean,
    @Json(name = "donor_id") val donorId: String,
    @Json(name = "status_updated_to") val statusUpdatedTo: String,
    @Json(name = "total_collected_from_donor") val totalCollectedFromDonor: Double
)

@JsonClass(generateAdapter = true)
data class RemoteDashboardSummary(
    val fiscalYear: Int,
    val totalDonors: Int,
    val fixedMonthlyTarget: Double,
    val yearlyTarget: Double,
    val totalCollectedTillNow: Double,
    val outstandingVs12MonthTarget: Double,
    val collectionRate: Double,
    val totalPaidEntriesCount: Int,
    val totalNotPaidEntriesCount: Int
)

interface DonationApiService {
    @GET("api/v1/donors")
    suspend fun listDonors(): List<RemoteDonor>

    @GET("api/v1/donors/search")
    suspend fun searchDonors(@Query("q") query: String): List<RemoteDonor>

    @POST("api/v1/donors")
    suspend fun createDonor(@Body body: RemoteDonorCreate): RemoteDonor

    @PATCH("api/v1/ledger/status")
    suspend fun updatePaymentStatus(@Body body: RemoteStatusUpdate): RemoteStatusResponse

    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(
        @Query("fiscal_year") fiscalYear: Int
    ): RemoteDashboardSummary
}

object ApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun createService(baseUrl: String): DonationApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DonationApiService::class.java)
    }
}
