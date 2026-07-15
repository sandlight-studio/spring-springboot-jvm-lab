// LEVEL 1: Streams & Readers — java.io 经典 API 与装饰器模式

package studio.sandlight.lang.io

import studio.sandlight.lang.support.Lab

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.file.Files

object StreamsAndReaders {

    fun run() {
        demo11StreamHierarchy()
        demo12BufferedReader()
        demo13BufferedWriter()
        demo14CharsetDecoding()
        demo15PrintStream()
    }

    // ──────────────────────────────────────────────────────────────────
    // 1.1  InputStream / OutputStream 层次结构 & 装饰器模式
    //
    // java.io 装饰器模式（Decorator Pattern）
    //
    //   InputStream (abstract)
    //     ├── FileInputStream          ← 从文件读取原始字节
    //     ├── ByteArrayInputStream     ← 从内存字节数组读取
    //     └── FilterInputStream        ← 装饰器基类（持有 InputStream in）
    //           ├── BufferedInputStream  ← 增加 8KB 缓冲区（减少系统调用）
    //           ├── DataInputStream      ← readInt() / readUTF() 等
    //           └── ObjectInputStream    ← Java 反序列化
    //
    //   OutputStream (abstract)
    //     ├── FileOutputStream
    //     ├── ByteArrayOutputStream
    //     └── FilterOutputStream
    //           ├── BufferedOutputStream ← 减少 write() 系统调用次数
    //           ├── DataOutputStream
    //           └── ObjectOutputStream
    //
    //   Reader (abstract, 字符流)
    //     ├── InputStreamReader        ← 字节→字符桥梁（使用 Charset 解码）
    //     │     └── FileReader         ← FileInputStream + InputStreamReader 的快捷写法
    //     └── BufferedReader           ← readLine() / lines() Stream
    //
    // 装饰器层叠示例：
    //   FileInputStream → BufferedInputStream → DataInputStream
    //   每层仅添加功能，不改变接口
    //
    // 阅读：java.io.FilterInputStream → 构造函数持有 InputStream in → 所有方法委托 in
    //       java.io.BufferedInputStream → read() → 首次调用时填充内部 buf[] 数组
    // ──────────────────────────────────────────────────────────────────
    private fun demo11StreamHierarchy() {
        Lab.section("1.1", "InputStream Decorator Pattern")

        val data = "Hello, Decorator!".toByteArray()

        // Layer 1: 原始数据源 — ByteArrayInputStream
        val layer1 = ByteArrayInputStream(data)
        println("Layer 1: ${layer1.javaClass.simpleName}")

        // Layer 2: 装饰器 — BufferedInputStream（添加 8KB 缓冲，不复制原始数据）
        // BufferedInputStream 持有 layer1 的引用，委托读取，自行管理缓冲区
        val layer2 = BufferedInputStream(layer1)
        println("Layer 2: ${layer2.javaClass.simpleName}")

        // 读取所有字节（通过 layer2，实际由 layer1 提供数据）
        val result = layer2.readBytes()
        println("Read via decorated stream: \"${String(result)}\"")

        // 装饰器只添加行为，不复制底层数据
        println("Wrapping adds behavior only — same underlying data, no copy")
    }

    // ──────────────────────────────────────────────────────────────────
    // 1.2  BufferedReader — 字符缓冲读取
    //
    //   BufferedReader.readLine() → 读取一行（不含换行符）
    //   BufferedReader.lines()    → 返回 Stream<String>（惰性，逐行读取）
    //
    //   Kotlin 扩展：Reader.buffered() 包装成 BufferedReader
    //
    // 注意：Stream<String> 必须在 BufferedReader 关闭前消费完
    //   正确：reader.use { it.lines().forEach { println(it) } }
    //   错误：val stream = reader.lines(); reader.close(); stream.forEach{...} ← IOException
    //
    // 阅读：java.io.BufferedReader → readLine() → 填充 cb[] 字符缓冲数组
    // ──────────────────────────────────────────────────────────────────
    private fun demo12BufferedReader() {
        Lab.section("1.2", "BufferedReader")

        val text = "Line one\nLine two\nLine three"

        // readLine() 循环
        println("--- readLine() loop ---")
        BufferedReader(StringReader(text)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                println("  > $line")
                line = reader.readLine()
            }
        }

        // lines() — 返回惰性 Stream<String>，必须在 use { } 内消费
        println("--- lines() Stream (consumed inside use{}) ---")
        BufferedReader(StringReader(text)).use { reader ->
            reader.lines().forEach { line -> println("  >> $line") }
        }

