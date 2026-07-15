package studio.sandlight.jvm

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Smoke tests with deliberately small parameters: each demo must complete
 * quickly and without throwing. The demos print their lessons; here we only
 * guard that they keep running (e.g. after refactors).
 */
class DemoSmokeTest {

    @Test
    fun `alloc demo runs for one second`() = assertDoesNotThrow { allocDemo(1) }

    @Test
    fun `classloader demo runs`() = assertDoesNotThrow { classLoaderDemo() }

    @Test
    fun `stack demo completes at shallow depth`() = assertDoesNotThrow { stackDemo(500) }

    @Test
    fun `stack demo catches StackOverflowError at extreme depth`() =
        // The demo must catch the SOE internally and not propagate it.
        assertDoesNotThrow { stackDemo(10_000_000) }

    @Test
    fun `string intern demo runs`() = assertDoesNotThrow { stringInternDemo() }

    @Test
    fun `escape analysis demo runs with few iterations`() =
        assertDoesNotThrow { escapeAnalysisDemo(10_000) }

    @Test
    fun `daemon thread demo runs`() = assertDoesNotThrow { daemonThreadDemo() }

    @Test
    fun `volatile demo terminates even if the flag is never observed`() =
        // Daemon worker threads guarantee the demo returns after its joins.
        assertDoesNotThrow { volatileVisibilityDemo(runMillis = 200) }
}
