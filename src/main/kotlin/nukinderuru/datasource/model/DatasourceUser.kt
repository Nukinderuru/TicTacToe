package nukinderuru.datasource.model

import java.util.UUID

data class DatasourceUser(
    val id: UUID,
    val login: String,
    val password: String
)