        // Kotlin Reader.buffered() 扩展：更简洁的包装方式
        println("--- Kotlin .buffered() extension ---")
        StringReader(text).buffered().use { reader ->
            println("  Class: ${reader.javaClass.simpleName}")
            println("  First line: ${reader.readLine()}")
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 1.3  BufferedWriter — 字符缓冲写入
    //
    //   问题：每次 write() 都触发系统调用 → 性能差
    //   方案：BufferedWriter 积累到 8192 字符后才真正写出
    //
    //   重要：flush() 强制将缓冲区内容写出
    //         close() 隐式调用 flush()
    //         如果没有 close()/flush()，数据可能丢失在缓冲区！
    //
    //   Kotlin .use { } 确保 close() 被调用（即使抛出异常）
    //   等价于 Java try-with-resources
    //
    // 阅读：java.io.BufferedWriter → flushBuffer() → out.write(cb, 0, nChars)
    // ──────────────────────────────────────────────────────────────────
    private fun demo13BufferedWriter() {
        Lab.section("1.3", "BufferedWriter")

        val tmpFile = Files.createTempFile("streams-demo-", ".txt")

        try {
            // 写入：use { } 保证 close()，close() 隐式 flush()
            // 若省略 use{} 且忘记 close()/flush()，缓冲区内容不会写到磁盘
            BufferedWriter(OutputStreamWriter(Files.newOutputStream(tmpFile), Charsets.UTF_8)).use { writer ->
                writer.write("First line")
                writer.newLine()
                writer.write("Second line")
                writer.newLine()
                writer.write("Third line")
                writer.newLine()
                // close() via use{} will call flushBuffer() → out.write(cb, 0, nChars)
            }
            println("Written to: $tmpFile")

            // 读回验证
            println("--- Reading back ---")
            Files.newBufferedReader(tmpFile, Charsets.UTF_8).use { reader ->
                reader.lines().forEach { println("  $it") }
            }
        } finally {
            Files.deleteIfExists(tmpFile)
            println("Temp file deleted")
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 1.4  InputStreamReader — 字节流 → 字符流的桥梁
    //
    //   构造时指定 Charset，影响如何解码字节为字符：
    //     InputStreamReader(stream, Charsets.UTF_8)
    //     InputStreamReader(stream, Charsets.ISO_8859_1)
    //
    //   "Hello, 世界" in UTF-8:  72 bytes for 中文 (3 bytes/char)
    //   "Hello, 世界" in UTF-16: 2 bytes/char（Java 内部字符串格式）
    //
    //   常见 Bug：服务端 UTF-8 写，客户端用默认 Charset 读 → 乱码
    //
    //   Kotlin：Charsets.UTF_8 / StandardCharsets.UTF_8（Java）
    //
    // 阅读：java.io.InputStreamReader → sun.nio.cs.StreamDecoder
    //       → Charset.newDecoder() → CharsetDecoder.decode()
    // ──────────────────────────────────────────────────────────────────
    private fun demo14CharsetDecoding() {
        Lab.section("1.4", "Charset Decoding")

        val original = "Hello, 世界"

        // UTF-8 编码：中文每字符 3 字节
        val utf8Bytes = original.toByteArray(Charsets.UTF_8)
        println("Original string  : \"$original\"")
        println("UTF-8 byte count : ${utf8Bytes.size}  (\"Hello, \" = 7×1B ASCII, \"世界\" = 2×3B CJK → 7+6 = 13)")

        // 正确解码：UTF-8 → UTF-8，字符还原
        val correctDecoded = InputStreamReader(ByteArrayInputStream(utf8Bytes), Charsets.UTF_8)
            .use { it.readText() }
        println("Correct (UTF-8)  : \"$correctDecoded\"")

        // 错误解码：UTF-8 字节用 ISO-8859-1 解读 → 乱码（每字节当一个字符）
        val garbledDecoded = InputStreamReader(ByteArrayInputStream(utf8Bytes), Charsets.ISO_8859_1)
            .use { it.readText() }
        println("Garbled (Latin-1): \"$garbledDecoded\"  ← charset mismatch bug")

        // 字节数对比
        val iso8859Bytes = original.toByteArray(Charset.forName("ISO-8859-1"))
        println("ISO-8859-1 byte count: ${iso8859Bytes.size}  (CJK mapped to '?' — data loss)")
    }

    // ──────────────────────────────────────────────────────────────────
    // 1.5  PrintStream — 最常用的输出流（System.out 就是它）
    //
    //   特点：
    //   • println() 自动换行（OS 相关的 line.separator）
    //   • autoFlush=true 时，println()/write(byte[])/flush() 自动刷出
    //   • 不抛 IOException（内部捕获并设置 checkError() 标志）
    //   • System.out = new PrintStream(new FileOutputStream(FileDescriptor.out), true)
    //
    //   PrintWriter vs PrintStream：
    //   • PrintStream  — 字节流，传统，System.out 使用
    //   • PrintWriter  — 字符流，推荐用于文件/网络输出
    //
    // 阅读：java.io.PrintStream → println() → synchronized(this) → newLine()
    //       java.lang.System → initPhase1() → setOut0(newPrintStream(...))
    // ──────────────────────────────────────────────────────────────────
    private fun demo15PrintStream() {
        Lab.section("1.5", "PrintStream & System.out")

        // System.out 本身就是 PrintStream
        println("System.out class : ${System.out.javaClass.name}")

        // PrintWriter → StringWriter：字符流，推荐用于非控制台场景
        println("--- PrintWriter to StringWriter ---")
        val sw = StringWriter()
        PrintWriter(sw).use { pw ->
            pw.println("Hello from PrintWriter")
            pw.printf("Formatted: %d + %d = %d%n", 6, 7, 13)
        }
        println("Captured output:\n${sw.toString().trimEnd()}")

        // PrintStream → ByteArrayOutputStream：捕获字节输出
        println("--- PrintStream to ByteArrayOutputStream ---")
        val baos = ByteArrayOutputStream()
        PrintStream(baos, /* autoFlush = */ true, Charsets.UTF_8).use { ps ->
            ps.println("Captured via PrintStream")
            ps.print("No newline here")
        }
        println("Captured bytes decoded: \"${baos.toString(Charsets.UTF_8)}\"")
    }
}
