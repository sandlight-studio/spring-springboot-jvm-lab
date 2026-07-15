package studio.sandlight.lang.support

/**
 * Console helpers shared by every topic. All banners and dividers render
 * through here so the whole lab has one visual language — no per-file
 * "=".repeat(<magic number>) drift.
 */
object Lab {
    const val WIDTH = 65

    fun rule(char: Char = '=') = println(char.toString().repeat(WIDTH))

    fun topicBanner(title: String) {
        println()
        rule('═')
        println("  $title")
        rule('═')
    }

    fun level(n: Int, title: String) {
        println()
        rule()
        println("  LEVEL $n: $title")
        rule()
    }

    fun section(id: String, title: String) = println("\n--- $id $title ---")
}

/** One runnable level of a topic; `key` is what the CLI matches on. */
data class Level(
    val number: Int,
    val key: String,
    val title: String,
    val run: () -> Unit,
    val aliases: List<String> = emptyList(),
)

/**
 * Every topic is data (a name plus a list of levels) instead of code:
 * dispatch, banners, and usage text fall out of the generic [run] below,
 * so no topic needs its own arg parsing.
 */
interface Topic {
    val name: String
    val description: String
    val levels: List<Level>
}

/** Runs `null`/"all" → every level; a digit → that level; else a key/alias (prefix ok). */
fun Topic.run(selector: String?) {
    if (selector == null || selector.equals("all", ignoreCase = true)) {
        Lab.topicBanner("$name — $description")
        levels.forEach { runLevel(it) }
        return
    }
    val sel = selector.lowercase()
    val level = levels.firstOrNull { it.number.toString() == sel }
        ?: levels.firstOrNull { it.key == sel || sel in it.aliases }
        ?: levels.firstOrNull { it.key.startsWith(sel) }
    if (level == null) {
        println("Unknown level '$selector' for topic '$name'.\n")
        printLevels()
        return
    }
    runLevel(level)
}

fun Topic.printLevels() {
    println("$name — $description")
    levels.forEach { println("  ${it.number}  ${it.key.padEnd(13)} ${it.title}") }
    println("  all${" ".repeat(12)} run every level (default)")
}

private fun runLevel(level: Level) {
    Lab.level(level.number, level.title)
    level.run()
}
