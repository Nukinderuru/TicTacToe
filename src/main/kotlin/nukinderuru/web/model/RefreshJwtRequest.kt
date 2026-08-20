package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class RefreshJwtRequest(
    val refreshToken: String
)
