package nukinderuru.web.mapper

import nukinderuru.domain.model.GameBoard
import nukinderuru.web.model.WebGameBoard

fun GameBoard.toWebModel(): WebGameBoard = WebGameBoard(cells = cells.map { it.toList() })

fun WebGameBoard.toDomainModel(): GameBoard = GameBoard(cells = cells.map { it.toList() })
