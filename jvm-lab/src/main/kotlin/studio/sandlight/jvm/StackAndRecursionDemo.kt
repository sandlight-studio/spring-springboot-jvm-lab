package studio.sandlight.jvm

fun stackDemo(targetDepth: Int) {
    println("Recursing to depth ~$targetDepth. Adjust -Xss to explore stack size.")
    try {
        val depth = recurse(0, targetDepth)
        println("Completed recursion to depth $depth without StackOverflowError.")
    } catch (e: StackOverflowError) {
        println("StackOverflowError thrown before reaching target depth. Try increasing -Xss.")
    }
}

private tailrec fun recurse(depth: Int, target: Int): Int {
    return if (depth >= target) depth else recurse(depth + 1, target)
}

