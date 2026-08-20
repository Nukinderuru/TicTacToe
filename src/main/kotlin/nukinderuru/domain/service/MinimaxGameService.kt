package nukinderuru.domain.service

import nukinderuru.common.ValidationMessages
import nukinderuru.domain.model.GameSymbol

class MinimaxGameService {
    fun getBestMove(board: List<List<Int>>): Pair<Int, Int>? {
        require(isBoardStructureValid(board)) { ValidationMessages.INVALID_BOARD_STRUCTURE }

        if (isGameFinished(board)) {
            return null
        }

        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null

        for ((rowIndex, row) in board.withIndex()) {
            for ((columnIndex, cell) in row.withIndex()) {
                if (cell != EMPTY) {
                    continue
                }

                val nextBoard = board.withMove(rowIndex, columnIndex, COMPUTER)
                val score = minimax(nextBoard, isComputerTurn = false)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = rowIndex to columnIndex
                }
            }
        }

        return bestMove
    }

    fun findWinner(board: List<List<Int>>): GameSymbol? = when (winner(board)) {
        PLAYER -> GameSymbol.X
        COMPUTER -> GameSymbol.O
        else -> null
    }

    fun isFinished(board: List<List<Int>>): Boolean = isGameFinished(board)

    fun isBoardValid(board: List<List<Int>>): Boolean = isBoardStructureValid(board)

    private fun minimax(board: List<List<Int>>, isComputerTurn: Boolean): Int {
        winner(board)?.let {
            return when (it) {
                COMPUTER -> 1
                PLAYER -> -1
                else -> 0
            }
        }

        if (board.all { row -> row.none { it == EMPTY } }) {
            return 0
        }

        return if (isComputerTurn) {
            var bestScore = Int.MIN_VALUE
            for ((rowIndex, row) in board.withIndex()) {
                for ((columnIndex, cell) in row.withIndex()) {
                    if (cell != EMPTY) {
                        continue
                    }

                    val score = minimax(board.withMove(rowIndex, columnIndex, COMPUTER), isComputerTurn = false)
                    bestScore = maxOf(bestScore, score)
                }
            }
            bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for ((rowIndex, row) in board.withIndex()) {
                for ((columnIndex, cell) in row.withIndex()) {
                    if (cell != EMPTY) {
                        continue
                    }

                    val score = minimax(board.withMove(rowIndex, columnIndex, PLAYER), isComputerTurn = true)
                    bestScore = minOf(bestScore, score)
                }
            }
            bestScore
        }
    }

    private fun isGameFinished(board: List<List<Int>>): Boolean = winner(board) != null || board.all { row -> row.none { it == EMPTY } }

    private fun winner(board: List<List<Int>>): Int? {
        val lines = buildList {
            addAll(board)
            addAll(board.indices.map { columnIndex -> board.map { row -> row[columnIndex] } })
            add(board.indices.map { index -> board[index][index] })
            add(board.indices.map { index -> board[index][board.lastIndex - index] })
        }

        return lines.firstNotNullOfOrNull { line ->
            line.firstOrNull()?.takeIf { cell -> cell != EMPTY && line.all { it == cell } }
        }
    }

    private fun isBoardStructureValid(board: List<List<Int>>): Boolean =
        board.size == BOARD_SIZE && board.all { row -> row.size == BOARD_SIZE && row.all { it in EMPTY..COMPUTER } }

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
        const val PLAYER = 1
        const val COMPUTER = 2
    }
}
