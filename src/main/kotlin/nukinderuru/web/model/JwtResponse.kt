package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class JwtResponse(
    val type: String,
    val accessToken: String,
    val refreshToken: String
)
