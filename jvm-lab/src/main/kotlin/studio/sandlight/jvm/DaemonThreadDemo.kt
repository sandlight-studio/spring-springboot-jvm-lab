package studio.sandlight.jvm

fun daemonThreadDemo() {
    println("Starting a daemon and a user thread; observe JVM exit behavior.")

    val daemon = Thread({
        try {
            println("[daemon] running; will sleep 3s")
            Thread.sleep(3000)
            println("[daemon] woke up (you might not see this if JVM exits earlier)")
        } catch (_: InterruptedException) {}
    }, "demo-daemon").apply { isDaemon = true }

    val user = Thread({
        try {
            println("[user] running; will sleep 1s")
            Thread.sleep(1000)
            println("[user] finished work")
        } catch (_: InterruptedException) {}
    }, "demo-user")

    daemon.start()
    user.start()
    // Wait only for the user thread; JVM may exit even if daemon still running.
    user.join()
    println("Main exits after user thread completes. If daemon is still sleeping, JVM will terminate it.")
}

