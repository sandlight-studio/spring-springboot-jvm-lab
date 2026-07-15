// LEVEL 4: Resources, Serialization & Kotlin I/O Extensions

package studio.sandlight.lang.io

import studio.sandlight.lang.support.Lab

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Properties
import java.util.Scanner

// Person is defined at file scope so it can implement Serializable cleanly
private data class Person(
    val name: String,
    val age: Int,
    @Transient val password: String = "secret"
) : Serializable {
    companion object {
        const val serialVersionUID = 1L
    }
}

object ResourcesAndExtensions {

    fun run() {
        demo41ClasspathResources()
        demo42PropertiesFiles()
        demo43Serialization()
        demo44KotlinIoExtensions()
        demo45StdinReading()
    }

    // ClassLoader.getResourceAsStream — 从 classpath 加载资源
    //
    //   类加载器层次（双亲委派）：
    //   ┌─────────────────────────────────────────────────────────┐
    //   │  Bootstrap ClassLoader  → JDK 核心类（rt.jar / modules)│
    //   │         ↑                                               │
    //   │  Extension ClassLoader  → JDK 扩展（Java 8 之前）      │
    //   │         ↑                                               │
    //   │  App ClassLoader        → classpath 上的类和资源        │
    //   └─────────────────────────────────────────────────────────┘
    //
    //   两种加载方式：
    //     MyClass::class.java.getResourceAsStream("/res.txt")
    //       → 路径以 / 开头 = 从 classpath 根部找
    //       → 路径不以 / 开头 = 相对于该类所在的包路径
    //
    //     Thread.currentThread().contextClassLoader
    //       .getResourceAsStream("res.txt")  ← 不加 /，已相对于根
    //       → Servlet 容器等场景推荐，可感知 WebAppClassLoader
    //
    //   资源文件位置：
    //     src/main/resources/sample.txt → classpath 根: /sample.txt
    //
    // 阅读：java.lang.ClassLoader → getResourceAsStream()
    //       → findResource() → 委托父加载器，然后自己找
    private fun demo41ClasspathResources() {
        Lab.section("4.1", "Classpath Resources")

        // Path with leading / = search from classpath root
        val stream = object {}.javaClass.getResourceAsStream("/sample.txt")
        if (stream != null) {
            stream.bufferedReader().use { println("sample.txt: ${it.readLine()}") }
        } else {
            println("sample.txt not found on classpath (expected at src/main/resources/)")
        }

        // Context ClassLoader variant — preferred in container environments
        val url = Thread.currentThread().contextClassLoader.getResource("sample.txt")
        if (url != null) {
            println("Resource URL: $url")
        } else {
            println("Resource URL: null (file not present)")
        }

        // Bonus: list first 5 available charset names
        val charsets = Charset.availableCharsets().keys.take(5)
        println("First 5 charsets: $charsets")
    }

    // java.util.Properties — 键值对配置文件
    //
    //   格式：key=value 或 key: value（# 注释）
    //   支持：从 InputStream / Reader 加载（loadFromXML 也支持）
    //
    //   Properties extends Hashtable<Object, Object>
    //   → getProperty(key) / getProperty(key, default)
    //   → setProperty(key, value) → put(key, value)
    //   → stringPropertyNames() → Set<String>（仅字符串键）
    //
    //   典型用法：
    //     Properties p = new Properties()
    //     p.load(MyClass.class.getResourceAsStream("/app.properties"))
    //     String host = p.getProperty("db.host", "localhost")
    //
    // 阅读：java.util.Properties → load() → load0()
    //       → LineReader 内部类：读取行，处理转义和注释
    private fun demo42PropertiesFiles() {
        Lab.section("4.2", "Properties Files")

        // Load from an in-memory string
        val propsText = """
            # Application configuration
            db.host=localhost
            db.port=5432
            app.name=JvmLab
        """.trimIndent()

        val props = Properties()
        props.load(StringReader(propsText))

        println("db.host  = ${props.getProperty("db.host")}")
        println("db.port  = ${props.getProperty("db.port", "3306")}") // default unused
        println("db.pass  = ${props.getProperty("db.pass", "changeme")}") // default used

        // setProperty and store back to string
        props.setProperty("db.pass", "s3cr3t")
        val out = StringWriter()
        props.store(out, "Updated config")
        println("Stored properties:\n${out.toString().lines().take(5).joinToString("\n")}")
    }

    // Java 序列化 — 对象 ↔ 字节流
    //
    //   class Foo : Serializable {
    //     companion object { const val serialVersionUID = 1L }
    //   }
    //
    //   serialVersionUID 的作用：
    //   • 反序列化时，JVM 比较字节流中的 serialVersionUID 与类定义的是否一致
    //   • 不一致 → InvalidClassException
    //   • 修改类后（加字段）需更新 serialVersionUID，否则旧数据无法读取
    //   • 不声明时 JVM 自动计算（不稳定，不推荐）
    //
    //   @Transient — 标记不参与序列化的字段
    //
    //   序列化字节格式（Java Object Serialization Protocol）：
    //     魔数(AC ED) 版本(00 05) 类描述符 字段值 ...
    //
    //   ⚠️ 生产建议：避免 Java 原生序列化（安全漏洞 + 脆弱）
    //      推荐：JSON（Jackson / Gson）/ Protocol Buffers / Avro
    //
    // 阅读：java.io.ObjectOutputStream → writeObject() → writeOrdinaryObject()
    //       java.io.ObjectStreamClass → serialVersionUID 计算逻辑
    private fun demo43Serialization() {
        Lab.section("4.3", "Java Serialization")

        val original = Person(name = "Alice", age = 30, password = "hunter2")

        // Serialize to byte array
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { oos -> oos.writeObject(original) }
        val bytes = baos.toByteArray()

        // Print magic number — Java serialization stream: AC ED 00 05
        val magic = bytes.take(4).joinToString(" ") { "%02X".format(it) }
        println("Serialized ${bytes.size} bytes, magic: $magic")

        // Deserialize from byte array
        val deserialized = ObjectInputStream(ByteArrayInputStream(bytes)).use { ois ->
            ois.readObject() as Person
        }

        println("Original   : $original")
        println("Deserialized: $deserialized")
        println("Objects equal: ${original.copy(password = "") == deserialized.copy(password = "")}")

        // @Transient field is NOT restored — it gets the default value ""
        println("@Transient password after deserialization: '${deserialized.password}' (expected empty)")
    }

