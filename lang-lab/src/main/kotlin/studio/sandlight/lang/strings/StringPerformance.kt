package studio.sandlight.lang.strings

import studio.sandlight.lang.support.Lab

import java.util.StringJoiner
import java.util.stream.Collectors
import kotlin.system.measureNanoTime

object StringPerformance {

    fun run() {
        demo41CompactStrings()
        demo42StringBuilderInternals()
        demo43QuadraticConcatTrap()
        demo44StringJoiner()
        demo45StringDeduplication()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.1 Compact Strings (Java 9+)
    //
    //   Before Java 9: String.value = char[]  (2 bytes per char always)
    //   Since  Java 9: String.value = byte[]  (1 or 2 bytes per char)
    //                  String.coder = 0 (LATIN1) or 1 (UTF16)
    //
    //   LATIN1 strings (ASCII + Latin-1 supplement):
    //     "Hello" → coder=0, value.length=5   (saves 50% vs char[])
    //
    //   UTF16 strings (any non-Latin-1 character):
    //     "中文"  → coder=1, value.length=4   (2 bytes × 2 chars)
    //     "Hi🍀"  → coder=1, value.length=8   (2+2+4 bytes in UTF16)
    //
    //   Impact: 30–50% heap reduction for typical ASCII-heavy workloads
    //           (HTTP headers, JSON keys, log messages, class names)
    //
    // 阅读: java.lang.String → COMPACT_STRINGS (static final boolean)
    //       → private final byte[] value
    //       → private final byte coder  (LATIN1=0, UTF16=1)
    //       → static final boolean COMPACT_STRINGS (= true by default)
    // ──────────────────────────────────────────────────────────────
    private fun demo41CompactStrings() {
        Lab.section("4.1", "Compact Strings (Java 9+)")

        val valueField = String::class.java.getDeclaredField("value").also { it.isAccessible = true }
        val coderField = String::class.java.getDeclaredField("coder").also { it.isAccessible = true }

        val samples = listOf("Hello", "中文", "Hi🍀", "café")

        println("%-12s  %s  %s".format("String", "coder (0=LATIN1/1=UTF16)", "value.length"))
        println("-".repeat(55))

        for (str in samples) {
            val bytes = valueField.get(str) as ByteArray
            val coder = coderField.getByte(str)
            println("%-12s  %-26d  %d".format(str, coder, bytes.size))
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4.2 StringBuilder Internals
    //
    //   StringBuilder capacity growth strategy:
    //     initial capacity = 16  (or str.length + 16 if seeded)
    //     when full: newCapacity = oldCapacity * 2 + 2
    //
    //   "append × 17 chars"  → capacity grows: 16 → 34 → 70 → ...
    //
    //   Key operations:
    //     append(x)      → O(1) amortized  (occasional resize)
    //     insert(0, x)   → O(n)            (shifts entire array right)
    //     delete(i, j)   → O(n)            (shifts remaining left)
    //     reverse()      → O(n)
    //
    // 阅读: java.lang.AbstractStringBuilder
    //       → ensureCapacityInternal(minimumCapacity)
    //       → Arrays.copyOf(value, newCapacity)  ← the resize
    // ──────────────────────────────────────────────────────────────
    private fun demo42StringBuilderInternals() {
        Lab.section("4.2", "StringBuilder Internals")

        val sb = StringBuilder()
        println("Initial capacity: ${sb.capacity()}")

        var lastCapacity = sb.capacity()
        var resizeCount = 0

        repeat(80) { i ->
            sb.append('a')
            val current = sb.capacity()
            if (current != lastCapacity) {
                resizeCount++
                println("  Resize #$resizeCount at length=${sb.length}: capacity $lastCapacity → $current  (rule: old*2+2 = ${lastCapacity * 2 + 2})")
                lastCapacity = current
            }
        }

        println("Final length=${sb.length}, capacity=${sb.capacity()}")

        val sbInsert = StringBuilder("world")
        println("\nBefore insert(0, \"hello \"): \"$sbInsert\"")
        sbInsert.insert(0, "hello ")
        println("After  insert(0, \"hello \"): \"$sbInsert\"  ← O(n): shifted all existing chars right")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.3 String + in a Loop — O(n²) Trap
    //
    //   var s = ""
    //   for (i in 0..n) s += i.toString()
    //
    //   Each += creates a NEW StringBuilder internally:
    //     new StringBuilder(s).append(i).toString()
    //
    //   → copies grow quadratically:
    //     iteration 0: copy 0 chars
    //     iteration 1: copy 1 char
    //     iteration n: copy n chars
    //     total copies: 0+1+2+...+n = n*(n+1)/2 = O(n²)
    //
    //   Fix: use buildString { } or reuse a StringBuilder.
    //
    //   Kotlin's buildString { append(...) } compiles to a single
    //   StringBuilder reused across the entire block — idiomatic and fast.
    //
    // 阅读: kotlinc decompile shows INVOKESPECIAL StringBuilder.<init> inside loop
    // ──────────────────────────────────────────────────────────────
    private fun demo43QuadraticConcatTrap() {
        Lab.section("4.3", "String + in a Loop — O(n²) Trap")

        val n = 5000

        var s = ""
        val timeA = measureNanoTime {
            repeat(n) { s += "x" }
        }

        var result = ""
        val timeB = measureNanoTime {
            result = buildString { repeat(n) { append("x") } }
        }

        val msA = timeA / 1_000_000.0
        val msB = timeB / 1_000_000.0
        val speedup = if (timeB > 0) timeA.toDouble() / timeB else Double.NaN

        println("Approach A (string +=):    %.3f ms  length=${s.length}".format(msA))
        println("Approach B (buildString):  %.3f ms  length=${result.length}".format(msB))
        println("Speedup: %.1fx  (both produce same length? ${s.length == result.length})".format(speedup))
    }

    // ──────────────────────────────────────────────────────────────
    // 4.4 StringJoiner / joinToString
    //
    //   Java StringJoiner (Java 8+):
    //     StringJoiner(delimiter, prefix, suffix)
    //     → used internally by String.join() and Collectors.joining()
    //
    //   Kotlin joinToString — idiomatic:
    //     list.joinToString(separator, prefix, suffix, limit, truncated) { transform }
    //
    //   Stream equivalent:
    //     stream.collect(Collectors.joining(", ", "[", "]"))
    //
    // 阅读: java.util.StringJoiner → merge() — efficient joining without intermediate strings
    //       java.util.stream.Collectors → joining() → uses StringJoiner internally
    // ──────────────────────────────────────────────────────────────
    private fun demo44StringJoiner() {
        Lab.section("4.4", "StringJoiner / joinToString")

        val joiner = StringJoiner(", ", "[", "]")
        (1..5).forEach { joiner.add(it.toString()) }
        println("StringJoiner(', ', '[', ']') with 1..5: $joiner")

        val joined = java.lang.String.join(" | ", listOf("a", "b", "c"))
        println("String.join(' | ', [a, b, c]): $joined")

        val basic = (1..5).toList().joinToString()
        println("joinToString() basic: $basic")

        val withFix = (1..5).toList().joinToString(separator = "-", prefix = "<<", postfix = ">>")
        println("joinToString(sep='-', prefix='<<', suffix='>>'): $withFix")

        val withLimit = (1..10).toList().joinToString(limit = 4, truncated = "...and more")
        println("joinToString(limit=4, truncated='...and more'): $withLimit")

        val transformed = listOf("alpha", "beta", "gamma").joinToString(", ") { it.uppercase() }
        println("joinToString with transform (uppercase): $transformed")

        val streamed = (1..5).toList().stream()
            .map { it.toString() }
            .collect(Collectors.joining(", ", "{", "}"))
        println("Stream Collectors.joining(', ', '{', '}'): $streamed")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.5 String Deduplication (G1 GC, Java 8u20+)
    //
    //   JVM flag: -XX:+UseStringDeduplication  (G1 GC only)
    //
    //   How it works:
    //     At GC time, G1 scans young-gen String objects.
    //     If two strings have equal content (different references),
    //     G1 replaces both backing byte[] arrays with a single shared one.
    //
    //   NOT the same as intern():
    //     intern() → adds to String pool, changes reference identity
    //     dedup    → shares backing array only, reference identity unchanged
    //
    //   Best for: apps with many duplicate strings (log messages, HTTP headers,
    //             config values, JSON field names read repeatedly)
    //
    //   Monitor: -XX:+PrintStringDeduplicationStatistics
    //
    //   Verify dedup happened: use -XX:+PrintStringTableStatistics
    //   or JFR event: jdk.StringDeduplication
    //
    // 阅读: share/gc/g1/g1StringDedup.cpp in OpenJDK source
    // ──────────────────────────────────────────────────────────────
    private fun demo45StringDeduplication() {
        Lab.section("4.5", "String Deduplication (G1 GC)")

        val list = List(1000) { "repeated-value" }
        println("Created ${list.size} strings with content: '${list[0]}'")
        println("All same content? ${list.all { it == "repeated-value" }}")
        println("String literals are pooled — same reference? ${list[0] === list[1]}")

        println()
        println("JVM flag: -XX:+UseStringDeduplication  (G1 GC only)")
        println("  → G1 scans young-gen Strings at GC time")
        println("  → Equal-content strings share one backing byte[]")
        println("  → Reference identity is preserved (unlike intern())")
        println("  → Monitor with: -XX:+PrintStringDeduplicationStatistics")

        val heapList = List(1000) { String(charArrayOf('x', 'y')) }
        println()
        println("Heap-allocated 'xy' strings (not pooled):")
        println("  heapList[0] == heapList[1]? ${heapList[0] == heapList[1]}  (same content)")
        println("  Built strings same ref? ${heapList[0] === heapList[1]}  (dedup would share their byte[])")
    }
}
