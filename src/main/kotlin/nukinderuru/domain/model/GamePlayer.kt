package nukinderuru.domain.model

import java.util.UUID

data class GamePlayer(
    val userId: UUID,
    val symbol: GameSymbol
)
