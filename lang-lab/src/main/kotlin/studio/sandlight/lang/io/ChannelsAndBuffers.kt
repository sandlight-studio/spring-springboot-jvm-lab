// LEVEL 3: Channels & Buffers — java.nio 非阻塞 I/O 核心

package studio.sandlight.lang.io

import studio.sandlight.lang.support.Lab

import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import kotlin.system.measureNanoTime

object ChannelsAndBuffers {

    fun run() {
        demo31ByteBufferFundamentals()
        demo32FileChannel()
        demo33ChannelTransfer()
        demo34MemoryMappedFiles()
        demo35DirectVsHeapBuffers()
    }

    // ─── 3.1 ByteBuffer Fundamentals ────────────────────────────────────────

    // ByteBuffer — NIO 的核心数据容器
    //
    //   三个关键指针：
    //   ┌────────────────────────────────────────────────────────────┐
    //   │  capacity  总容量（固定）                                   │
    //   │  limit     可读/写的边界（<= capacity）                    │
    //   │  position  当前读写位置（<= limit）                        │
    //   └────────────────────────────────────────────────────────────┘
    //
    //   allocate(8) 后：
    //   [_, _, _, _, _, _, _, _]
    //    ^                       ^
    //  pos=0                  lim=cap=8
    //
    //   put("ABC"):
    //   [A, B, C, _, _, _, _, _]
    //             ^             ^
    //           pos=3         lim=8
    //
    //   flip()  → 准备读取（limit=position, position=0）：
    //   [A, B, C, _, _, _, _, _]
    //    ^       ^
    //  pos=0   lim=3
    //
    //   get() × 3 → 读完：
    //   [A, B, C, _, _, _, _, _]
    //             ^
    //          pos=3=lim   (hasRemaining() = false)
    //
    //   rewind() → 重新从头读（position=0，limit 不变）：
    //   [A, B, C, _, _, _, _, _]
    //    ^       ^
    //  pos=0   lim=3
    //
    //   clear() → 准备重新写（position=0，limit=capacity）：
    //   [_, _, _, _, _, _, _, _]
    //    ^                     ^
    //  pos=0              lim=cap=8
    //
    //   compact() → 保留未读数据，继续写（适合网络读取的半包场景）：
    //   如果已读到 pos=2，剩余 [C]，compact 后：
    //   [C, _, _, _, _, _, _, _]
    //       ^                  ^
    //     pos=1             lim=cap=8
    //
    // 阅读：java.nio.Buffer → flip() / clear() / compact()
    //       java.nio.ByteBuffer → get() / put() / slice() / duplicate()
    private fun demo31ByteBufferFundamentals() {
        Lab.section("3.1", "ByteBuffer Fundamentals")

        // allocate: position=0, limit=capacity=8
        val buf = ByteBuffer.allocate(8)
        println("After allocate(8):  pos=${buf.position()} lim=${buf.limit()} cap=${buf.capacity()}")

        // put bytes — "Hello" is 5 bytes; position advances to 5
        val hello = "Hello".toByteArray(StandardCharsets.UTF_8)
        buf.put(hello)
        println("After put(\"Hello\"): pos=${buf.position()} lim=${buf.limit()} cap=${buf.capacity()}")

        // flip: limit=position, position=0 — switch to read mode
        buf.flip()
        println("After flip():       pos=${buf.position()} lim=${buf.limit()} cap=${buf.capacity()}")

        // read all remaining bytes
        val readBytes = ByteArray(buf.remaining())
        buf.get(readBytes)
        println("Read back: \"${String(readBytes, StandardCharsets.UTF_8)}\"")
        println("After get() × 5:    pos=${buf.position()} lim=${buf.limit()} hasRemaining=${buf.hasRemaining()}")

        // rewind: position=0, limit unchanged — re-read from start
        buf.rewind()
        println("After rewind():     pos=${buf.position()} lim=${buf.limit()}")
        val firstByte = buf.get()
        println("First byte after rewind: '${firstByte.toInt().toChar()}'")

        // clear: position=0, limit=capacity — back to write mode
        buf.clear()
        println("After clear():      pos=${buf.position()} lim=${buf.limit()} cap=${buf.capacity()}")

        // compact() demo: write 3 bytes, flip, read 2, then compact
        val buf2 = ByteBuffer.allocate(8)
        buf2.put("ABC".toByteArray(StandardCharsets.UTF_8))
        buf2.flip()
        buf2.get(); buf2.get()                        // read 'A' and 'B'
        println("\nBefore compact():   pos=${buf2.position()} lim=${buf2.limit()} (1 byte remaining: 'C')")
        buf2.compact()                                // 'C' shifted to index 0; pos=1, lim=cap
        println("After compact():    pos=${buf2.position()} lim=${buf2.limit()} cap=${buf2.capacity()}")

        // wrap: create a buffer backed by an existing array (no copy)
        val arr = "World".toByteArray(StandardCharsets.UTF_8)
        val wrapped = ByteBuffer.wrap(arr)
        println("\nwrap(arr):          pos=${wrapped.position()} lim=${wrapped.limit()} isDirect=${wrapped.isDirect}")
        val wrappedStr = ByteArray(wrapped.remaining()).also { wrapped.get(it) }
        println("Wrapped content: \"${String(wrappedStr, StandardCharsets.UTF_8)}\"")
    }

