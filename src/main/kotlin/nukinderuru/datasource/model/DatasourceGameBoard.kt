package nukinderuru.datasource.model

import kotlinx.serialization.Serializable

@Serializable
data class DatasourceGameBoard(
    val cells: List<List<Int>>
)
