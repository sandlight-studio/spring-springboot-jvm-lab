package studio.sandlight.jvm

import java.util.concurrent.atomic.AtomicLong

@Volatile private var keepRunningVolatile = true
private var keepRunningNonVolatile = true

fun volatileVisibilityDemo() {
    println("Demonstrating visibility with and without @Volatile (2 seconds).")

    val counter = AtomicLong(0)

    val t1 = Thread {
        while (keepRunningNonVolatile) {
            counter.incrementAndGet()
        }
        println("[non-volatile] observed stop flag, iterations=${counter.get()}")
    }

    val t2 = Thread {
        while (keepRunningVolatile) {
            // busy loop
        }
        println("[volatile] observed stop flag")
    }

    t1.start(); t2.start()

    Thread.sleep(2000)
    keepRunningNonVolatile = false
    keepRunningVolatile = false

    // Safety timeout so the demo cannot hang forever
    t1.join(1000)
    t2.join(1000)
    if (t1.isAlive) {
        println("[non-volatile] thread did not stop promptly; lack of visibility can cause this.")
        t1.interrupt()
    }
    if (t2.isAlive) {
        println("[volatile] thread still alive unexpectedly.")
        t2.interrupt()
    }
}

