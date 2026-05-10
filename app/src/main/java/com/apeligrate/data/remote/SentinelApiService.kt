package com.apeligrate.data.remote

import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body

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
)

/**
 * Response for create user — ORDS normally returns the created row or a success envelope.
 * We accept a generic map to be flexible.
 */
typealias CreateUserResponse = Map<String, Any>

/**
 * Placeholder for Retrofit or Ktor API Service
 */
interface SentinelApiService {
    suspend fun login(credentials: AuthCredentials): User
    suspend fun getAlerts(): List<Alert>

    @GET("admin/users/")
    suspend fun queryUsers(@Query("q") q: String): OrdsUsersResponse

    @POST("admin/users/")
    suspend fun createUser(@Body body: CreateUserRequest): CreateUserResponse
}
