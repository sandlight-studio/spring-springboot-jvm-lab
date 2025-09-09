package studio.sandlight.jvm

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    // Back-compat: numeric first arg -> alloc seconds
    val first = args[0]
    first.toIntOrNull()?.let { secs ->
        allocDemo(secs)
        return
    }

    when (first.lowercase()) {
        "alloc" -> {
            val secs = args.getOrNull(1)?.toIntOrNull() ?: 5
            allocDemo(secs)
        }
        "classloaders" -> classLoaderDemo()
        "stack" -> {
            val depth = args.getOrNull(1)?.toIntOrNull() ?: 1_000
            stackDemo(depth)
        }
        "strings" -> stringInternDemo()
        "escape" -> {
            val iterations = args.getOrNull(1)?.toIntOrNull() ?: 5_000_000
            escapeAnalysisDemo(iterations)
        }
        "daemon" -> daemonThreadDemo()
        "volatile" -> volatileVisibilityDemo()
        else -> {
            println("Unknown demo: '$first'\n")
            printUsage()
        }
    }
}

private fun printUsage() {
    println(
        """
        jvm-lab demos (choose one):
          alloc [seconds]        - allocate arrays to trigger GC
          classloaders           - show classloader hierarchy
          stack [depth]          - recursion to observe stack behavior (-Xss)
          strings                - String pool and interning
          escape [iterations]    - escape analysis microbench
          daemon                 - daemon vs user thread behavior
          volatile               - @Volatile visibility example

        Examples:
          ./gradlew :jvm-lab:run --args=alloc
          ./gradlew :jvm-lab:run --args="alloc 10"
          ./gradlew :jvm-lab:run --args=classloaders
          ./gradlew :jvm-lab:run --args="stack 2000" -Dorg.gradle.jvmargs=-Xss256k
          ./gradlew :jvm-lab:run --args="escape 10000000" -Dorg.gradle.jvmargs="-XX:-DoEscapeAnalysis"
        """.trimIndent()
    )
}

