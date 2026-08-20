package nukinderuru.datasource.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object CurrentGamesTable : Table("current_games") {
    val id = uuid("id")
    val createdAt = timestamp("created_at")
    val stateType = varchar("state_type", 16)
    val winnerId = uuid("winner_id").nullable()
    val firstPlayerId = uuid("first_player_id").nullable()
    val secondPlayerId = uuid("second_player_id").nullable()
    val board = text("board")

    override val primaryKey = PrimaryKey(id)
}
