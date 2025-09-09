package studio.sandlight.jvm

import kotlin.system.measureNanoTime

private data class Point(val x: Int, val y: Int)

fun escapeAnalysisDemo(iterations: Int) {
    println("Running $iterations iterations. Try with -XX:-DoEscapeAnalysis for contrast.")
    // Warm-up to trigger JIT
    repeat(3) { runOnce(200_000) }

    val t1 = measureNanoTime { runOnce(iterations) }
    val t2 = measureNanoTime { runOnce(iterations) }
    println("Time (ns): $t1, then $t2 (second run often faster due to JIT)")
}

private fun runOnce(n: Int): Long {
    var acc = 0L
    var i = 0
    while (i < n) {
        val p = Point(i, i + 1) // may not escape; can be scalar replaced
        acc += p.x + p.y
        i++
    }
    // return to prevent dead-code elimination
    return acc
}

