package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class WebGamePlayer(
    val userId: String,
    val symbol: WebGameSymbol
)
