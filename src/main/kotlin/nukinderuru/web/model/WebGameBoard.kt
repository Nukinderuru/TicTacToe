package nukinderuru.web.model

import kotlinx.serialization.Serializable

@Serializable
data class WebGameBoard(
    val cells: List<List<Int>>
)
