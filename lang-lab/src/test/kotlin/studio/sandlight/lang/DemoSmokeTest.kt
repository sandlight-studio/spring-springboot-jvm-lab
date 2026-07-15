package studio.sandlight.lang

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Smoke tests: every level of every topic must run without throwing.
 * Generated from the same `topics` registry Main.kt dispatches on, so a new
 * topic/level is covered automatically. Output is not asserted — these demos
 * teach via println; the contract is simply "runs cleanly".
 */
class DemoSmokeTest {

    @TestFactory
    fun `every topic level runs without throwing`(): List<DynamicTest> =
        topics.flatMap { topic ->
            topic.levels.map { level ->
                dynamicTest("${topic.name} L${level.number} ${level.key}") {
                    assertDoesNotThrow { level.run() }
                }
            }
        }

    @Test
    fun `topic names and level numbers are unique`() {
        assertTrue(topics.map { it.name }.toSet().size == topics.size)
        topics.forEach { topic ->
            val numbers = topic.levels.map { it.number }
            assertTrue(numbers.toSet().size == numbers.size) { "duplicate level in ${topic.name}" }
        }
    }
}
