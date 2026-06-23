package com.apeligrate.data.remote

import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * ORDS response for listing users: contains an `items` array
 */
data class OrdsUsersResponse(
    val items: List<Map<String, Any>> = emptyList()
)

data class CreateUserRequest(
    val name: String,
    val email: String,
    val password_hash: String,
    val profile_image_url: String? = null,
    val city: String? = null,
    val is_verified: Boolean = false,
    val experience: Int = 0,
    val level: Int = 1,
    val next_level_experience: Int = 100,
    val reputation_title: String = "Iniciante",
    val reports_count: Int = 0,
    val validations_count: Int = 0,
)

data class CreateIncidentReportRequest(
    val category: String,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val is_anonymous: Boolean,
    val reported_at: Long,
    val user_id: Long? = null,
    val status: String,
    val images: List<String> = emptyList(),
    val validation_count: Int = 0,
    val false_count: Int = 0,
    val persistence_message: String = ""
)

data class CreateReportVoteRequest(
    val report_id: String,
    val user_id: Long,
    val vote_type: String
)

data class UpdateIncidentReportRequest(
    val status: String? = null,
    val validation_count: Int? = null,
    val false_count: Int? = null
)

data class UpdateUserRequest(
    val name: String? = null,
    val city: String? = null,
    val is_verified: Boolean? = null,
    val experience: Int? = null,
    val level: Int? = null,
    val next_level_experience: Int? = null,
    val reputation_title: String? = null,
    val reports_count: Int? = null,
    val validations_count: Int? = null
)

/**
 * Response for create user - ORDS normally returns the created row or a success envelope.
 * We accept a generic map to be flexible.
 */
typealias CreateUserResponse = Map<String, Any>

/**
 * Placeholder for Retrofit or Ktor API Service
 */
interface SentinelApiService {
    suspend fun login(credentials: AuthCredentials): User
    suspend fun getAlerts(): List<Alert>

    @GET("users")
    suspend fun queryUsers(
        @Query("select") select: String = "*",
        @Query("email") email: String,
        @Query("password_hash") passwordHash: String,
    ): List<Map<String, Any>>

    @Headers("Prefer: return=representation")
    @POST("users")
    suspend fun createUser(@Body body: CreateUserRequest): List<Map<String, Any>>

    @GET("users")
    suspend fun getUsers(
        @Query("select") select: String = "*",
        @Query("id") id: String? = null
    ): List<Map<String, Any?>>

    @Headers("Prefer: return=representation")
    @PATCH("users")
    suspend fun updateUser(
        @Query("id") id: String,
        @Body body: UpdateUserRequest
    ): List<Map<String, Any?>>

    @GET("incident_reports")
    suspend fun getIncidentReports(
        @Query("select") select: String = "*",
        @Query("status") status: String? = "neq.cancelled",
        @Query("order") order: String = "reported_at.desc",
        @Query("user_id") userId: String? = null
    ): List<Map<String, Any?>>

    @Headers("Prefer: return=representation")
    @POST("incident_reports")
    suspend fun createIncidentReport(
        @Body body: CreateIncidentReportRequest
    ): List<Map<String, Any?>>

    @Headers("Prefer: return=representation")
    @PATCH("incident_reports")
    suspend fun updateIncidentReport(
        @Query("id") id: String,
        @Body body: UpdateIncidentReportRequest
    ): List<Map<String, Any?>>

    @GET("report_votes")
    suspend fun getReportVotes(
        @Query("select") select: String = "*",
        @Query("report_id") reportId: String? = null,
        @Query("user_id") userId: String? = null
    ): List<Map<String, Any?>>

    @Headers("Prefer: return=representation")
    @POST("report_votes")
    suspend fun createReportVote(
        @Body body: CreateReportVoteRequest
    ): List<Map<String, Any?>>
}
