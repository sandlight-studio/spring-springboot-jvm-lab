package studio.sandlight.jvm

fun stackDemo(targetDepth: Int) {
    println("Recursing to depth ~$targetDepth. Adjust -Xss to explore stack size.")

    deepestReached = 0
    try {
        recurse(0, targetDepth)
        println("[recursive] completed depth $targetDepth without StackOverflowError.")
    } catch (e: StackOverflowError) {
        println("[recursive] StackOverflowError at depth ~$deepestReached (target $targetDepth). Larger -Xss reaches deeper.")
    }

    // Contrast: the same recursion marked `tailrec` compiles to a loop —
    // no stack frames accumulate, so it completes at ANY depth.
    val depth = recurseTailrec(0, targetDepth)
    println("[tailrec]   completed depth $depth — tailrec is compiled to a loop, the stack never grows.")
}

private var deepestReached = 0

// Deliberately NOT tailrec, and the call is kept out of tail position by the
// `1 +`: each level must hold a real stack frame, which is what makes
// StackOverflowError possible. Marking this tailrec would silently turn it
// into a loop and kill the demo.
private fun recurse(depth: Int, target: Int): Int {
    deepestReached = depth
    if (depth >= target) return 0
    return 1 + recurse(depth + 1, target)
}

private tailrec fun recurseTailrec(depth: Int, target: Int): Int =
    if (depth >= target) depth else recurseTailrec(depth + 1, target)
