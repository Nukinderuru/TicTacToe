package nukinderuru.datasource.mapper

import nukinderuru.common.ValidationMessages
import nukinderuru.datasource.model.DatasourceCurrentGame
import nukinderuru.datasource.model.DatasourceGamePlayer
import nukinderuru.datasource.model.DatasourceGameState
import nukinderuru.datasource.model.DatasourceGameStateType
import nukinderuru.datasource.model.DatasourceGameSymbol
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GamePlayer
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.GameSymbol

fun CurrentGame.toDatasourceModel(): DatasourceCurrentGame = DatasourceCurrentGame(
    id = id,
    createdAt = createdAt,
    board = board.toDatasourceModel(),
    state = state.toDatasourceModel(),
    players = players.map { it.toDatasourceModel() },
    computerSymbol = computerSymbol?.toDatasourceModel()
)

fun DatasourceCurrentGame.toDomainModel(): CurrentGame = CurrentGame(
    id = id,
    createdAt = createdAt,
    board = board.toDomainModel(),
    state = state.toDomainModel(),
    players = players.map { it.toDomainModel() },
    computerSymbol = computerSymbol?.toDomainModel()
)

private fun GameState.toDatasourceModel(): DatasourceGameState = when (this) {
    GameState.WaitingForPlayers -> DatasourceGameState(type = DatasourceGameStateType.Waiting)
    GameState.Draw -> DatasourceGameState(type = DatasourceGameStateType.Draw)
    is GameState.PlayerTurn -> DatasourceGameState(type = DatasourceGameStateType.Turn, playerId = playerId)
    is GameState.PlayerWin -> DatasourceGameState(type = DatasourceGameStateType.Win, playerId = playerId)
}

private fun DatasourceGameState.toDomainModel(): GameState = when (type) {
    DatasourceGameStateType.Waiting -> GameState.WaitingForPlayers
    DatasourceGameStateType.Draw -> GameState.Draw
    DatasourceGameStateType.Turn -> GameState.PlayerTurn(playerId ?: error(ValidationMessages.PLAYER_TURN_REQUIRES_PLAYER_ID))
    DatasourceGameStateType.Win -> GameState.PlayerWin(playerId ?: error(ValidationMessages.PLAYER_WIN_REQUIRES_PLAYER_ID))
}

private fun GamePlayer.toDatasourceModel(): DatasourceGamePlayer = DatasourceGamePlayer(
    userId = userId,
    symbol = symbol.toDatasourceModel()
)

private fun DatasourceGamePlayer.toDomainModel(): GamePlayer = GamePlayer(
    userId = userId,
    symbol = symbol.toDomainModel()
)

private fun GameSymbol.toDatasourceModel(): DatasourceGameSymbol = when (this) {
    GameSymbol.X -> DatasourceGameSymbol.X
    GameSymbol.O -> DatasourceGameSymbol.O
}

private fun DatasourceGameSymbol.toDomainModel(): GameSymbol = when (this) {
    DatasourceGameSymbol.X -> GameSymbol.X
    DatasourceGameSymbol.O -> GameSymbol.O
}