    // ─── 3.2 FileChannel Read/Write ─────────────────────────────────────────

    // FileChannel — 文件的 NIO 通道
    //
    //   FileChannel 与 InputStream 的区别：
    //     InputStream.read(byte[])  → 阻塞，可能读少于 len 字节
    //     FileChannel.read(ByteBuffer) → 同样阻塞，但与 Buffer 配合
    //
    //   FileChannel.read() 返回实际读取字节数（-1 表示 EOF）
    //   因此需要循环读取直到 buffer 满或 EOF
    //
    //   打开方式（无需 FileInputStream）：
    //     FileChannel.open(path, READ)
    //     FileChannel.open(path, WRITE, CREATE, TRUNCATE_EXISTING)
    //
    //   FileChannel 同时支持随机访问：
    //     channel.position(offset)   → 移动到指定位置读/写
    //     channel.size()             → 文件总大小
    //
    // 阅读：java.nio.channels.FileChannel → read(ByteBuffer) → IOUtil.read()
    //       → pread/read syscall
    private fun demo32FileChannel() {
        Lab.section("3.2", "FileChannel Read/Write")

        val tmpFile: Path = Files.createTempFile("nio-channel-", ".txt")
        try {
            // Write via FileChannel
            FileChannel.open(tmpFile, WRITE, CREATE, TRUNCATE_EXISTING).use { wc ->
                val content = "Hello NIO\n"
                val writeBuf = ByteBuffer.wrap(content.toByteArray(StandardCharsets.UTF_8))
                while (writeBuf.hasRemaining()) {
                    wc.write(writeBuf)
                }
                println("Wrote ${wc.size()} bytes; channel position=${wc.position()}")
            }

            // Read via FileChannel
            FileChannel.open(tmpFile, READ).use { rc ->
                println("File size=${rc.size()}")
                val readBuf = ByteBuffer.allocate(64)
                var totalRead = 0
                var bytesRead: Int
                while (rc.read(readBuf).also { bytesRead = it } != -1) {
                    totalRead += bytesRead
                }
                readBuf.flip()
                val result = StandardCharsets.UTF_8.decode(readBuf).toString()
                println("Read back ($totalRead bytes): \"${result.trimEnd()}\"")

                // random-access: position to offset 6 and read "NIO"
                rc.position(6)
                val smallBuf = ByteBuffer.allocate(3)
                rc.read(smallBuf)
                smallBuf.flip()
                println("Random read at offset 6: \"${StandardCharsets.UTF_8.decode(smallBuf)}\"")
            }
        } finally {
            Files.deleteIfExists(tmpFile)
        }
    }

    // ─── 3.3 Channel Transfer (Zero-Copy) ───────────────────────────────────

