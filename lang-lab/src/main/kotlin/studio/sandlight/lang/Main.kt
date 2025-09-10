package studio.sandlight.lang

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage(); return
    }
    when (args[0].lowercase()) {
        "strings" -> StringBasics.run()
        "collections" -> CollectionsBasics.run()
        "concurrency" -> ConcurrencyBasics.run()
        "io" -> IoBasics.run()
        "reflection" -> ReflectionBasics.run(args)
        else -> {
            println("Unknown topic: ${args[0]}\n"); printUsage()
        }
    }
}

private fun printUsage() {
    println(
        """
        lang-lab topics:
          strings       - Kotlin string basics and interop
          collections   - Lists, sets, maps, sequences
          concurrency   - Threads, executor, AtomicInteger
          io            - Files, resources, temp dirs
          reflection    - Java reflection from basic to expert level

        Examples:
          ./gradlew :lang-lab:run --args=strings --quiet
          ./gradlew :lang-lab:run --args=collections --quiet
          ./gradlew :lang-lab:run --args=concurrency --quiet
          ./gradlew :lang-lab:run --args=io --quiet
          ./gradlew :lang-lab:run --args="reflection basic" --quiet
        """.trimIndent()
    )
}

