package nukinderuru.domain.service

import nukinderuru.common.ValidationMessages
import nukinderuru.datasource.repository.CurrentGameRepository
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameBoard
import nukinderuru.domain.model.GameOpponentType
import nukinderuru.domain.model.GamePlayer
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.GameSymbol
import nukinderuru.domain.model.PlayerWinRatio
import java.util.UUID

class CurrentGameService(
    private val repository: CurrentGameRepository,
) : GameService {
    private val minimaxGameService = MinimaxGameService()

    override fun createNewGame(creatorId: UUID, opponentType: GameOpponentType): CurrentGame {
        val newGame = CurrentGame(
            id = UUID.randomUUID(),
            board = GameBoard(List(BOARD_SIZE) { List(BOARD_SIZE) { EMPTY } }),
            state = when (opponentType) {
                GameOpponentType.Computer -> GameState.PlayerTurn(creatorId)
                GameOpponentType.User -> GameState.WaitingForPlayers
            },
            players = listOf(GamePlayer(creatorId, GameSymbol.X)),
            computerSymbol = opponentType.takeIf { it == GameOpponentType.Computer }?.let { GameSymbol.O },
        )

        repository.saveCurrentGame(newGame)
        return newGame
    }

    override fun getAvailableGames(userId: UUID): List<CurrentGame> = repository.fetchCurrentGames().filter { game ->
        game.state is GameState.WaitingForPlayers &&
            game.computerSymbol == null &&
            game.players.none { player -> player.userId == userId }
    }

    override fun joinGame(gameId: UUID, userId: UUID): CurrentGame {
        val game = repository.fetchCurrentGame(gameId) ?: throw IllegalArgumentException(ValidationMessages.GAME_NOT_FOUND)
        validateJoinRequest(game, userId)

        val updatedGame = game.copy(
            players = game.players + GamePlayer(userId, GameSymbol.O),
            state = GameState.PlayerTurn(game.players.single().userId),
        )

        repository.saveCurrentGame(updatedGame)
        return updatedGame
    }

    override fun makeMove(gameId: UUID, userId: UUID, rowIndex: Int, columnIndex: Int): CurrentGame {
        val currentGame = repository.fetchCurrentGame(gameId) ?: throw IllegalArgumentException(ValidationMessages.GAME_NOT_FOUND)
        val player = validateMoveRequest(currentGame, userId, rowIndex, columnIndex)

        val boardAfterPlayerMove = currentGame.board.cells.withMove(rowIndex, columnIndex, player.symbol.cellValue)
        val gameAfterPlayerMove = currentGame.copy(board = GameBoard(boardAfterPlayerMove))
        val playerResult = resolveTerminalState(gameAfterPlayerMove)
        if (playerResult != null) {
            val finishedGame = gameAfterPlayerMove.copy(state = playerResult)
            repository.saveCurrentGame(finishedGame)
            return finishedGame
        }

        if (currentGame.computerSymbol != null) {
            val computerMove = minimaxGameService.getBestMove(boardAfterPlayerMove)
            if (computerMove == null) {
                val drawGame = gameAfterPlayerMove.copy(state = GameState.Draw)
                repository.saveCurrentGame(drawGame)
                return drawGame
            }

            val boardAfterComputerMove = boardAfterPlayerMove.withMove(
                computerMove.first,
                computerMove.second,
                currentGame.computerSymbol.cellValue,
            )
            val gameAfterComputerMove = currentGame.copy(board = GameBoard(boardAfterComputerMove))
            val computerResult = resolveTerminalState(gameAfterComputerMove) ?: GameState.PlayerTurn(userId)
            val updatedGame = gameAfterComputerMove.copy(state = computerResult)
            repository.saveCurrentGame(updatedGame)
            return updatedGame
        }

        val nextPlayerId = currentGame.players.first { it.userId != userId }.userId
        val updatedGame = gameAfterPlayerMove.copy(state = GameState.PlayerTurn(nextPlayerId))
        repository.saveCurrentGame(updatedGame)
        return updatedGame
    }

    override fun getCurrentGame(gameId: UUID): CurrentGame? = repository.fetchCurrentGame(gameId)

    override fun getCompletedGames(userId: UUID): List<CurrentGame> = repository.fetchCompletedGamesByUserId(userId)

    override fun getTopPlayers(limit: Int): List<PlayerWinRatio> {
        require(limit > 0) { ValidationMessages.LEADERBOARD_LIMIT_MUST_BE_POSITIVE }
        return repository.fetchTopPlayers(limit)
    }

    private fun resolveTerminalState(currentGame: CurrentGame): GameState? {
        val winnerSymbol = minimaxGameService.findWinner(currentGame.board.cells)
        if (winnerSymbol != null) {
            val winnerId = currentGame.players.firstOrNull { it.symbol == winnerSymbol }?.userId
                ?: if (currentGame.computerSymbol == winnerSymbol) COMPUTER_PLAYER_ID else null
            if (winnerId == null) throw IllegalStateException(ValidationMessages.WINNER_SYMBOL_NOT_MAPPED)

            return GameState.PlayerWin(winnerId)
        }

        if (minimaxGameService.isFinished(currentGame.board.cells)) {
            return GameState.Draw
        }

        return null
    }

    private fun validateMoveRequest(currentGame: CurrentGame, userId: UUID, rowIndex: Int, columnIndex: Int): GamePlayer {
        val currentTurn = currentGame.state as? GameState.PlayerTurn
            ?: throw IllegalArgumentException(ValidationMessages.GAME_IS_NOT_READY_FOR_MOVE)
        require(currentTurn.playerId == userId) { ValidationMessages.NOT_USERS_TURN }

        val player = currentGame.players.firstOrNull { it.userId == userId }
            ?: throw IllegalArgumentException(ValidationMessages.USER_IS_NOT_GAME_PARTICIPANT)

        require(minimaxGameService.isBoardValid(currentGame.board.cells)) { ValidationMessages.INVALID_BOARD_STRUCTURE }
        require(rowIndex in 0 until BOARD_SIZE && columnIndex in 0 until BOARD_SIZE) { ValidationMessages.MOVE_IS_OUT_OF_BOUNDS }
        require(currentGame.board.cells[rowIndex][columnIndex] == EMPTY) { ValidationMessages.CELL_IS_ALREADY_OCCUPIED }

        return player
    }

    private fun validateJoinRequest(game: CurrentGame, userId: UUID) {
        require(game.computerSymbol == null) { ValidationMessages.COMPUTER_GAME_CANNOT_BE_JOINED }
        require(game.state is GameState.WaitingForPlayers) { ValidationMessages.GAME_IS_NOT_WAITING_FOR_PLAYERS }
        require(game.players.none { it.userId == userId }) { ValidationMessages.USER_ALREADY_IN_GAME }
        require(game.players.size == 1) { ValidationMessages.GAME_ALREADY_HAS_TWO_PLAYERS }
    }

    private fun List<List<Int>>.withMove(rowIndex: Int, columnIndex: Int, value: Int): List<List<Int>> =
        mapIndexed { currentRowIndex, row ->
            if (currentRowIndex != rowIndex) {
                row
            } else {
                row.mapIndexed { currentColumnIndex, cell ->
                    if (currentColumnIndex == columnIndex) value else cell
                }
            }
        }

    private companion object {
        const val BOARD_SIZE = 3
        const val EMPTY = 0
        val COMPUTER_PLAYER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
