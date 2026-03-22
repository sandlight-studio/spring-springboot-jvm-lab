package studio.sandlight.lang.strings

import java.nio.charset.StandardCharsets

object StringFundamentals {

    fun run() {
        println("\n🔤 LEVEL 1: STRING FUNDAMENTALS - 字符串基础")
        demo11StringPoolAndInterning()
        demo12EqualityVsIdentity()
        demo13ImmutabilityAndCompactStrings()
        demo14StringCreationAPIs()
    }

    private fun demo11StringPoolAndInterning() {
        // ──────────────────────────────────────────────────────────────
        // 1.1 String Pool & Interning
        //
        // String Pool (Heap — PermGen Java 7, Heap Java 8+):
        //
        //   "Hello" literal ──► pool["Hello"]  (shared)
        //   "Hel"+"lo"      ──► pool["Hello"]  (compile-time folded by javac)
        //   buildString {}  ──► heap object    (runtime, NOT in pool)
        //   String(chars)   ──► heap object    (new instance every time)
        //   str.intern()    ──► pool["Hello"]  (returns pooled ref)
        //
        // 阅读: java.lang.String → intern() (native) → StringTable::intern (C++ side)
        //       JVM flag: -XX:StringTableSize=65536 (default bucket count)
        // ──────────────────────────────────────────────────────────────

        println("\n--- 1.1 String Pool & Interning ---")

        val a = "Hello"                              // literal, goes to pool
        val b = "Hel" + "lo"                         // javac folds at compile time → same pool entry
        val c = buildString { append("He"); append("llo") }  // runtime, NOT pooled
        val d = String("Hello".toCharArray())        // new heap object
        val e = c.intern()                           // forces pool lookup, returns pooled ref

        println("a = \"Hello\" (literal)")
        println("b = \"Hel\" + \"lo\" (compile-time folded)")
        println("c = buildString { append(\"He\"); append(\"llo\") } (runtime)")
        println("d = String(\"Hello\".toCharArray()) (new heap object)")
        println("e = c.intern() (interned)")
        println()
        println("a === b → ${a === b}  (both literals share pool entry)")
        println("a === c → ${a === c}  (buildString not pooled)")
        println("a === d → ${a === d}  (new heap object)")
        println("a === e → ${a === e}  (intern returns pooled ref)")
    }

    private fun demo12EqualityVsIdentity() {
        println("\n--- 1.2 Equality vs Identity ---")

        val literal1 = "abc"
        val literal2 = "abc"
        val built1 = buildString { append("ab"); append("c") }
        val built2 = buildString { append("ab"); append("c") }

        println("Identity (===) checks:")
        println("  %-40s → %s".format("\"abc\" === \"abc\" (two literals)", literal1 === literal2))
        println("  %-40s → %s".format("\"abc\" === buildString { ... }", literal1 === built1))
        println("  %-40s → %s".format("\"abc\" === built1.intern()", literal1 === built1.intern()))
        println("  %-40s → %s".format("buildString1 === buildString2", built1 === built2))

        println()
        println("Equality (==) checks:")
        println("  %-40s → %s".format("\"abc\" == \"abc\"", literal1 == literal2))
        println("  %-40s → %s".format("\"abc\" == buildString { ... }", literal1 == built1))
        println("  %-40s → %s".format("buildString1 == buildString2", built1 == built2))
    }

    private fun demo13ImmutabilityAndCompactStrings() {
        // ──────────────────────────────────────────────────────────────
        // 1.3 Immutability & Compact Strings (Java 9+)
        //
        // Before Java 9:  String.value = char[]  (2 bytes per char always)
        // Since Java 9:   String.value = byte[]  (1 byte for LATIN1, 2 for UTF16)
        //                 String.coder = 0 (LATIN1) or 1 (UTF16)
        //
        //   "Hello"  → coder=0 LATIN1, value.length=5   (saves 50% memory)
        //   "中文"   → coder=1 UTF16,  value.length=4   (2 bytes × 2 chars)
        //
        //   String is immutable — toCharArray() returns a COPY
        //   Modify the array → original String unchanged
        //
        // 阅读: java.lang.String → COMPACT_STRINGS (static final boolean)
        //       → coder field → encode(char[]) chooses storage format
        // ──────────────────────────────────────────────────────────────

        println("\n--- 1.3 Immutability & Compact Strings (Java 9+) ---")

        val latin1Str = "Hello"
        val utf16Str = "中文"

        // Use reflection to inspect internal representation
        val valueField = String::class.java.getDeclaredField("value").also { it.isAccessible = true }
        val coderField = String::class.java.getDeclaredField("coder").also { it.isAccessible = true }

        val latin1Bytes = valueField.get(latin1Str) as ByteArray
        val latin1Coder = coderField.get(latin1Str) as Byte

        val utf16Bytes = valueField.get(utf16Str) as ByteArray
        val utf16Coder = coderField.get(utf16Str) as Byte

        println("Compact Strings internal representation:")
        println("  \"Hello\" (LATIN1): coder=$latin1Coder, value.length=${latin1Bytes.size}")
        println("  \"中文\" (UTF16):  coder=$utf16Coder, value.length=${utf16Bytes.size}")
        println()
        println("Explanation:")
        println("  - coder=0 (LATIN1): 1 byte per character")
        println("  - coder=1 (UTF16):  2 bytes per character")
        println("  - \"Hello\" needs 5 bytes (saves 50% vs char[])")
        println("  - \"中文\" needs 4 bytes (2 chars × 2 bytes)")

        println()
        println("Immutability demonstration:")
        val original = "Hello"
        val chars = original.toCharArray()
        chars[0] = 'X'
        println("  Original: $original")
        println("  Modified array: ${String(chars)}")
        println("  Original unchanged: $original (toCharArray() returns a copy)")
    }

    private fun demo14StringCreationAPIs() {
        println("\n--- 1.4 String Creation APIs ---")

        // Literal
        val literal = "Hello, World!"
        println("Literal:              \"$literal\"")

        // buildString
        val built = buildString {
            append("Hello")
            append(", ")
            append("World!")
        }
        println("buildString { }:      \"$built\"")

        // String(charArray)
        val charArray = charArrayOf('H', 'e', 'l', 'l', 'o')
        val fromChars = String(charArray)
        println("String(charArray):    \"$fromChars\"")

        // String(bytes, charset)
        val bytes = "Hello".toByteArray(StandardCharsets.UTF_8)
        val fromBytes = String(bytes, StandardCharsets.UTF_8)
        println("String(bytes, UTF_8): \"$fromBytes\"")

        // String.format
        val formatted = String.format("Name: %s, Age: %d", "Alice", 30)
        println("String.format():      \"$formatted\"")
    }
}
