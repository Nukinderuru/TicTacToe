package nukinderuru.web.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WebGameSymbol {
    @SerialName("X")
    X,
    @SerialName("O")
    O
}
