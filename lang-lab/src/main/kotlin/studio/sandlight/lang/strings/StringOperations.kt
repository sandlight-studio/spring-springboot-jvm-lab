package studio.sandlight.lang.strings

import studio.sandlight.lang.support.Lab

import java.util.Locale

object StringOperations {

    fun run() {
        demo21StringTemplates()
        demo22RawStrings()
        demo23CoreStringApi()
        demo24Regex()
        demo25StringFormat()
    }

    // ──────────────────────────────────────────────────────────────
    // 2.1 String Templates
    //
    // Kotlin template compilation → JVM bytecode:
    //
    //   Kotlin:  "Hello, $name! Age=${age + 1}"
    //
    //   Bytecode equivalent:
    //     StringBuilder()
    //       .append("Hello, ").append(name)
    //       .append("! Age=").append(age + 1)
    //       .toString()
    //
    //   Nested call: "result=${list.map { it * 2 }.sum()}"
    //   → expression inside ${} can be any Kotlin expression
    //
    // 阅读: kotlinc -include-runtime → javap -c → INVOKEVIRTUAL StringBuilder.append
    // ──────────────────────────────────────────────────────────────
    private fun demo21StringTemplates() {
        Lab.section("2.1", "String Templates")

        val name = "Kotlin"
        val age = 10
        val list = listOf(1, 2, 3, 4, 5)

        // Simple $var
        val simple = "Hello, $name!"
        println("Simple \$var:       $simple")

        // Expression ${expr}
        val expr = "Next year age: ${age + 1}"
        println("Expression \${...}: $expr")

        // Nested call inside ${}
        val nested = "Doubled sum: ${list.map { it * 2 }.sum()}"
        println("Nested call:       $nested")

        // Multi-line template
        val multiLine = """
            Name : $name
            Age  : $age
            List : $list
            Sum  : ${list.sum()}
        """.trimIndent()
        println("Multi-line template:\n$multiLine")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.2 Raw Strings
    //
    //   trimMargin("|") — strips leading whitespace up to and including "|"
    //                     each line must start with "|" (or custom prefix)
    //   trimIndent()    — removes the common leading indent from all lines
    //                     more flexible; no per-line marker needed
    //
    //   Escaping $ in raw string: use ${'$'}
    // ──────────────────────────────────────────────────────────────
    private fun demo22RawStrings() {
        Lab.section("2.2", "Raw Strings")

        // trimMargin — each line begins with "|"
        val withMargin = """
            |SELECT *
            |FROM users
            |WHERE active = true
            |ORDER BY name
        """.trimMargin()
        println("trimMargin result:\n$withMargin")

        // trimIndent — common indent removed automatically
        val withIndent = """
            {
                "lang": "Kotlin",
                "version": 2
            }
        """.trimIndent()
        println("trimIndent result:\n$withIndent")

        // JSON-like snippet embedded directly — no escaping needed in raw strings
        val json = """
            |{
            |  "host": "localhost",
            |  "port": 5432,
            |  "db": "mydb"
            |}
        """.trimMargin()
        println("Embedded JSON-like snippet:\n$json")

        // Literal dollar sign inside a raw string using ${'$'}
        val price = 42
        val dollarLiteral = """
            |Item cost: ${'$'}$price
            |Tax (10%): ${'$'}${price * 10 / 100}
        """.trimMargin()
        println("Literal \$ in raw string:\n$dollarLiteral")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.3 Core String API
    // ──────────────────────────────────────────────────────────────
    private fun demo23CoreStringApi() {
        Lab.section("2.3", "Core String API")

        val s = "Hello, World!"

        println("original          : $s")
        println("substring(1..3)   : ${s.substring(1..3)}")
        println("take(3)           : ${s.take(3)}")
        println("drop(2)           : ${s.drop(2)}")
        println("startsWith(\"He\")  : ${s.startsWith("He")}")
        println("endsWith(\"!\")     : ${s.endsWith("!")}")
        println("contains(\"World\") : ${s.contains("World")}")
        println("replace(\"l\",\"L\")  : ${s.replace("l", "L")}")
        println("replaceFirst      : ${s.replaceFirst("l", "L")}")

        val csv = "one,two,three"
        val parts = csv.split(",")
        println("split(\",\")        : $parts")
        println("joinToString      : ${parts.joinToString(" | ")}")

        println("padStart(5,'-')   : ${"AB".padStart(5, '-')}")
        println("padEnd(5,'-')     : ${"AB".padEnd(5, '-')}")
        println("repeat(3)         : ${"abc".repeat(3)}")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.4 Regex
    //
    //   compile once, reuse many — Pattern.compile() is expensive
    //   → store as companion val or top-level val
    //
    //   Key API:
    //     find()      → first match (MatchResult or null)
    //     findAll()   → sequence of all matches
    //     matches()   → full-string match
    //     replace()   → substitution (supports backreferences $1)
    //     split()     → split by pattern
    //
    //   Named groups: (?<name>...)  → matchResult.groups["name"]?.value
    //
    // 阅读: java.util.regex.Pattern → compile() → NFA/DFA engine
    //       java.util.regex.Matcher → find() → next()
    // ──────────────────────────────────────────────────────────────
    private val emailRegex = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
    private val dateRegex = Regex("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})")

    private fun demo24Regex() {
        Lab.section("2.4", "Regex")

        val text = "Contact us at support@example.com or sales@company.org for help."

        // find — first match
        val first = emailRegex.find(text)
        println("find() first email    : ${first?.value}")

        // findAll — all matches
        val all = emailRegex.findAll(text).map { it.value }.toList()
        println("findAll() all emails  : $all")

        // matches — full-string match
        val validEmail = "user@domain.com"
        val invalidEmail = "not-an-email"
        println("matches() valid       : ${emailRegex.matches(validEmail)}")
        println("matches() invalid     : ${emailRegex.matches(invalidEmail)}")

        // replace with backreference — wrap each email in brackets
        val bracketed = emailRegex.replace(text, "[$0]")
        println("replace() bracketed   : $bracketed")

        // split by whitespace-or-comma
        val splitResult = Regex("[,\\s]+").split("one, two,three  four")
        println("split() result        : $splitResult")

        // Named groups — parse a date
        val dateStr = "2024-03-15"
        val match = dateRegex.find(dateStr)
        if (match != null) {
            val year  = match.groups["year"]?.value
            val month = match.groups["month"]?.value
            val day   = match.groups["day"]?.value
            println("Named groups ($dateStr) → year=$year  month=$month  day=$day")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2.5 String.format & Locale sensitivity
    //
    //   String.format(format, args...)  → uses default Locale
    //   String.format(Locale.US, ...)   → explicit locale (safe for APIs)
    //
    //   Locale trap: "%.2f".format(3.14) in German locale → "3,14" (comma!)
    //   → always pass Locale.US for numbers in API responses / files
    //
    // 阅读: java.util.Formatter → format() → Conversion.print()
    //       java.util.Locale → getDefault() → system locale
    // ──────────────────────────────────────────────────────────────
    private fun demo25StringFormat() {
        Lab.section("2.5", "String.format & Locale sensitivity")

        println("%05d:                  " + "%05d".format(42))
        println("%.2f:                  " + "%.2f".format(3.14159))
        println("%-10s|:               " + "%-10s|".format("left"))
        println("%10s|:                " + "%10s|".format("right"))
        println("Locale.US price:       " + String.format(Locale.US, "Price: %.2f USD", 1234.5))
        println("Locale.GERMANY price:  " + String.format(Locale.GERMANY, "Preis: %.2f EUR", 1234.5))
    }
}
