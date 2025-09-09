package studio.sandlight.lang

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object IoBasics {
    fun run() {
        println("-- IO basics --")

        // Read a classpath resource
        val resourceText = object {}.javaClass.getResourceAsStream("/sample.txt")
            ?.bufferedReader()?.use { it.readText() }
            ?: "<resource missing>"
        println("resource sample.txt: ${resourceText.lines().firstOrNull()} ...")

        // Temp directory and file
        val tmpDir: Path = Files.createTempDirectory("lang-lab-")
        val file = tmpDir.resolve("hello.txt")
        Files.writeString(file, "Hello, file!\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        Files.write(file, listOf("Line1", "Line2"), StandardOpenOption.APPEND)

        // Read lines and print
        Files.newBufferedReader(file).use { br ->
            println("file contents:")
            br.lines().forEach { println("  $it") }
        }

        // Cleanup
        Files.deleteIfExists(file)
        Files.deleteIfExists(tmpDir)
        println("Temp files cleaned up at $tmpDir")
    }
}

