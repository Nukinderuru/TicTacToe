package nukinderuru.domain.model

import java.time.Instant
import java.util.UUID

data class CurrentGame(
    val id: UUID,
    val createdAt: Instant = Instant.now(),
    val board: GameBoard,
    val state: GameState,
    val players: List<GamePlayer>,
    val computerSymbol: GameSymbol? = null
)
