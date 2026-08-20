package nukinderuru.datasource.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class DatasourceCurrentGame(
    @Serializable(with = UuidAsStringSerializer::class)
    val id: UUID,
    @Serializable(with = InstantAsStringSerializer::class)
    val createdAt: Instant,
    val board: DatasourceGameBoard,
    val state: DatasourceGameState,
    val players: List<DatasourceGamePlayer>,
    val computerSymbol: DatasourceGameSymbol? = null
)
