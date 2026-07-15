// LEVEL 2: NIO.2 Files — java.nio.file 现代文件 API（Java 7+）
package studio.sandlight.lang.io

import studio.sandlight.lang.support.Lab

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.nio.file.attribute.BasicFileAttributes

object NioFiles {

    fun run() {
        demo21PathOperations()
        demo22FilesReadWrite()
        demo23FileMetadata()
        demo24FilesWalk()
        demo25FileOperations()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.1 Path Operations
    //
    // Path — 文件路径的抽象表示
    //
    //   Path 只是一个路径字符串的包装器（OS 感知），不代表文件存在
    //
    //   创建方式：
    //     Path.of("/tmp/foo/bar.txt")    ← Java 11+（推荐）
    //     Paths.get("/tmp/foo/bar.txt")  ← Java 7+（旧写法）
    //     Path.of("relative", "path")   ← 可变参数拼接
    //
    //   常用操作：
    //     path.resolve("child")      → 追加子路径
    //     path.resolveSibling("b")   → 同级路径
    //     path.relativize(other)     → 计算相对路径
    //     path.normalize()           → 消除 . 和 ..
    //     path.toAbsolutePath()      → 转为绝对路径
    //     path.parent / fileName / root → 路径组件
    //
    //   Path IS OS-specific:
    //     Unix:    UnixPath    → separator '/'
    //     Windows: WindowsPath → separator '\'
    //
    // 阅读：java.nio.file.Path → OS 相关实现（UnixPath / WindowsPath）
    //       java.nio.file.Paths.get() → FileSystems.getDefault().getPath()
    // ─────────────────────────────────────────────────────────────────────
    private fun demo21PathOperations() {
        Lab.section("2.1", "Path Operations")

        // Path.of with varargs — segments joined with OS separator
        val base = Path.of("/tmp", "nio-lab", "data")
        println("base:              $base")

        // resolve: append child segment
        val file = base.resolve("report.txt")
        println("resolve:           $file")

        // resolveSibling: replace last segment with sibling
        val sibling = file.resolveSibling("summary.txt")
        println("resolveSibling:    $sibling")

        // relativize: compute relative path from base to target
        val other = Path.of("/tmp", "nio-lab", "archive", "old.txt")
        val rel = base.relativize(other)
        println("relativize:        $rel")   // → ../archive/old.txt

        // normalize: collapse . and .. segments
        val messy = Path.of("/tmp/nio-lab/../nio-lab/./data/report.txt")
        println("normalize:         ${messy.normalize()}")

        // Path components
        println("parent:            ${file.parent}")
        println("fileName:          ${file.fileName}")
        println("root:              ${file.root}")
        println("nameCount:         ${file.nameCount}")

        // Iterate over path name elements (excludes root)
        print("components:        ")
        for (i in 0 until file.nameCount) print("[${file.getName(i)}] ")
        println()
        println()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.2 Files: Read and Write
    //
    // Files — 静态工具类（Java 7+），所有文件操作的首选
    //
    //   读取：
    //     Files.readString(path)          → 一次性读全部（Java 11+）
    //     Files.readAllLines(path)        → List<String>
    //     Files.newBufferedReader(path)   → 流式读取（大文件）
    //     Files.lines(path)              → Stream<String>（惰性，必须 close）
    //
    //   写入：
    //     Files.writeString(path, str, opts)     → Java 11+
    //     Files.write(path, lines, charset, opts) → List<String>
    //     Files.newBufferedWriter(path, opts)    → 流式写入
    //
    //   StandardOpenOption：
    //   ┌────────────────────────────────────────────────────────────┐
    //   │  CREATE              → 文件不存在则创建，存在则保留       │
    //   │  CREATE_NEW          → 文件存在则抛 FileAlreadyExists     │
    //   │  TRUNCATE_EXISTING   → 打开时截断（清空）                 │
    //   │  APPEND              → 追加到文件末尾                     │
    //   │  SYNC                → 每次 write 同步到磁盘（含元数据）  │
    //   │  DSYNC               → 每次 write 同步数据（不含元数据）  │
    //   └────────────────────────────────────────────────────────────┘
    //
    // 阅读：java.nio.file.Files → writeString() → newBufferedWriter()
    //       → Channels.newWriter() → FileChannel + Charset
    // ─────────────────────────────────────────────────────────────────────
    private fun demo22FilesReadWrite() {
        Lab.section("2.2", "Files: Read and Write")

        val tmp = Files.createTempFile("nio-demo", ".txt")
        try {
            // Write with CREATE + TRUNCATE_EXISTING (overwrite if exists)
            Files.writeString(tmp, "Hello, NIO.2!\nLine two.\n", CREATE, TRUNCATE_EXISTING)
            println("wrote initial content to: $tmp")

            // Append additional lines
            Files.writeString(tmp, "Appended line.\n", APPEND)
            println("appended a line")

            // readString — entire file as one String (Java 11+)
            val full = Files.readString(tmp)
            println("readString:\n$full")

            // readAllLines — List<String>, each element is one line
            val lines = Files.readAllLines(tmp)
            println("readAllLines (${lines.size} lines): $lines")

            // Files.lines — lazy Stream<String>, MUST be closed
            Files.lines(tmp).use { stream ->
                val upperLines = stream.map { it.uppercase() }.toList()
                println("lines() uppercased: $upperLines")
            }
        } finally {
            Files.deleteIfExists(tmp)
        }
        println()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.3 File Metadata
    //
    // BasicFileAttributes — 统一的文件属性接口
    //
    //   Files.readAttributes(path, BasicFileAttributes::class.java)
    //     → size(), isRegularFile(), isDirectory(), isSymbolicLink()
    //     → creationTime(), lastModifiedTime(), lastAccessTime()
    //
    //   快捷方法（无需读属性对象）：
    //     Files.exists(path)
    //     Files.size(path)
    //     Files.isRegularFile(path)
    //     Files.isDirectory(path)
    //     Files.getLastModifiedTime(path)
    //
    //   PosixFileAttributes (Unix only):
    //     permissions(), owner(), group()
    //
    // 阅读：java.nio.file.Files → readAttributes() → provider().readAttributes()
    //       sun.nio.fs.UnixFileAttributeViews → stat() syscall
    // ─────────────────────────────────────────────────────────────────────
    private fun demo23FileMetadata() {
        Lab.section("2.3", "File Metadata")

        val tmp = Files.createTempFile("nio-meta", ".txt")
        try {
            Files.writeString(tmp, "metadata demo content\n", CREATE, TRUNCATE_EXISTING)

            // Read all basic attributes in one syscall
            val attrs = Files.readAttributes(tmp, BasicFileAttributes::class.java)
            println("size:             ${attrs.size()} bytes")
            println("isRegularFile:    ${attrs.isRegularFile}")
            println("isDirectory:      ${attrs.isDirectory}")
            println("isSymbolicLink:   ${attrs.isSymbolicLink}")
            println("creationTime:     ${attrs.creationTime()}")
            println("lastModifiedTime: ${attrs.lastModifiedTime()}")
            println("lastAccessTime:   ${attrs.lastAccessTime()}")

            // Convenience shortcuts — no attribute object needed
            println("Files.exists:     ${Files.exists(tmp)}")
            println("Files.size:       ${Files.size(tmp)} bytes")
            println("Files.isRegFile:  ${Files.isRegularFile(tmp)}")
            println("Files.lastMod:    ${Files.getLastModifiedTime(tmp)}")
        } finally {
            Files.deleteIfExists(tmp)
        }
        println()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.4 Files.walk
    //
    // Files.walk — 递归遍历目录树
    //
    //   Files.walk(start, maxDepth?, options...)
    //     → Stream<Path>（惰性，深度优先，包含起点本身）
    //     → 必须 close()！用 .use { } 或 try-with-resources
    //
    //   Files.find(start, maxDepth, BiPredicate<Path, BasicFileAttributes>)
    //     → 带属性的过滤遍历（比 walk + filter 更高效）
    //
    //   Files.list(dir)
    //     → Stream<Path>，只列一层（非递归）
    //     → 同样必须 close()
    //
    //   walk 遍历顺序：
    //   ┌─ root/
    //   │  ├─ a.txt      ← 先遍历文件
    //   │  └─ subdir/    ← 然后进入子目录
    //   │     └─ b.txt
    //   访问顺序：root/ → a.txt → subdir/ → b.txt
    //
    // 阅读：java.nio.file.Files → walk() → FileTreeIterator
    //       → FileTreeWalker.visit() — 底层用 DirectoryStream
    // ─────────────────────────────────────────────────────────────────────
    private fun demo24FilesWalk() {
        Lab.section("2.4", "Files.walk")

        val root = Files.createTempDirectory("nio-walk")
        try {
            // Build a small two-level tree
            val subA = root.resolve("subA")
            val subB = root.resolve("subB")
            Files.createDirectories(subA)
            Files.createDirectories(subB)
            Files.writeString(root.resolve("top.txt"), "top", CREATE)
            Files.writeString(subA.resolve("a1.txt"), "a1", CREATE)
            Files.writeString(subA.resolve("a2.log"), "a2", CREATE)
            Files.writeString(subB.resolve("b1.txt"), "b1", CREATE)

            // walk — depth-first, includes root itself, MUST close stream
            println("All paths (walk):")
            Files.walk(root).use { stream ->
                stream.forEach { println("  $it") }
            }

            // Filter: only .txt files
            val txtFiles = Files.walk(root).use { stream ->
                stream.filter { it.toString().endsWith(".txt") }.toList()
            }
            println("txt files only: $txtFiles")

            // Count regular files (exclude directories)
            val fileCount = Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) }.count()
            }
            println("total regular files: $fileCount")

            // Files.list — one level only, no recursion
            println("top-level entries (list):")
            Files.list(root).use { stream ->
                stream.forEach { println("  $it") }
            }
        } finally {
            // Delete deepest entries first (reverse sort ensures children before parents)
            Files.walk(root).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
        println()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.5 File Operations: copy, move, delete
    //
    // Files 文件操作
    //
    //   Files.copy(src, dst, options)
    //     → CopyOption: REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS
    //     → 不递归！目录 copy 只复制目录本身，不含内容
    //
    //   Files.move(src, dst, options)
    //     → ATOMIC_MOVE（原子移动，不支持跨文件系统）
    //     → 重命名时 ATOMIC_MOVE 无需复制，仅修改目录项
    //
    //   Files.delete(path)   → 不存在则抛异常
    //   Files.deleteIfExists → 不存在返回 false（更安全）
    //
    //   Files.createDirectories(path) → 创建多级目录（等价于 mkdir -p）
    //   Files.createTempFile(dir, prefix, suffix)
    //   Files.createTempDirectory(dir, prefix)
    //
    // 阅读：java.nio.file.Files → copy() → provider().copy()
    //       sun.nio.fs.UnixCopyFile → copyFile() — sendfile() or read/write loop
    // ─────────────────────────────────────────────────────────────────────
    private fun demo25FileOperations() {
        Lab.section("2.5", "File Operations: copy, move, delete")

        val tmpDir = Files.createTempDirectory("nio-ops")
        try {
            // Source file
            val src = tmpDir.resolve("source.txt")
            Files.writeString(src, "copy-move demo content\n", CREATE)
            println("created source: $src")

            // copy with REPLACE_EXISTING — overwrites dst if it exists
            val dst = tmpDir.resolve("dest.txt")
            Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            println("copied to: $dst")

            // Verify copy
            val copiedContent = Files.readString(dst)
            println("dest content: ${copiedContent.trim()}")

            // move (rename) within same filesystem — typically atomic
            val moved = tmpDir.resolve("renamed.txt")
            Files.move(dst, moved, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            println("moved to: $moved")
            println("dst exists after move: ${Files.exists(dst)}")
            println("moved exists:          ${Files.exists(moved)}")

            // createDirectories — mkdir -p equivalent (no error if exists)
            val nested = tmpDir.resolve("level1/level2/level3")
            Files.createDirectories(nested)
            println("created nested dirs: $nested")
            println("isDirectory: ${Files.isDirectory(nested)}")

            // deleteIfExists — returns false instead of throwing when missing
            val gone = tmpDir.resolve("nonexistent.txt")
            val deleted = Files.deleteIfExists(gone)
            println("deleteIfExists(nonexistent): $deleted")
        } finally {
            // Recursive cleanup
            Files.walk(tmpDir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
        println()
    }
}
