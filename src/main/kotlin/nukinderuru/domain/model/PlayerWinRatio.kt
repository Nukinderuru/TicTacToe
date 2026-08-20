package nukinderuru.domain.model

import java.util.UUID

data class PlayerWinRatio(
    val userId: UUID,
    val winRatio: Double
)
