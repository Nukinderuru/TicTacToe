package nukinderuru.datasource.mapper

import nukinderuru.domain.model.GameBoard
import nukinderuru.datasource.model.DatasourceGameBoard

fun GameBoard.toDatasourceModel(): DatasourceGameBoard = DatasourceGameBoard(cells = cells.map { it.toList() })

fun DatasourceGameBoard.toDomainModel(): GameBoard = GameBoard(cells = cells.map { it.toList() })
