package nukinderuru.datasource.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DatasourceGamePlayer(
    @Serializable(with = UuidAsStringSerializer::class)
    val userId: UUID,
    val symbol: DatasourceGameSymbol
)
