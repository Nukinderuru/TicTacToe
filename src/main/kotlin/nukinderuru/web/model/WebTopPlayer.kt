package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class WebTopPlayer(
    val userId: String,
    val login: String,
    val winRatio: Double
)
