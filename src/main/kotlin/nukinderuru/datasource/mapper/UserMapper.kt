package nukinderuru.datasource.mapper

import nukinderuru.datasource.model.DatasourceUser
import nukinderuru.domain.model.User

fun User.toDatasourceModel(): DatasourceUser = DatasourceUser(
    id = id,
    login = login,
    password = password
)

fun DatasourceUser.toDomainModel(): User = User(
    id = id,
    login = login,
    password = password
)
