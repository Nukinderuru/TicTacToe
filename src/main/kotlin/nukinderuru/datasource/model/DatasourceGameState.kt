package nukinderuru.datasource.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DatasourceGameState(
    val type: DatasourceGameStateType,
    @Serializable(with = NullableUuidAsStringSerializer::class)
    val playerId: UUID? = null
)

@Serializable
enum class DatasourceGameStateType {
    @SerialName("waiting")
    Waiting,

    @SerialName("turn")
    Turn,

    @SerialName("draw")
    Draw,

    @SerialName("win")
    Win
}
