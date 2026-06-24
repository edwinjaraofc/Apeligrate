package com.apeligrate.data.remote

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

data class OSRMResponse(
    val code: String?,
    val routes: List<OSRMRoute>
)

data class OSRMRoute(
    val geometry: String,
    val duration: Double,
    val distance: Double
)

interface RouteService {
    @Headers("User-Agent: Apeligrate-Android-App")
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): OSRMResponse
}
