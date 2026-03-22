package studio.sandlight.lang.reflection

// LEVEL 3: Advanced — 动态代理、性能、安全、高级模式

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

object Advanced {

    fun run() {
        println("⚡ LEVEL 3: ADVANCED - Dynamic Proxies, Performance, Security")
        println("=".repeat(60))

        demo11DynamicProxies()
        demo12AdvancedAnnotations()
        demo13PerformanceConsiderations()
        demo14SecurityAspects()
        demo15AdvancedPatterns()
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
    private fun demo11DynamicProxies() {
        println("\n--- 3.1 Dynamic Proxies ---")

        val handler = object : InvocationHandler {
            private val target = ServiceImpl()

            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                println("Before method: ${method?.name}")
                val start = System.nanoTime()

                val result = try {
                    method?.invoke(target, *(args ?: emptyArray()))
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }

                val duration = System.nanoTime() - start
                println("After method: ${method?.name} (took ${duration}ns)")
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
    private fun demo12AdvancedAnnotations() {
        println("\n--- 3.2 Advanced Annotation Processing ---")

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
    private fun demo13PerformanceConsiderations() {
        println("\n--- 3.3 Performance Considerations ---")

        val testClass  = PerformanceTest::class.java
        val instance   = testClass.getDeclaredConstructor().newInstance()
        val iterations = 100_000

        println("Method lookup performance ($iterations iterations):")

        val cachedMethod = testClass.getMethod("fastMethod", String::class.java)
        val start1       = System.nanoTime()
        repeat(iterations) { cachedMethod.invoke(instance, "test") }
        val duration1    = System.nanoTime() - start1
        println("Cached method: ${duration1 / 1_000_000}ms")

        val start2 = System.nanoTime()
        repeat(iterations) {
            val method = testClass.getMethod("fastMethod", String::class.java)
            method.invoke(instance, "test")
        }
        val duration2 = System.nanoTime() - start2
        println("Repeated lookup: ${duration2 / 1_000_000}ms")
        println("Performance difference: ${duration2 / duration1.toDouble()}x slower")

        val methodHandles = java.lang.invoke.MethodHandles.lookup()
        val methodHandle  = methodHandles.findVirtual(
            testClass,
            "fastMethod",
            java.lang.invoke.MethodType.methodType(String::class.java, String::class.java)
        )

        val start3    = System.nanoTime()
        repeat(iterations) { methodHandle.invoke(instance, "test") }
        val duration3 = System.nanoTime() - start3
        println("MethodHandle: ${duration3 / 1_000_000}ms")
        println("MethodHandle vs Reflection: ${duration1 / duration3.toDouble()}x faster")

        val field  = testClass.getDeclaredField("data")
        field.isAccessible = true
        val start4 = System.nanoTime()
        repeat(iterations) { field.set(instance, "new value"); field.get(instance) }
        val duration4 = System.nanoTime() - start4
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
    private fun demo14SecurityAspects() {
        println("\n--- 3.4 Security Aspects ---")

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
    private fun demo15AdvancedPatterns() {
        println("\n--- 3.5 Advanced Reflection Patterns ---")

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

interface Service {
    fun doWork(data: String): String
    fun calculate(a: Int, b: Int): Int
}

class ServiceImpl : Service {
    override fun doWork(data: String): String = "Processed: $data"
    override fun calculate(a: Int, b: Int): Int = a + b
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
annotation class Cacheable(val timeout: Int = 30)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Audited(val level: String = "INFO")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Tag(val value: String)

@Cacheable(timeout = 60)
open class AdvancedAnnotatedClass {
    @Cacheable(timeout = 120)
    open fun cachedMethod(): String = "cached"
}

class ChildAnnotatedClass : AdvancedAnnotatedClass() {
    @Audited(level = "DEBUG")
    fun auditedOperation(): String = "audited"

    @Tag("important")
    @Tag("user-facing")
    @Tag("api")
    fun multiTaggedMethod(): String = "tagged"
}

class AnnotationProcessor {
    fun process(instance: Any) {
        val clazz = instance.javaClass
        println("Processing annotations for: ${clazz.simpleName}")

        clazz.annotations.forEach { annotation ->
            when (annotation) {
                is Cacheable -> println("  Class cached with timeout: ${annotation.timeout}")
                else         -> println("  Class annotation: $annotation")
            }
        }

        clazz.methods.forEach { method ->
            method.annotations.forEach { annotation ->
                when (annotation) {
                    is Cacheable -> println("  Method ${method.name} cached with timeout: ${annotation.timeout}")
                    is Audited   -> println("  Method ${method.name} audited at level: ${annotation.level}")
                    else         -> println("  Method ${method.name} annotation: $annotation")
                }
            }
        }
    }
}

class PerformanceTest {
    var data: String = "test data"
    fun fastMethod(input: String): String = "Result: $input"
}

class SecureClass {
    private val secretKey: String = "top-secret-key-123"
    val publicData: String = "public information"
}

object SafeReflectionUtils {
    fun getFieldValue(instance: Any, fieldName: String): Any? {
        val clazz = instance.javaClass
        return try {
            val field = clazz.getDeclaredField(fieldName)
            if (!Modifier.isPublic(field.modifiers)) {
                throw IllegalAccessException("Field $fieldName is not public")
            }
            field.get(instance)
        } catch (e: NoSuchFieldException) {
            throw IllegalArgumentException("Field $fieldName not found in ${clazz.simpleName}")
        }
    }
}

class ReflectionBuilder<T>(private val clazz: Class<T>) {
    private val values = mutableMapOf<String, Any?>()

    fun set(fieldName: String, value: Any?): ReflectionBuilder<T> {
        values[fieldName] = value
        return this
    }

    fun build(): T {
        val instance = clazz.getDeclaredConstructor().newInstance()
        values.forEach { (fieldName, value) ->
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(instance, value)
            } catch (e: NoSuchFieldException) {
                println("Warning: Field $fieldName not found in ${clazz.simpleName}")
            }
        }
        return instance
    }
}

class SimpleDIContainer {
    private val registry  = mutableMapOf<Class<*>, () -> Any>()
    private val instances = mutableMapOf<Class<*>, Any>()

    fun <T> register(type: Class<T>, factory: () -> T) {
        @Suppress("UNCHECKED_CAST")
        registry[type] = factory as () -> Any
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: Class<T>): T {
        return instances.getOrPut(type) {
            val factory  = registry[type] ?: throw IllegalArgumentException("Type not registered: ${type.simpleName}")
            val instance = factory()
            injectDependencies(instance)
            instance
        } as T
    }

    private fun injectDependencies(instance: Any) {
        instance.javaClass.declaredFields.forEach { field ->
            if (field.isAnnotationPresent(Inject::class.java)) {
                field.isAccessible = true
                field.set(instance, get(field.type))
            }
        }
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

interface DatabaseService {
    fun save(data: String)
}

class DatabaseServiceImpl : DatabaseService {
    override fun save(data: String) {
        println("Saving to database: $data")
    }
}

interface UserService {
    fun createUser(name: String)
}

class UserServiceImpl : UserService {
    @Inject
    lateinit var databaseService: DatabaseService

    override fun createUser(name: String) {
        println("Creating user: $name")
        databaseService.save("User: $name")
    }
}

class SimpleObjectMapper {
    fun <T> map(data: Map<String, Any>, targetClass: Class<T>): T {
        val instance = targetClass.getDeclaredConstructor().newInstance()
        data.forEach { (key, value) ->
            try {
                val field = targetClass.getDeclaredField(key)
                field.isAccessible = true
                val convertedValue = when {
                    field.type == value.javaClass                    -> value
                    field.type == Int::class.java && value is Number -> value.toInt()
                    field.type == String::class.java                 -> value.toString()
                    else                                             -> value
                }
                field.set(instance, convertedValue)
            } catch (e: NoSuchFieldException) {
                println("Warning: Field $key not found in ${targetClass.simpleName}")
            } catch (e: Exception) {
                println("Warning: Cannot set field $key: ${e.message}")
            }
        }
        return instance
    }
}

data class ReflectionPerson(
    var name:  String = "",
    var age:   Int    = 0,
    var email: String = ""
)

class ComplexObject {
    val id:     Long          = 12345
    val name:   String        = "Complex"
    private val secret: String = "hidden"
    val list:   List<String>  = listOf("a", "b", "c")
}

class ReflectionToStringBuilder {
    fun toString(obj: Any): String {
        val clazz  = obj.javaClass
        val fields = mutableListOf<String>()
        clazz.declaredFields.forEach { field ->
            field.isAccessible = true
            try {
                fields.add("${field.name}=${field.get(obj)}")
            } catch (e: Exception) {
                fields.add("${field.name}=<inaccessible>")
            }
        }
        return "${clazz.simpleName}(${fields.joinToString(", ")})"
    }
}
