package nukinderuru.web.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WebOpponentType {
    @SerialName("computer")
    Computer,
    @SerialName("user")
    User
}
