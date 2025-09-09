package studio.sandlight.jvm

fun classLoaderDemo() {
    println("ClassLoader hierarchy for this class:")
    var cl: ClassLoader? = ClassLoaderDemo::class.java.classLoader
    var level = 0
    while (cl != null) {
        println("  #$level -> ${cl.javaClass.name}")
        cl = cl.parent
        level++
    }
    println("  #$level -> <bootstrap>")

    println()
    println("Loaders of some classes:")
    println("  String.class -> ${String::class.java.classLoader ?: "<bootstrap>"}")
    println("  this.class   -> ${ClassLoaderDemo::class.java.classLoader}")
    println("  List.class   -> ${java.util.ArrayList::class.java.classLoader ?: "<bootstrap>"}")

    val cp = System.getProperty("java.class.path")
    println("\nSystem classpath entries: \n" + cp.split(System.getProperty("path.separator")).joinToString("\n") { "  - $it" })
}

private class ClassLoaderDemo

