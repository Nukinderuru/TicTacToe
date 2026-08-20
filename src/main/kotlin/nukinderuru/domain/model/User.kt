package nukinderuru.domain.model

import java.util.UUID

data class User(
    val id: UUID,
    val login: String,
    val password: String
)
