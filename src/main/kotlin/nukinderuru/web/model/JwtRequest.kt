package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class JwtRequest(
    val login: String,
    val password: String
)
