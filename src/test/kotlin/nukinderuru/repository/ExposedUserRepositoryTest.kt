package nukinderuru.repository

import nukinderuru.datasource.repository.ExposedUserRepository
import nukinderuru.datasource.table.CurrentGamesTable
import nukinderuru.datasource.table.UsersTable
import nukinderuru.domain.model.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposedUserRepositoryTest {
    private val repository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )

        transaction {
            SchemaUtils.drop(CurrentGamesTable, UsersTable)
            SchemaUtils.create(CurrentGamesTable, UsersTable)
        }
    }

    @Test
    fun `saveUser should persist user`() {
        val user = User(
            id = UUID.randomUUID(),
            login = "alice",
            password = "secret"
        )

        val isSaved = repository.saveUser(user)

        assertTrue(isSaved)
        assertEquals(user, repository.fetchUserByLogin("alice"))
        assertEquals(user, repository.fetchUserById(user.id))
    }

    @Test
    fun `saveUser should reject duplicate login`() {
        repository.saveUser(
            User(
                id = UUID.randomUUID(),
                login = "alice",
                password = "secret"
            )
        )

        val isSaved = repository.saveUser(
            User(
                id = UUID.randomUUID(),
                login = "alice",
                password = "another-secret"
            )
        )

        assertFalse(isSaved)
        transaction {
            assertEquals(1, UsersTable.selectAll().count())
        }
    }

    @Test
    fun `fetchUser methods should return null for missing user`() {
        assertNull(repository.fetchUserByLogin("missing"))
        assertNull(repository.fetchUserById(UUID.randomUUID()))
    }
}
