package nukinderuru.datasource.repository

import nukinderuru.datasource.mapper.toDatasourceModel
import nukinderuru.datasource.mapper.toDomainModel
import nukinderuru.datasource.model.DatasourceUser
import nukinderuru.datasource.table.UsersTable
import nukinderuru.domain.model.User
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedUserRepository : UserRepository {
    override fun saveUser(user: User): Boolean = transaction {
        val datasourceUser = user.toDatasourceModel()
        val existingUser = UsersTable
            .selectAll()
            .where { UsersTable.login eq datasourceUser.login }
            .singleOrNull()

        if (existingUser != null) {
            return@transaction false
        }

        UsersTable.insert {
            it[id] = datasourceUser.id
            it[login] = datasourceUser.login
            it[password] = datasourceUser.password
        }

        true
    }

    override fun fetchUserByLogin(login: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.login eq login }
            .singleOrNull()
            ?.toDomainUser()
    }

    override fun fetchUserById(userId: UUID): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toDomainUser()
    }

    private fun ResultRow.toDomainUser(): User = DatasourceUser(
        id = this[UsersTable.id],
        login = this[UsersTable.login],
        password = this[UsersTable.password],
    ).toDomainModel()
}
