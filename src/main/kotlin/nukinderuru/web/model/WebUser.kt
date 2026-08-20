package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class WebUser(
    val id: String,
    val login: String
)