    // FileChannel.transferTo() — 高效文件复制（零拷贝）
    //
    //   传统复制（4 次内存拷贝）：
    //   磁盘 → 内核缓冲区 → JVM 堆 → 内核缓冲区 → 目标
    //
    //   transferTo() 零拷贝（2 次拷贝，Linux sendfile(2)）：
    //   磁盘 → 内核缓冲区 ────────────────────────────► 目标
    //            (无需经过 JVM 堆！)
    //
    //   ┌──────────────────────────────────────────┐
    //   │  传统 IO：4次拷贝                         │
    //   │  disk → page cache → JVM heap → socket  │
    //   │                                           │
    //   │  transferTo（sendfile）：2次拷贝           │
    //   │  disk → page cache ──────────► socket   │
    //   └──────────────────────────────────────────┘
    //
    //   适合：大文件复制、网络传输静态文件（Nginx/Tomcat 底层用此）
    //
    // 阅读：java.nio.channels.FileChannel → transferTo()
    //       → sun.nio.ch.FileChannelImpl.transferToDirectly() → sendfile()
    private fun demo33ChannelTransfer() {
        Lab.section("3.3", "Channel Transfer (Zero-Copy)")

        val srcFile: Path = Files.createTempFile("nio-src-", ".txt")
        val dstFile: Path = Files.createTempFile("nio-dst-", ".txt")
        try {
            // Write source content
            val srcContent = "Zero-copy transfer via sendfile(2) on Linux."
            Files.write(srcFile, srcContent.toByteArray(StandardCharsets.UTF_8))

            // transferTo: src → dst without JVM heap involvement on Linux
            FileChannel.open(srcFile, READ).use { src ->
                FileChannel.open(dstFile, WRITE, CREATE, TRUNCATE_EXISTING).use { dst ->
                    var remaining = src.size()
                    var position = 0L
                    while (remaining > 0) {
                        val transferred = src.transferTo(position, remaining, dst)
                        position += transferred
                        remaining -= transferred
                    }
                    println("Transferred $position bytes via transferTo()")
                }
            }

            // Verify
            val dstContent = String(Files.readAllBytes(dstFile), StandardCharsets.UTF_8)
            println("Destination content: \"$dstContent\"")
            println("Content matches: ${srcContent == dstContent}")
            println("transferTo: zero-copy on Linux (sendfile), fallback on other OS")
        } finally {
            Files.deleteIfExists(srcFile)
            Files.deleteIfExists(dstFile)
        }
    }

    // ─── 3.4 Memory-Mapped Files ─────────────────────────────────────────────

