package studio.sandlight.lang

import studio.sandlight.lang.collections.CollectionsBasics
import studio.sandlight.lang.concurrency.ConcurrencyBasics
import studio.sandlight.lang.io.IoBasics
import studio.sandlight.lang.reflection.ReflectionBasics
import studio.sandlight.lang.strings.StringBasics

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
          strings       - String pool, encoding, Unicode, StringBuilder performance
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

