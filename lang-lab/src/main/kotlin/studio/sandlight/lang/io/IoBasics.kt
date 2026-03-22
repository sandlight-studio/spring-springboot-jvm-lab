package studio.sandlight.lang.io

// ══════════════════════════════════════════════════════════════════
// Java / Kotlin I/O — Four Levels
//
// Java I/O 演进历史：
//
//   ┌─────────────────────────────────────────────────────────────┐
//   │  LEVEL 4: Classpath Resources / Serialization / Kotlin ext  │
//   │    getResourceAsStream / Properties / ObjectOutputStream    │
//   │    kotlin.io extensions (readText / forEachLine / useLines) │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 3: java.nio (Channels & Buffers)                    │
//   │    FileChannel / ByteBuffer / MappedByteBuffer              │
//   │    transferTo (zero-copy) / direct vs heap buffer           │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 2: java.nio.file (NIO.2, Java 7+)                  │
//   │    Path / Files.readString / writeString / walk             │
//   │    StandardOpenOption / BasicFileAttributes                 │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 1: java.io (Classic, Java 1.0+)                    │
//   │    InputStream / OutputStream / Reader / Writer             │
//   │    Decorator pattern: FileInputStream → Buffered → Data    │
//   └─────────────────────────────────────────────────────────────┘
//
// 核心 JDK 入口：
//   java.io.InputStream          → read() abstract method
//   java.io.FilterInputStream    → Decorator pattern base class
//   java.nio.file.Files          → static utility (Java 7+)
//   java.nio.ByteBuffer          → flip() / clear() / compact()
//   java.nio.channels.FileChannel → transferTo() / map()
// ══════════════════════════════════════════════════════════════════

object IoBasics {

    fun run() {
        StreamsAndReaders.run()
        NioFiles.run()
        ChannelsAndBuffers.run()
        ResourcesAndExtensions.run()
    }
}
