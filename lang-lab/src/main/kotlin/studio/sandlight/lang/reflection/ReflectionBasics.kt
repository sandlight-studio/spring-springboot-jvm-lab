package studio.sandlight.lang.reflection

// ┌─────────────────────────────────────────────────────────────────┐
// │                   Java Reflection API 总览                       │
// │                                                                 │
// │  java.lang                                                      │
// │    Class<T>  ←── JVM 中每个类型有且只有一个 Class 对象           │
// │      │                                                          │
// │      ├── getDeclaredFields()       → Field[]                    │
// │      ├── getDeclaredMethods()      → Method[]                   │
// │      ├── getDeclaredConstructors() → Constructor[]              │
// │      └── getAnnotations()         → Annotation[]                │
// │                                                                 │
// │  java.lang.reflect                                              │
// │    AccessibleObject                                             │
// │      ├── Field        ← 字段元数据 + get/set                    │
// │      ├── Method       ← 方法元数据 + invoke                     │
// │      └── Constructor  ← 构造器元数据 + newInstance              │
// │    ParameterizedType  ← List<String> 的运行时类型表示           │
// │    TypeVariable       ← class Foo<T> 中的 T                     │
// │    Proxy              ← 运行时生成接口的代理类                   │
// │    InvocationHandler  ← 代理调用拦截逻辑                        │
// │                                                                 │
// │  JDK 源码入口: java.base/java/lang/Class.java                   │
// └─────────────────────────────────────────────────────────────────┘

object ReflectionBasics {

    fun run(args: Array<String>) {
        if (args.size < 2) {
            printReflectionUsage()
            return
        }

        when (args[1].lowercase()) {
            "basic", "foundation", "1" -> Foundation.run()
            "intermediate", "2"        -> Intermediate.run()
            "advanced", "3"            -> Advanced.run()
            "expert", "4"              -> Expert.run()
            "all"                      -> runAllLevels()
            else -> {
                println("Unknown reflection level: ${args[1]}\n")
                printReflectionUsage()
            }
        }
    }

    private fun printReflectionUsage() {
        println("""
        |Java Reflection Learning Levels:
        |  basic/1         - Foundation: Class objects, fields, methods, constructors
        |  intermediate/2  - Generics, annotations, arrays, nested classes
        |  advanced/3      - Dynamic proxies, performance, security
        |  expert/4        - Custom classloaders, framework patterns
        |  all             - Run all levels sequentially
        |
        |Examples:
        |  ./gradlew :lang-lab:run --args="reflection basic" --quiet
        |  ./gradlew :lang-lab:run --args="reflection intermediate" --quiet
        |  ./gradlew :lang-lab:run --args="reflection all" --quiet
        """.trimMargin())
    }

    private fun runAllLevels() {
        Foundation.run()
        println("\n" + "=".repeat(60) + "\n")
        Intermediate.run()
        println("\n" + "=".repeat(60) + "\n")
        Advanced.run()
        println("\n" + "=".repeat(60) + "\n")
        Expert.run()
    }
}
