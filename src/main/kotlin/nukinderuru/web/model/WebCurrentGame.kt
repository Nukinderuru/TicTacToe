package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class WebCurrentGame(
    val id: String,
    val createdAt: String,
    val board: WebGameBoard,
    val state: WebGameState,
    val players: List<WebGamePlayer>,
    val computerSymbol: WebGameSymbol? = null
)
