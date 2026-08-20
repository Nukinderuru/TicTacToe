package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGameRequest(
    val opponentType: WebOpponentType
)
