package nukinderuru.service

import nukinderuru.domain.model.GameSymbol
import nukinderuru.domain.service.MinimaxGameService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class MinimaxGameServiceTest {
    private val service = MinimaxGameService()

    @Test
    fun `getBestMove should take winning move for computer`() {
        val nextMove = service.getBestMove(
            listOf(
                listOf(2, 2, 0),
                listOf(1, 1, 0),
                listOf(0, 0, 0)
            )
        )

        assertEquals(0 to 2, nextMove)
    }

    @Test
    fun `getBestMove should block winning player move`() {
        val nextMove = service.getBestMove(
            listOf(
                listOf(1, 1, 0),
                listOf(2, 0, 0),
                listOf(0, 2, 0)
            )
        )

        assertEquals(0 to 2, nextMove)
    }

    @Test
    fun `getBestMove should reject invalid board structure`() {
        assertFailsWith<IllegalArgumentException> {
            service.getBestMove(
                listOf(
                    listOf(1, 0),
                    listOf(0, 2)
                )
            )
        }
    }

    @Test
    fun `findWinner should detect row winner`() {
        val winner = service.findWinner(
            listOf(
                listOf(2, 2, 2),
                listOf(1, 1, 0),
                listOf(0, 0, 0)
            )
        )

        assertEquals(GameSymbol.O, winner)
    }

    @Test
    fun `isFinished should detect draw`() {
        assertTrue(
            service.isFinished(
                listOf(
                    listOf(1, 2, 1),
                    listOf(2, 1, 2),
                    listOf(2, 1, 2)
                )
            )
        )
    }

    @Test
    fun `isBoardValid should reject malformed board`() {
        assertFalse(
            service.isBoardValid(
                listOf(
                    listOf(1, 0, 0),
                    listOf(0, 2),
                )
            )
        )
    }
}
