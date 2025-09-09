package studio.sandlight.lang

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ConcurrencyBasics {
    fun run() {
        println("-- Concurrency basics --")

        // Thread and shared counter (AtomicInteger)
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(4)
        val pool = Executors.newFixedThreadPool(4)
        repeat(4) { idx ->
            pool.submit {
                repeat(100_000) { counter.incrementAndGet() }
                println("worker-$idx done")
                latch.countDown()
            }
        }
        latch.await(2, TimeUnit.SECONDS)
        pool.shutdown()
        println("counter=${counter.get()} (expected 400000)")

        // CompletableFuture
        val fut: CompletableFuture<Int> = CompletableFuture.supplyAsync {
            Thread.sleep(100);
            42
        }.thenApply { it * 2 }
        println("CompletableFuture result=${fut.join()}")

        // Synchronized block (mutual exclusion)
        val lock = Any()
        var shared = 0
        val t1 = Thread { synchronized(lock) { shared += 1 } }
        val t2 = Thread { synchronized(lock) { shared += 2 } }
        t1.start(); t2.start(); t1.join(); t2.join()
        println("shared via synchronized=$shared")
    }
}

