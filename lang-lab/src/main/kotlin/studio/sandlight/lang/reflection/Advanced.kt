package studio.sandlight.lang.reflection

import studio.sandlight.lang.support.Lab
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.system.measureNanoTime
import kotlin.time.measureTimedValue

// LEVEL 3: Advanced — 动态代理、性能、安全、高级模式
// (support classes live in AdvancedSupport.kt)

object Advanced {

    fun run() {

        demo31DynamicProxies()
        demo32AdvancedAnnotations()
        demo33PerformanceConsiderations()
        demo34SecurityAspects()
        demo35AdvancedPatterns()
    }

    // ──────────────────────────────────────────────────────────────
    // 3.1 动态代理
    //
    // Proxy.newProxyInstance() 原理：
    //
    //   Proxy.newProxyInstance(loader, interfaces, handler)
    //       │
    //       └── ProxyGenerator.generateProxyClass()  ← 运行时生成字节码
    //               │  生成的类（如 $Proxy0）实现所有指定接口
    //               │  每个方法体 = handler.invoke(this, method, args)
    //               │
    //   caller → $Proxy0.doWork("x")
    //                 │
    //                 └── handler.invoke(proxy, method, args)
    //                           │
    //                           └── method.invoke(realTarget, args)
    //
    // 关键限制：JDK 动态代理只能代理「接口」，无法代理具体类。
    //   原因：生成的代理类已经 extends Proxy，Java 不支持多继承。
    //   代理具体类需要 CGLIB（字节码子类化）或 ByteBuddy。
    //
    // 阅读 JDK 源码建议：
    //   java.base/java/lang/reflect/Proxy.java     → newProxyInstance() / ProxyBuilder
    //   java.base/java/lang/reflect/WeakCache.java ← 代理类缓存机制
    // ──────────────────────────────────────────────────────────────
    private fun demo31DynamicProxies() {
        Lab.section("3.1", "Dynamic Proxies")

        val handler = object : InvocationHandler {
            private val target = ServiceImpl()

            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                println("Before method: ${method?.name}")

                // measureTimedValue gives both the result and the duration.
                val (result, duration) = measureTimedValue {
                    try {
                        method?.invoke(target, *(args ?: emptyArray()))
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                }

                println("After method: ${method?.name} (took ${duration.inWholeNanoseconds}ns)")
                return result
            }
        }

        val proxy = Proxy.newProxyInstance(
            Service::class.java.classLoader,
            arrayOf(Service::class.java),
            handler
        ) as Service

        println("Proxy class: ${proxy.javaClass.name}")
        println("Is proxy: ${Proxy.isProxyClass(proxy.javaClass)}")
        println("Invocation handler: ${Proxy.getInvocationHandler(proxy)}")

        println("\nUsing proxy:")
        proxy.doWork("test data")
        val result = proxy.calculate(10, 5)
        println("Calculation result: $result")

        // 多接口代理
        val multiHandler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                return when (method?.name) {
                    "run"  -> { println("Runnable.run() called"); Unit }
                    "call" -> { println("Callable.call() called"); "proxy result" }
                    else   -> throw UnsupportedOperationException("Unknown method: ${method?.name}")
                }
            }
        }

        val multiProxy = Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(Runnable::class.java, java.util.concurrent.Callable::class.java),
            multiHandler
        )

        println("\nMulti-interface proxy:")
        (multiProxy as Runnable).run()
        val callResult = (multiProxy as java.util.concurrent.Callable<*>).call()
        println("Callable result: $callResult")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.2 高级注解处理
    //
    // @Inherited：只对「类注解」生效，方法/字段注解不会被继承。
    //   子类通过 getAnnotation() 可获取父类的 @Inherited 注解。
    //
    // @Repeatable：允许在同一位置使用多个同类型注解。
    //   编译器将多个 @Tag 包装为 @Tags（容器注解）。
    //   通过 getAnnotationsByType(Tag.class) 可展开获取。
    //
    // 阅读：java.base/java/lang/annotation/Inherited.java
    //       java.base/java/lang/annotation/Repeatable.java
    // ──────────────────────────────────────────────────────────────
    private fun demo32AdvancedAnnotations() {
        Lab.section("3.2", "Advanced Annotation Processing")

        val processor = AnnotationProcessor()
        val instance  = AdvancedAnnotatedClass()
        processor.process(instance)

        val childClass       = ChildAnnotatedClass::class.java
        val parentAnnotation = childClass.getAnnotation(Cacheable::class.java)
        println("\nAnnotation inheritance:")
        println("Child has @Cacheable: ${parentAnnotation != null}")

        val method         = childClass.getMethod("cachedMethod")
        val methodCacheable = method.getAnnotation(Cacheable::class.java)
        println("Inherited method has @Cacheable: ${methodCacheable != null}")
        if (methodCacheable != null) {
            println("Cache timeout: ${methodCacheable.timeout}")
        }

        val auditedMethod = childClass.getMethod("auditedOperation")
        val audited       = auditedMethod.getAnnotation(Audited::class.java)
        if (audited != null) {
            println("\nMeta-annotation example:")
            println("Audited level: ${audited.level}")
            val auditedClass = audited.annotationClass.java
            auditedClass.annotations.forEach { metaAnnotation ->
                println("Meta-annotation on @Audited: $metaAnnotation")
            }
        }

        val repeatedMethod = childClass.getMethod("multiTaggedMethod")
        val tags           = repeatedMethod.getAnnotationsByType(Tag::class.java)
        println("\nRepeatable annotations:")
        tags.forEach { tag -> println("Tag: ${tag.value}") }
    }

    // ──────────────────────────────────────────────────────────────
    // 3.3 性能考量
    //
    // 反射调用的开销来源：
    //   1. 方法查找（getMethod）：遍历类的 method 数组，O(n)
    //   2. 访问检查：每次 invoke 都检查权限
    //   3. 装箱/拆箱：原始类型参数被包装为 Object
    //   4. 前 15 次调用走解释模式，第 16 次 JIT 生成专用 accessor
    //
    // MethodHandle（java.lang.invoke）优势：
    //   - 编译期类型安全（invokeExact）
    //   - JIT 可内联，接近直接调用性能
    //   - 适合高频场景
    //
    // 阅读：java.base/java/lang/invoke/MethodHandles.java
    //       java.base/java/lang/invoke/MethodHandle.java
    // ──────────────────────────────────────────────────────────────
    private fun demo33PerformanceConsiderations() {
        Lab.section("3.3", "Performance Considerations")

        val testClass  = PerformanceTest::class.java
        val instance   = testClass.getDeclaredConstructor().newInstance()
        val iterations = 100_000

        println("Method lookup performance ($iterations iterations):")

        val cachedMethod = testClass.getMethod("fastMethod", String::class.java)
        val duration1 = measureNanoTime {
            repeat(iterations) { cachedMethod.invoke(instance, "test") }
        }
        println("Cached method: ${duration1 / 1_000_000}ms")

        val duration2 = measureNanoTime {
            repeat(iterations) {
                val method = testClass.getMethod("fastMethod", String::class.java)
                method.invoke(instance, "test")
            }
        }
        println("Repeated lookup: ${duration2 / 1_000_000}ms")
        println("Performance difference: ${duration2 / duration1.toDouble()}x slower")

        val methodHandles = java.lang.invoke.MethodHandles.lookup()
        val methodHandle  = methodHandles.findVirtual(
            testClass,
            "fastMethod",
            java.lang.invoke.MethodType.methodType(String::class.java, String::class.java)
        )

        val duration3 = measureNanoTime {
            repeat(iterations) { methodHandle.invoke(instance, "test") }
        }
        println("MethodHandle: ${duration3 / 1_000_000}ms")
        println("MethodHandle vs Reflection: ${duration1 / duration3.toDouble()}x faster")

        val field  = testClass.getDeclaredField("data")
        field.isAccessible = true
        val duration4 = measureNanoTime {
            repeat(iterations) { field.set(instance, "new value"); field.get(instance) }
        }
        println("Field access: ${duration4 / 1_000_000}ms")

        println("\nSecurity considerations:")
        println("Note: SecurityManager was deprecated in Java 17 and removed in Java 18+")
        println("Modern JVMs use module system and JVM flags for security control")
        try {
            @Suppress("DEPRECATION")
            val securityManager = System.getSecurityManager()
            println("Security manager present: ${securityManager != null}")
        } catch (e: Exception) {
            println("SecurityManager API not available: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3.4 安全考量
    //
    // 现代 JVM 访问控制体系（Java 9+）：
    //
    //   模块系统（JPMS）
    //   ├── opens 声明 → 允许深度反射（setAccessible）
    //   ├── exports 声明 → 允许编译时访问，不允许深度反射
    //   └── 未声明 → 完全封闭
    //
    //   强封装 JEP：
    //     JEP 396 (Java 16) → 默认拒绝非公共 JDK API 的强访问
    //     JEP 403 (Java 17) → 无法用 --illegal-access 绕过
    //
    // 阅读：java.base/java/lang/Module.java → isOpen() / addOpens()
    // ──────────────────────────────────────────────────────────────
    private fun demo34SecurityAspects() {
        Lab.section("3.4", "Security Aspects")

        val secureClass = SecureClass::class.java

        try {
            val secretField = secureClass.getDeclaredField("secretKey")
            println("Found private field: ${secretField.name}")
            println("Field is accessible: ${secretField.isAccessible}")

            secretField.isAccessible = true
            println("Made field accessible: ${secretField.isAccessible}")

            val instance    = secureClass.getDeclaredConstructor().newInstance()
            val secretValue = secretField.get(instance)
            println("Secret value accessed: $secretValue")

            println("\n⚠️  Security implications:")
            println("- Reflection can bypass encapsulation")
            println("- Private members become accessible")
            println("- Modern JVMs use module system for access control")
            println("- Use --illegal-access=deny to restrict reflection access")
            println("- Use with caution in production")

        } catch (e: SecurityException) {
            println("Security manager prevented access: ${e.message}")
        } catch (e: Exception) {
            println("Access failed: ${e.message}")
        }

        println("\n✅ Safer reflection patterns:")
        try {
            val safeAccess = SafeReflectionUtils.getFieldValue(
                SecureClass::class.java.getDeclaredConstructor().newInstance(),
                "publicData"
            )
            println("Safe field access: $safeAccess")
        } catch (e: Exception) {
            println("Safe access failed appropriately: ${e.message}")
        }

        try {
            SafeReflectionUtils.getFieldValue(
                SecureClass::class.java.getDeclaredConstructor().newInstance(),
                "secretKey"
            )
        } catch (e: Exception) {
            println("Private field access properly blocked: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3.5 高级反射模式
    //
    // 下面展示三种常见的反射应用场景：
    //   ReflectionBuilder  → 建造者模式（无需手写 builder）
    //   SimpleDIContainer  → 依赖注入（@Inject 字段注入）
    //   SimpleObjectMapper → Map → POJO 映射
    //   ReflectionToStringBuilder → 自动生成 toString
    // ──────────────────────────────────────────────────────────────
    private fun demo35AdvancedPatterns() {
        Lab.section("3.5", "Advanced Reflection Patterns")

        val builder = ReflectionBuilder(ReflectionPerson::class.java)
        val person  = builder
            .set("name", "John Doe")
            .set("age", 30)
            .set("email", "john@example.com")
            .build()
        println("Builder pattern result: $person")

        val container = SimpleDIContainer()
        container.register(DatabaseService::class.java) { DatabaseServiceImpl() }
        container.register(UserService::class.java) { UserServiceImpl() }
        val userService = container.get(UserService::class.java)
        userService.createUser("Alice")

        val mapper   = SimpleObjectMapper()
        val userData = mapOf("name" to "Bob", "age" to 25, "email" to "bob@example.com")
        val mappedPerson = mapper.map(userData, ReflectionPerson::class.java)
        println("Object mapper result: $mappedPerson")

        val toStringBuilder = ReflectionToStringBuilder()
        val complexObject   = ComplexObject()
        println("Reflection toString: ${toStringBuilder.toString(complexObject)}")
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 3 支撑类
// ══════════════════════════════════════════════════════════════════
