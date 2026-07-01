package com.apeligrate.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Estructura exacta según documentación técnica de ORS V2
data class ORSRequest(
    @SerializedName("coordinates") val coordinates: List<List<Double>>,
    @SerializedName("options") val options: ORSOptions? = null,
    @SerializedName("preference") val preference: String = "fastest"
)

data class ORSOptions(
    @SerializedName("avoid_polygons") val avoidPolygons: ORSAvoidPolygons? = null
)

data class ORSAvoidPolygons(
    @SerializedName("type") val type: String = "MultiPolygon",
    @SerializedName("coordinates") val coordinates: List<List<List<List<Double>>>>
)

data class ORSResponse(
    @SerializedName("routes") val routes: List<ORSRoute>
)

data class ORSRoute(
    @SerializedName("geometry") val geometry: String,
    @SerializedName("summary") val summary: ORSSummary
)

data class ORSSummary(
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double
)

interface RouteService {
    @POST("v2/directions/{profile}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Header("Authorization") apiKey: String,
        @Body request: ORSRequest
    ): ORSResponse
}
