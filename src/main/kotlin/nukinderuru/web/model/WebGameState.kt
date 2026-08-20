package nukinderuru.web.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebGameState(
    val type: WebGameStateType,
    val playerId: String? = null
)

@Serializable
enum class WebGameStateType {
    @SerialName("waiting")
    Waiting,

    @SerialName("turn")
    Turn,

    @SerialName("draw")
    Draw,

    @SerialName("win")
    Win
}
