package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class WebMoveRequest(
    val rowIndex: Int,
    val columnIndex: Int
)
