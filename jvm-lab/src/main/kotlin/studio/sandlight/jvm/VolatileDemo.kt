package studio.sandlight.jvm

@Volatile private var keepRunningVolatile = true
private var keepRunningNonVolatile = true

fun volatileVisibilityDemo(runMillis: Long = 2000) {
    println("Demonstrating visibility with and without @Volatile ($runMillis ms).")

    // The loop bodies must stay barrier-free (plain local counter only):
    // any synchronized/atomic call inside would insert memory barriers that
    // can make even the non-volatile flag visible and hide the bug.
    val t1 = Thread {
        var iterations = 0L
        while (keepRunningNonVolatile) {
            iterations++
        }
        println("[non-volatile] observed stop flag after $iterations iterations")
    }

    val t2 = Thread {
        var iterations = 0L
        while (keepRunningVolatile) {
            iterations++
        }
        println("[volatile] observed stop flag after $iterations iterations")
    }

    // Daemon threads: if the non-volatile thread never sees the write (the
    // very bug being demonstrated), the JVM can still exit afterwards.
    t1.isDaemon = true
    t2.isDaemon = true
    t1.start(); t2.start()

    Thread.sleep(runMillis)
    keepRunningNonVolatile = false
    keepRunningVolatile = false

    t1.join(1000)
    t2.join(1000)
    if (t1.isAlive) {
        println("[non-volatile] thread never observed the write — the JIT hoisted the plain field read out of the loop. This is the visibility bug @Volatile fixes.")
    }
    if (t2.isAlive) {
        println("[volatile] thread still alive unexpectedly.")
    }
}
