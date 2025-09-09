package studio.sandlight.jvm

import kotlin.system.measureTimeMillis

fun allocDemo(seconds: Int = 5) {
    println("Starting allocation demo for ${seconds}s …")
    val data = mutableListOf<ByteArray>()
    val elapsed = measureTimeMillis {
        val end = System.nanoTime() + seconds * 1_000_000_000L
        var i = 0
        while (System.nanoTime() < end) {
            data += ByteArray(256 * 1024) // 256KB
            if (data.size > 100) data.clear() // allow GC
            if (++i % 1000 == 0) Thread.yield()
        }
    }
    println("Done. Elapsed: ${elapsed}ms; arrays kept: ${data.size}")
}
