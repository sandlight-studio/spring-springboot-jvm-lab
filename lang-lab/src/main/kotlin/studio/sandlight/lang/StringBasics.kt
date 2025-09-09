package studio.sandlight.lang

object StringBasics {
    fun run() {
        println("-- String basics --")

        val a = "Hello"
        val b = "Hel" + "lo" // compile-time concat
        val c = buildString { append("He"); append("llo") } // runtime
        val d = String("Hello".toCharArray()) // new instance

        println("== equals (content): a == b -> ${a == b}")
        println("=== identity: a === b -> ${a === b}")
        println("=== identity: a === d -> ${a === d}")

        // Templates and raw strings
        val name = "Kotlin"
        val msg = "$a, $name! length=${name.length}"
        println(msg)

        val raw = """
            |Line1
            |  Line2 with indent
            |Line3
        """.trimMargin()
        println(raw)

        // Useful operations
        println("substring(1..3): ${a.substring(1..3)}")
        println("lower/upper: ${a.lowercase()} / ${a.uppercase()}")
        println("split/join: ${"a,b,c".split(',').joinToString("+")}")

        // Encoding/bytes
        val bytes = "🍀".encodeToByteArray()
        println("UTF-8 byte length of 🍀: ${bytes.size}")
        println("decoded: ${bytes.decodeToString()}")
    }
}

