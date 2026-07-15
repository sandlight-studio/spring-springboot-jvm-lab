package studio.sandlight.lang

import studio.sandlight.lang.collections.CollectionsBasics
import studio.sandlight.lang.concurrency.ConcurrencyBasics
import studio.sandlight.lang.io.IoBasics
import studio.sandlight.lang.kotlin.KotlinBasics
import studio.sandlight.lang.reflection.ReflectionBasics
import studio.sandlight.lang.strings.StringBasics
import studio.sandlight.lang.support.Topic
import studio.sandlight.lang.support.run

// Single registry: usage text and dispatch are both derived from this list,
// so adding a topic is one line. (Also drives the smoke tests.)
internal val topics: List<Topic> = listOf(
    StringBasics,
    CollectionsBasics,
    ConcurrencyBasics,
    IoBasics,
    ReflectionBasics,
    KotlinBasics,
)

fun main(args: Array<String>) {
    val topic = topics.find { it.name == args.getOrNull(0)?.lowercase() }
    if (topic == null) {
        if (args.isNotEmpty()) println("Unknown topic: ${args[0]}\n")
        printUsage()
        return
    }
    topic.run(args.getOrNull(1))
}

private fun printUsage() {
    println("lang-lab topics (usage: <topic> [level]):")
    topics.forEach { println("  ${it.name.padEnd(13)} - ${it.description}") }
    println(
        """

        Every topic runs all levels by default; pick one with a number or name:
          ./gradlew :lang-lab:run --args=strings --quiet
          ./gradlew :lang-lab:run --args="strings 1" --quiet
          ./gradlew :lang-lab:run --args="reflection expert" --quiet
          ./gradlew :lang-lab:run --args="kotlin coroutines" --quiet
        """.trimIndent()
    )
}
