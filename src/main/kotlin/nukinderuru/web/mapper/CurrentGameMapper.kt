package nukinderuru.web.mapper

import nukinderuru.common.ValidationMessages
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameOpponentType
import nukinderuru.domain.model.GamePlayer
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.GameSymbol
import nukinderuru.web.model.CreateGameRequest
import nukinderuru.web.model.WebCurrentGame
import nukinderuru.web.model.WebGamePlayer
import nukinderuru.web.model.WebGameState
import nukinderuru.web.model.WebGameStateType
import nukinderuru.web.model.WebGameSymbol
import nukinderuru.web.model.WebOpponentType
import java.util.UUID

fun CurrentGame.toWebModel(): WebCurrentGame = WebCurrentGame(
    id = id.toString(),
    createdAt = createdAt.toString(),
    board = board.toWebModel(),
    state = state.toWebModel(),
    players = players.map { it.toWebModel() },
    computerSymbol = computerSymbol?.toWebModel()
)

fun CreateGameRequest.toDomainModel(): GameOpponentType = when (opponentType) {
    WebOpponentType.Computer -> GameOpponentType.Computer
    WebOpponentType.User -> GameOpponentType.User
}

private fun GameState.toWebModel(): WebGameState = when (this) {
    GameState.WaitingForPlayers -> WebGameState(type = WebGameStateType.Waiting)
    GameState.Draw -> WebGameState(type = WebGameStateType.Draw)
    is GameState.PlayerTurn -> WebGameState(type = WebGameStateType.Turn, playerId = playerId.toString())
    is GameState.PlayerWin -> WebGameState(type = WebGameStateType.Win, playerId = playerId.toString())
}

private fun WebGameState.toDomainModel(): GameState = when (type) {
    WebGameStateType.Waiting -> GameState.WaitingForPlayers
    WebGameStateType.Draw -> GameState.Draw
    WebGameStateType.Turn -> GameState.PlayerTurn(UUID.fromString(playerId ?: error(ValidationMessages.PLAYER_TURN_REQUIRES_PLAYER_ID)))
    WebGameStateType.Win -> GameState.PlayerWin(UUID.fromString(playerId ?: error(ValidationMessages.PLAYER_WIN_REQUIRES_PLAYER_ID)))
}

private fun GamePlayer.toWebModel(): WebGamePlayer = WebGamePlayer(
    userId = userId.toString(),
    symbol = symbol.toWebModel()
)

private fun WebGamePlayer.toDomainModel(): GamePlayer = GamePlayer(
    userId = UUID.fromString(userId),
    symbol = symbol.toDomainModel()
)

private fun GameSymbol.toWebModel(): WebGameSymbol = when (this) {
    GameSymbol.X -> WebGameSymbol.X
    GameSymbol.O -> WebGameSymbol.O
}

private fun WebGameSymbol.toDomainModel(): GameSymbol = when (this) {
    WebGameSymbol.X -> GameSymbol.X
    WebGameSymbol.O -> GameSymbol.O
}
