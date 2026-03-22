package studio.sandlight.lang.strings

// ══════════════════════════════════════════════════════════════════
// Kotlin / Java String — Four Levels
//
//   ┌─────────────────────────────────────────────────────────────┐
//   │  LEVEL 4: Performance & Internals                           │
//   │    StringBuilder / StringJoiner / compact strings (Java 9) │
//   │    O(n²) + loop trap / G1 string deduplication             │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 3: Encoding & Unicode                                │
//   │    UTF-8 variable-width / Charset / code points            │
//   │    surrogate pairs / emoji / mojibake pitfalls             │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 2: Operations & Kotlin Features                      │
//   │    string templates / raw strings / Regex                  │
//   │    split / joinToString / format / Locale                  │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 1: Fundamentals                                      │
//   │    String pool / intern() / identity (===) / immutability  │
//   │    compact strings: value=byte[] + coder (Java 9+)         │
//   └─────────────────────────────────────────────────────────────┘
//
// 核心 JDK 入口：
//   java.lang.String          → value (byte[]) + coder (byte) since Java 9
//   java.lang.String.intern() → native → StringTable::intern (C++)
//   java.lang.StringBuilder   → AbstractStringBuilder.ensureCapacityInternal()
//   java.lang.Character       → codePointAt() / isHighSurrogate() / toCodePoint()
//   java.nio.charset.StandardCharsets → UTF_8 / ISO_8859_1 / US_ASCII
// ══════════════════════════════════════════════════════════════════

object StringBasics {

    fun run() {
        StringFundamentals.run()
        StringOperations.run()
        StringEncoding.run()
        StringPerformance.run()
    }
}