    // MappedByteBuffer — 内存映射文件
    //
    //   原理：OS 将文件直接映射到进程虚拟内存
    //     → 读写映射区域 = 读写文件，无需 read()/write() 系统调用
    //     → OS 负责页面换入换出（page fault 驱动）
    //
    //   ┌────────────────────────────────────────────────┐
    //   │  进程虚拟地址空间                              │
    //   │  [heap][stack][code]...[mapped region]         │
    //   │                          ↕  page fault         │
    //   │  OS 页缓存 ←→ 磁盘文件                         │
    //   └────────────────────────────────────────────────┘
    //
    //   适合场景：
    //   • 随机访问大文件（数据库、索引文件）
    //   • 进程间共享内存（映射同一文件）
    //   • 只读扫描大文件（避免 OOM，OS 自动换页）
    //
    //   注意：MappedByteBuffer 只能通过 GC 释放（无法手动 unmap）
    //         直到 GC 回收前，文件句柄保持打开
    //
    // 阅读：java.nio.channels.FileChannel → map() → mmap() syscall
    //       java.nio.MappedByteBuffer → force() → msync() syscall
    private fun demo34MemoryMappedFiles() {
        Lab.section("3.4", "Memory-Mapped Files")

        val tmpFile: Path = Files.createTempFile("nio-mmap-", ".txt")
        try {
            // Prepare file with known content
            val initial = "ABCDEFGHIJ"
            Files.write(tmpFile, initial.toByteArray(StandardCharsets.UTF_8))

            // READ_ONLY map: read bytes without read() syscall
            FileChannel.open(tmpFile, READ).use { fc ->
                val mapped: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
                println("Mapped size=${mapped.capacity()} isDirect=${mapped.isDirect}")
                val first3 = ByteArray(3).also { mapped.get(it) }
                println("First 3 bytes (READ_ONLY): \"${String(first3, StandardCharsets.UTF_8)}\"")
            }

            // READ_WRITE map: mutations to the buffer are written back to file
            FileChannel.open(tmpFile, READ, WRITE).use { fc ->
                val mapped: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fc.size())
                // Overwrite first 3 bytes with "XYZ"
                mapped.put(0, 'X'.code.toByte())
                mapped.put(1, 'Y'.code.toByte())
                mapped.put(2, 'Z'.code.toByte())
                mapped.force()          // msync() — flush dirty pages to disk
                println("Wrote 'XYZ' at offset 0 via READ_WRITE map; force() called")
            }

            // Verify the file now starts with XYZ
            val updated = String(Files.readAllBytes(tmpFile), StandardCharsets.UTF_8)
            println("File after READ_WRITE map: \"$updated\"")
        } finally {
            Files.deleteIfExists(tmpFile)
        }
    }

    // ─── 3.5 Direct vs Heap Buffers ─────────────────────────────────────────

    // ByteBuffer.allocate() vs allocateDirect()
    //
    //   Heap Buffer (allocate):
    //   ┌────────────────────────────────────────────────┐
    //   │  JVM 堆内（GC 管理）                           │
    //   │  I/O 时需拷贝到临时 Direct Buffer              │
    //   │  → 额外一次内存拷贝                            │
    //   │  适合：小数据、短生命周期 buffer               │
    //   └────────────────────────────────────────────────┘
    //
    //   Direct Buffer (allocateDirect):
    //   ┌────────────────────────────────────────────────┐
    //   │  JVM 堆外（native memory，不受 GC 管理）        │
    //   │  I/O 时直接传给 OS，无需中间拷贝               │
    //   │  分配慢（malloc + mlock），但 I/O 快            │
    //   │  适合：长期存在的大 buffer（网络/文件 IO 密集）  │
    //   └────────────────────────────────────────────────┘
    //
    //   检测：buffer.isDirect
    //   限制：-XX:MaxDirectMemorySize（默认 = -Xmx）
    //
    // 阅读：java.nio.ByteBuffer → allocateDirect() → DirectByteBuffer
    //       → Unsafe.allocateMemory() → malloc()
    private fun demo35DirectVsHeapBuffers() {
        Lab.section("3.5", "Direct vs Heap Buffers")

        val heapBuf = ByteBuffer.allocate(1024)
        val directBuf = ByteBuffer.allocateDirect(1024)

        println("Heap   buffer isDirect=${heapBuf.isDirect}   capacity=${heapBuf.capacity()}")
        println("Direct buffer isDirect=${directBuf.isDirect} capacity=${directBuf.capacity()}")

        // Benchmark: write 1 MB of data to a temp file using each buffer type
        val oneMB = 1 * 1024 * 1024
        val data = ByteArray(oneMB) { it.toByte() }
        val tmpHeap = Files.createTempFile("nio-heap-", ".bin")
        val tmpDirect = Files.createTempFile("nio-direct-", ".bin")

        try {
            // Heap buffer write
            val heapMs = measureNanoTime {
                FileChannel.open(tmpHeap, WRITE, CREATE, TRUNCATE_EXISTING).use { fc ->
                    val buf = ByteBuffer.wrap(data)      // heap-backed
                    while (buf.hasRemaining()) fc.write(buf)
                }
            } / 1_000_000.0

            // Direct buffer write
            val directMs = measureNanoTime {
                FileChannel.open(tmpDirect, WRITE, CREATE, TRUNCATE_EXISTING).use { fc ->
                    val buf = ByteBuffer.allocateDirect(oneMB)
                    buf.put(data)
                    buf.flip()
                    while (buf.hasRemaining()) fc.write(buf)
                }
            } / 1_000_000.0

            println(String.format("Heap   buffer write 1 MB: %.2f ms", heapMs))
            println(String.format("Direct buffer write 1 MB: %.2f ms (allocation cost included)", directMs))
            println("Note: direct buffer shows benefit for large repeated I/O operations;")
            println("      single-shot allocation overhead may dominate in micro-benchmarks.")
        } finally {
            Files.deleteIfExists(tmpHeap)
            Files.deleteIfExists(tmpDirect)
        }
    }
}
