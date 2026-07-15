package studio.sandlight.lang.strings

import studio.sandlight.lang.support.Lab

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object StringEncoding {

    fun run() {
        demo31CharsetBasics()
        demo32Utf8VariableWidth()
        demo33CodePointsVsChars()
        demo34SurrogatePairs()
        demo35CharsetConversionPitfalls()
    }

    // ──────────────────────────────────────────────────────────────
    // 3.1 Charset Basics
    //
    //   Java's String.getBytes() uses platform default charset — DANGEROUS!
    //   Kotlin's String.toByteArray() defaults to UTF-8 — safe.
    //
    //   Always specify charset explicitly:
    //     str.toByteArray(Charsets.UTF_8)          ← Kotlin idiom
    //     str.getBytes(StandardCharsets.UTF_8)     ← Java idiom
    //     String(bytes, StandardCharsets.UTF_8)    ← decode explicitly
    //
    //   Charset hierarchy:
    //     Charset.defaultCharset()     → platform-dependent (avoid!)
    //     StandardCharsets.UTF_8       → always available
    //     StandardCharsets.ISO_8859_1  → 1-byte Western European
    //     StandardCharsets.US_ASCII    → 7-bit ASCII only
    //
    // 阅读: java.nio.charset.Charset → decode() / encode() → CharsetDecoder / CharsetEncoder
    //       java.nio.charset.StandardCharsets → well-known constant charsets
    // ──────────────────────────────────────────────────────────────
    private fun demo31CharsetBasics() {
        Lab.section("3.1", "Charset Basics")

        println("Platform default charset: ${Charset.defaultCharset().name()}")

        val ascii = "Hello"
        val utf8Bytes = ascii.toByteArray(Charsets.UTF_8)
        val isoBytes = ascii.toByteArray(Charsets.ISO_8859_1)
        println("\n\"$ascii\" UTF-8  bytes (${utf8Bytes.size}): ${utf8Bytes.toHex()}")
        println("\"$ascii\" ISO-8859-1 bytes (${isoBytes.size}): ${isoBytes.toHex()}")
        println("  → ASCII range is identical in both charsets")

        val chinese = "中"
        val chineseUtf8 = chinese.toByteArray(Charsets.UTF_8)
        val chineseIso = chinese.toByteArray(Charsets.ISO_8859_1)
        println("\n\"$chinese\" UTF-8  bytes (${chineseUtf8.size}): ${chineseUtf8.toHex()}")
        println("\"$chinese\" ISO-8859-1 bytes (${chineseIso.size}): ${chineseIso.toHex()} → lossy! decoded back: \"${String(chineseIso, Charsets.ISO_8859_1)}\"")

        val roundTrip = String(chineseUtf8, Charsets.UTF_8)
        println("\nRound-trip encode/decode \"$chinese\" → bytes → \"$roundTrip\" (match=${chinese == roundTrip})")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.2 UTF-8 Variable-Width Encoding
    //
    //   Code point range      Bytes  Bit pattern
    //   U+0000  – U+007F      1      0xxxxxxx
    //   U+0080  – U+07FF      2      110xxxxx 10xxxxxx
    //   U+0800  – U+FFFF      3      1110xxxx 10xxxxxx 10xxxxxx
    //   U+10000 – U+10FFFF    4      11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
    //
    //   'A'  = U+0041 → 1 byte:  0x41
    //   '中' = U+4E2D → 3 bytes: 0xE4 0xB8 0xAD
    //   '🍀' = U+1F340 → 4 bytes: 0xF0 0x9F 0x8D 0x80
    //
    // 阅读: RFC 3629 (UTF-8 spec) — or java.nio.charset.UTF_8 source
    // ──────────────────────────────────────────────────────────────
    private fun demo32Utf8VariableWidth() {
        Lab.section("3.2", "UTF-8 Variable-Width Encoding")

        val samples = listOf("A", "中", "🍀", "Hello, 世界!")
        for (sample in samples) {
            val bytes = sample.toByteArray(Charsets.UTF_8)
            val codePoints = sample.codePoints().toArray()
            val cpHex = codePoints.joinToString(" ") { "U+%04X".format(it) }
            val hexDump = bytes.joinToString(", ", "[", "]") { "0x%02X".format(it) }
            println("  \"$sample\"  code point(s): $cpHex  →  ${bytes.size} UTF-8 byte(s): $hexDump")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3.3 Code Points vs Chars
    //
    //   Java char = 16-bit UTF-16 code unit (BMP only: U+0000..U+FFFF)
    //   Chars outside BMP use TWO chars (surrogate pair):
    //     High surrogate: U+D800..U+DBFF  (first char)
    //     Low  surrogate: U+DC00..U+DFFF  (second char)
    //
    //   "🍀".length      = 2  (two chars: high + low surrogate)
    //   "🍀".codePointCount(0, 2) = 1  (one Unicode code point)
    //
    //   Correct iteration:
    //     "🍀👋".codePoints().forEach { cp -> ... }  ← IntStream of code points
    //     NOT: for (char in str) { ... }              ← misses surrogates!
    //
    // 阅读: java.lang.Character → isHighSurrogate() / isLowSurrogate()
    //       java.lang.String → codePointAt(index) / codePoints() stream
    // ──────────────────────────────────────────────────────────────
    private fun demo33CodePointsVsChars() {
        Lab.section("3.3", "Code Points vs Chars")

        val str = "Hello, 🍀👋中文"
        println("String: \"$str\"")
        println("  str.length             = ${str.length}  (UTF-16 char count)")
        println("  str.codePointCount(…)  = ${str.codePointCount(0, str.length)}  (Unicode code point count)")

        println("\n  Iterating via codePoints():")
        str.codePoints().forEach { cp ->
            val rendered = String(Character.toChars(cp))
            println("    U+%04X (%s)".format(cp, rendered))
        }

        val highSurrogate = str[7]
        val lowSurrogate  = str[8]
        println("\n  str.charAt(7) = '${highSurrogate}' → isHighSurrogate = ${Character.isHighSurrogate(highSurrogate)}")
        println("  str.charAt(8) = '${lowSurrogate}'  → isLowSurrogate  = ${Character.isLowSurrogate(lowSurrogate)}")
    }

    // 3.4 Surrogate Pairs
    private fun demo34SurrogatePairs() {
        Lab.section("3.4", "Surrogate Pairs")

        val clover = "🍀"
        val high = clover[0]
        val low  = clover[1]

        println("\"🍀\"[0] = '${high}' (U+%04X)".format(high.code))
        println("\"🍀\"[1] = '${low}'  (U+%04X)".format(low.code))
        println("Character.isHighSurrogate(\"🍀\"[0]) = ${Character.isHighSurrogate(high)}")
        println("Character.isLowSurrogate(\"🍀\"[1])  = ${Character.isLowSurrogate(low)}")

        val codePoint = Character.toCodePoint(high, low)
        println("Character.toCodePoint(high, low)   = U+%04X  (${codePoint})".format(codePoint))

        val reconstructed = String(Character.toChars(0x1F340))
        println("String(Character.toChars(0x1F340)) = \"$reconstructed\"  (reconstructed)")

        println("\nSurrogate ranges:")
        println("  High surrogates: U+D800 – U+DBFF")
        println("  Low  surrogates: U+DC00 – U+DFFF")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.5 Charset Conversion Pitfalls — Mojibake (文字化け)
    //
    //   Mojibake: bytes encoded in one charset decoded with another → garbage
    //
    //   Common traps:
    //     1. File written as UTF-8, read with ISO-8859-1 → garbled CJK/emoji
    //     2. Java getBytes() (platform default) sent over network → breaks on different OS
    //     3. BOM (Byte Order Mark) in UTF-8 file: 0xEF 0xBB 0xBF prefix
    //        → causes phantom character at start of first line
    //
    // ──────────────────────────────────────────────────────────────
    private fun demo35CharsetConversionPitfalls() {
        Lab.section("3.5", "Charset Conversion Pitfalls — Mojibake (文字化け)")

        val cjk = "中文"
        val utf8Bytes = cjk.toByteArray(Charsets.UTF_8)
        val mojibake = String(utf8Bytes, Charsets.ISO_8859_1)
        println("\"$cjk\" encoded as UTF-8, decoded with ISO-8859-1:")
        println("  → \"$mojibake\"  (mojibake!)")

        val hello = "Hello"
        val isoBytes = hello.toByteArray(Charsets.ISO_8859_1)
        val safeRoundtrip = String(isoBytes, Charsets.UTF_8)
        println("\n\"$hello\" encoded as ISO-8859-1, decoded with UTF-8:")
        println("  → \"$safeRoundtrip\"  (safe — ASCII range overlaps)")

        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bomHex = bom.toHex()
        val bomDecoded = String(bom, Charsets.UTF_8)
        println("\nUTF-8 BOM bytes: $bomHex")
        println("Decoded BOM string length: ${bomDecoded.length}")
        println("BOM character code point: U+%04X (\\uFEFF — phantom char at line start!)".format(bomDecoded[0].code))
    }

    private fun ByteArray.toHex(): String =
        joinToString(", ", "[", "]") { "0x%02X".format(it) }
}