    // Kotlin kotlin.io 扩展 — 对 java.io 的简洁封装
    //
    //   File extensions (kotlin.io.FilesKt):
    //     file.readText(charset)       → file.reader(charset).use { it.readText() }
    //     file.writeText(text)         → file.writer().use { it.write(text) }
    //     file.appendText(text)        → FileWriter(append=true)
    //     file.readLines()             → bufferedReader().lineSequence().toList()
    //     file.forEachLine { }         → 逐行，自动 close
    //     file.useLines { seq -> }     → Sequence<String>，自动 close（惰性）
    //     file.bufferedReader()        → BufferedReader（需手动 close）
    //     file.inputStream()           → FileInputStream
    //
    //   InputStream extensions:
    //     inputStream.bufferedReader() → BufferedReader
    //     inputStream.reader()         → InputStreamReader
    //     inputStream.readBytes()      → ByteArray（一次性读全部）
    //
    //   use { } — 等价于 Java try-with-resources：
    //     Closeable.use { block }
    //     → 无论 block 是否抛异常，finally { close() } 都执行
    //     → 如果 close() 本身抛异常，它会被 suppress（附加到原始异常）
    //
    // 阅读：kotlin/io/FilesKt — readText() / forEachLine()
    //       kotlin/io/Closeable.kt — use() 扩展
    private fun demo44KotlinIoExtensions() {
        Lab.section("4.4", "Kotlin I/O Extensions")

        val tmp = Files.createTempFile("kotlin-io-", ".txt").toFile()
        try {
            // writeText / readText
            tmp.writeText("Hello, Kotlin I/O!\n")
            tmp.appendText("Second line\nThird line\n")
            println("readText:\n${tmp.readText().trimEnd()}")

            // readLines → List<String>
            val lines: List<String> = tmp.readLines()
            println("readLines() count: ${lines.size}, first: '${lines.first()}'")

            // forEachLine — auto-closes reader
            print("forEachLine: ")
            tmp.forEachLine { line -> print("[${line}] ") }
            println()

            // useLines — lazy Sequence, auto-closes
            val upper = tmp.useLines { seq -> seq.map { it.uppercase() }.toList() }
            println("useLines uppercase: $upper")

            // inputStream().readBytes()
            val rawBytes = tmp.inputStream().readBytes()
            println("inputStream readBytes: ${rawBytes.size} bytes")

            // use { } on a custom Closeable to prove finally runs
            val log = StringBuilder()
            val resource = object : Closeable {
                override fun close() { log.append("closed") }
            }
            try {
                resource.use { log.append("used;") ; error("boom") }
            } catch (_: IllegalStateException) { /* swallowed for demo */ }
            println("use {} Closeable log: '$log' (close() ran even after exception)")

        } finally {
            tmp.delete()
        }
    }

    // 从标准输入读取 — CLI 工具的基础
    //
    //   System.in — InputStream（FileInputStream(FileDescriptor.in)）
    //
    //   常用方式：
    //     readLine()               → Kotlin stdlib，读一行（挂起直到输入）
    //     System.in.bufferedReader().readLines() → 读到 EOF
    //     Scanner(System.in)       → Java 方式，支持 nextInt() 等
    //
    //   在非交互式程序中从 stdin 读数据：
    //     echo "hello" | ./program
    //     → System.in 收到 "hello\n" 然后 EOF
    //
    //   注意：System.in.close() 后无法重开，避免在 library 代码中关闭它
    //
    // 阅读：java.lang.System → in (静态字段) → FileInputStream(FileDescriptor.in)
    //       java.io.BufferedInputStream → 包装 System.in 加速读取
    private fun demo45StdinReading() {
        Lab.section("4.5", "stdin Reading")

        // Simulate reading from stdin (redirected input) — does NOT block
        val simulatedInput = "line1\nline2\nline3".byteInputStream()
        simulatedInput.bufferedReader().use { reader ->
            println("Reading simulated stdin:")
            reader.forEachLine { line -> println("  > $line") }
        }

        // Scanner on simulated input — supports typed reads (nextInt etc.)
        val scannerInput = "42 hello 3.14".byteInputStream()
        Scanner(scannerInput).use { scanner ->
            println("Scanner nextInt:    ${scanner.nextInt()}")
            println("Scanner next:       ${scanner.next()}")
            println("Scanner nextDouble: ${scanner.nextDouble()}")
        }

        println("In real usage: pipe input via: echo 'text' | ./program")
    }
}
