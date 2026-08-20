package nukinderuru.domain.model

import java.util.UUID

sealed interface GameState {
    data object WaitingForPlayers : GameState

    data class PlayerTurn(
        val playerId: UUID
    ) : GameState

    data object Draw : GameState

    data class PlayerWin(
        val playerId: UUID
    ) : GameState
}
