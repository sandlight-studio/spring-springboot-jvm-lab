package studio.sandlight.lang.reflection

import studio.sandlight.lang.support.Level
import studio.sandlight.lang.support.Topic

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

object ReflectionBasics : Topic {
    override val name = "reflection"
    override val description = "Java reflection from basic to expert level"
    override val levels = listOf(
        Level(1, "foundation", "FOUNDATION - Class Objects, Fields, Methods, Constructors", Foundation::run, aliases = listOf("basic")),
        Level(2, "intermediate", "INTERMEDIATE - Generics, Annotations, Arrays, Nested Classes", Intermediate::run),
        Level(3, "advanced", "ADVANCED - Dynamic Proxies, Performance, Security", Advanced::run),
        Level(4, "expert", "EXPERT - ClassLoaders, Frameworks, Real-world Patterns", Expert::run),
    )
}
