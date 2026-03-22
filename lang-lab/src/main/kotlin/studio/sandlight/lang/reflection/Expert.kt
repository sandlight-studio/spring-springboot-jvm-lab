package studio.sandlight.lang.reflection

// LEVEL 4: Expert — 自定义 ClassLoader、框架级模式、序列化、测试框架

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

object Expert {

    fun run() {
        println("🧠 LEVEL 4: EXPERT - ClassLoaders, Frameworks, Real-world Patterns")
        println("=".repeat(68))

        demo16CustomClassLoader()
        demo17AdvancedFrameworkPatterns()
        demo18SerializationFramework()
        demo19TestingFramework()
        demo20ReflectionBestPractices()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.1 自定义 ClassLoader
    //
    // ClassLoader 双亲委派模型（Parent-First Delegation）：
    //
    //   CustomClassLoader
    //        │  loadClass(name) → 先委托给 parent
    //        ↓
    //   AppClassLoader          ← 加载 classpath 上的类
    //        │
    //        ↓
    //   PlatformClassLoader     ← 加载 java.* 扩展模块 (Java 9+)
    //        │
    //        ↓
    //   BootstrapClassLoader    ← 加载 java.lang.* / rt.jar
    //
    // 打破双亲委派的场景：
    //   - SPI（ServiceLoader）：子类加载器加载 META-INF/services 实现
    //   - OSGi：每个 Bundle 有独立 ClassLoader
    //   - 热重载：新版本类由子 ClassLoader 加载，覆盖缓存
    //
    // 阅读 JDK 源码建议：
    //   java.base/java/lang/ClassLoader.java  → loadClass() / findClass() / defineClass()
    //   java.base/java/lang/Class.java        → forName(name, init, loader)
    // ──────────────────────────────────────────────────────────────
    private fun demo16CustomClassLoader() {
        println("\n--- 4.1 Custom ClassLoader ---")

        val customLoader = CustomClassLoader()
        println("Custom ClassLoader demonstration:")
        println("ClassLoader: ${customLoader.javaClass.simpleName}")
        println("Parent: ${customLoader.parent?.javaClass?.simpleName}")
        println("Custom class loader created successfully")

        println("\nClassLoader hierarchy:")
        var currentLoader: ClassLoader? = customLoader
        var level = 0
        while (currentLoader != null) {
            println("${"  ".repeat(level)}${currentLoader.javaClass.simpleName}")
            currentLoader = currentLoader.parent
            level++
        }

        val originalContextLoader = Thread.currentThread().contextClassLoader
        println("\nOriginal context loader: ${originalContextLoader.javaClass.simpleName}")

        Thread.currentThread().contextClassLoader = customLoader
        println("Set custom context loader: ${Thread.currentThread().contextClassLoader.javaClass.simpleName}")

        Thread.currentThread().contextClassLoader = originalContextLoader
    }

    // ──────────────────────────────────────────────────────────────
    // 4.2 框架级模式
    //
    // Spring 的核心反射机制简图：
    //
    //   BeanDefinition (类元数据)
    //       │
    //       ├── 实例化：Constructor.newInstance()
    //       ├── 依赖注入：Field.set() / Method.invoke()  (@Autowired)
    //       └── 生命周期：Method.invoke()                (@PostConstruct)
    //
    // Spring 源码对应：
    //   spring-beans/AbstractAutowireCapableBeanFactory.java
    //     → createBeanInstance()  → instantiateBean()
    //     → populateBean()        → applyPropertyValues() / autowireByType()
    //     → invokeInitMethods()   → invokeCustomInitMethod()
    // ──────────────────────────────────────────────────────────────
    private fun demo17AdvancedFrameworkPatterns() {
        println("\n--- 4.2 Advanced Framework Patterns ---")

        val advancedContainer = AdvancedDIContainer()
        advancedContainer.registerBean("userService",  UserServiceAdvanced::class.java)
        advancedContainer.registerBean("emailService", EmailServiceImpl::class.java)
        advancedContainer.registerBean("auditService", AuditServiceImpl::class.java)

        val userService = advancedContainer.getBean("userService", UserServiceAdvanced::class.java)
        userService.createUser("John Doe", "john@example.com")

        println("\nAOP-style method interception:")
        println("Note: Java dynamic proxies only work with interfaces")
        println("For concrete class interception, you'd need CGLIB or ByteBuddy")

        val emailService          = advancedContainer.getBean("emailService", EmailService::class.java)
        val interceptor           = MethodInterceptor()
        val interceptedEmailService = interceptor.intercept(emailService)
        interceptedEmailService.sendEmail("intercepted@example.com", "Intercepted", "This call was intercepted")

        println("\nConfiguration binding:")
        val config = mapOf(
            "database.url"            to "jdbc:postgresql://localhost:5432/mydb",
            "database.username"       to "admin",
            "database.maxConnections" to 20,
            "cache.enabled"           to true,
            "cache.ttl"               to 3600
        )

        val configBinder = ConfigurationBinder()
        val dbConfig     = configBinder.bind(config, DatabaseConfig::class.java, "database")
        val cacheConfig  = configBinder.bind(config, CacheConfig::class.java, "cache")

        println("Database config: $dbConfig")
        println("Cache config: $cacheConfig")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.3 反射实现序列化框架
    //
    // 真实框架（Jackson / Gson）的核心也是：
    //   序列化：遍历 getDeclaredFields() → 逐字段 get → 生成 JSON
    //   反序列化：解析 JSON → 匹配字段名 → field.set(instance, value)
    //
    // 性能优化方向：
    //   - 缓存 Field 对象（避免重复查找）
    //   - 使用 MethodHandle 代替 Field.get/set
    //   - 代码生成（Jackson 的 afterburner 模块）
    // ──────────────────────────────────────────────────────────────
    private fun demo18SerializationFramework() {
        println("\n--- 4.3 Reflection-based Serialization ---")

        val jsonSerializer = ReflectionJsonSerializer()
        val user           = User(1, "Alice Johnson", "alice@example.com", 28)
        user.address       = Address("123 Main St", "Springfield", "USA")

        val json = jsonSerializer.serialize(user)
        println("Serialized JSON:")
        println(json)

        val jsonDeserializer   = ReflectionJsonDeserializer()
        val deserializedUser   = jsonDeserializer.deserialize(json, User::class.java)
        println("\nDeserialized object:")
        println("ID: ${deserializedUser.id}")
        println("Name: ${deserializedUser.name}")
        println("Email: ${deserializedUser.email}")
        println("Age: ${deserializedUser.age}")
        println("Address: ${deserializedUser.address}")

        println("\nDeep copy example:")
        val deepCopier = ReflectionDeepCopier()
        val userCopy   = deepCopier.deepCopy(user)
        println("Original: $user")
        println("Copy: $userCopy")
        println("Are same instance: ${user === userCopy}")
        println("Are equal: ${user == userCopy}")

        userCopy.name = "Alice Modified"
        println("After modifying copy:")
        println("Original name: ${user.name}")
        println("Copy name: ${userCopy.name}")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.4 测试框架模式
    //
    // Mock 框架（Mockito）核心原理：
    //
    //   createMock(interface)
    //       │
    //       └── Proxy.newProxyInstance(...)  ← JDK 代理（接口 Mock）
    //           或 ByteBuddy/CGLIB           ← 具体类 Mock
    //                   │
    //                   └── InvocationHandler 记录调用 + 返回默认值
    //
    //   verify(mock).someMethod()
    //       └── 检查 interactions 列表中是否有对应记录
    //
    // Spy = 真实对象 + 代理包裹（调用真实方法并记录调用）
    // ──────────────────────────────────────────────────────────────
    private fun demo19TestingFramework() {
        println("\n--- 4.4 Testing Framework Patterns ---")

        val mockFramework    = SimpleMockFramework()
        val mockEmailService = mockFramework.createMock(EmailService::class.java)
        println("Created mock: ${mockEmailService.javaClass.simpleName}")

        mockEmailService.sendEmail("test@example.com", "Test Subject", "Test Body")

        val interactions = mockFramework.getInteractions(mockEmailService)
        println("Mock interactions: ${interactions.size}")
        interactions.forEach { interaction ->
            println("  Called: ${interaction.methodName} with args: ${interaction.args.joinToString()}")
        }

        println("\nTest spy example:")
        val realEmailService: EmailService = EmailServiceImpl()
        val spy                            = mockFramework.createSpy(realEmailService)
        spy.sendEmail("spy@example.com", "Spy Test", "This is a spy test")

        val spyInteractions = mockFramework.getInteractions(spy)
        println("Spy interactions: ${spyInteractions.size}")

        println("\nAssertion framework:")
        val assertionFramework = ReflectionAssertions()
        val testObject         = TestData("test", 42, listOf("a", "b", "c"))

        try {
            assertionFramework.assertField(testObject, "name", "test")
            println("✅ Field assertion passed")
        } catch (e: AssertionError) {
            println("❌ Field assertion failed: ${e.message}")
        }

        try {
            assertionFramework.assertField(testObject, "number", 42)
            println("✅ Number field assertion passed")
        } catch (e: Exception) {
            println("❌ Number field assertion failed: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4.5 最佳实践与性能优化
    //
    // 反射使用的黄金法则：
    //
    //   ┌──────────────────────────────────────────────────────┐
    //   │ 操作              │ 推荐做法                          │
    //   ├──────────────────────────────────────────────────────┤
    //   │ Method/Field 查找 │ 缓存，避免每次调用重复 lookup    │
    //   │ 高频方法调用      │ 用 MethodHandle 替代 Method       │
    //   │ 类型安全         │ isAssignableFrom() 先检查          │
    //   │ 错误处理         │ 包装为业务异常，不要吞掉反射异常   │
    //   │ 安全             │ 尽量只访问 public 成员             │
    //   └──────────────────────────────────────────────────────┘
    // ──────────────────────────────────────────────────────────────
    private fun demo20ReflectionBestPractices() {
        println("\n--- 4.5 Best Practices & Performance Optimization ---")

        val reflectionCache = ReflectionCache()
        val testClass       = PerformanceTest::class.java
        val iterations      = 10_000

        val start1 = System.nanoTime()
        repeat(iterations) {
            reflectionCache.getMethod(testClass, "fastMethod", String::class.java)
        }
        val duration1 = System.nanoTime() - start1
        println("Cached reflection lookups ($iterations): ${duration1 / 1_000_000}ms")

        val start2 = System.nanoTime()
        repeat(iterations) {
            testClass.getMethod("fastMethod", String::class.java)
        }
        val duration2 = System.nanoTime() - start2
        println("Non-cached reflection lookups ($iterations): ${duration2 / 1_000_000}ms")
        println("Cache performance improvement: ${duration2 / duration1.toDouble()}x faster")

        println("\nMemory considerations:")
        val memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val methods   = mutableListOf<Method>()
        repeat(1000) {
            methods.add(testClass.getMethod("fastMethod", String::class.java))
        }
        val memAfter  = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println("Memory used by reflection objects: ${(memAfter - memBefore) / 1024}KB")

        println("\nError handling examples:")
        val errorHandler = ReflectionErrorHandler()
        val result1      = errorHandler.safeGetField(PerformanceTest(), "nonExistentField")
        println("Safe field access result: $result1")
        val result2      = errorHandler.safeInvokeMethod(PerformanceTest(), "nonExistentMethod", emptyArray())
        println("Safe method invocation result: $result2")

        val typeSafety = ReflectionTypeSafety()
        println("Type safety examples:")
        println("Can assign String to Object: ${typeSafety.canAssign(String::class.java, Any::class.java)}")
        println("Can assign Object to String: ${typeSafety.canAssign(Any::class.java, String::class.java)}")
        println("Is primitive: ${typeSafety.isPrimitive(Int::class.java)}")

        println("\n📋 Reflection Best Practices Summary:")
        println("1. Cache Method/Field/Constructor objects for performance")
        println("2. Use specific exception handling for reflection operations")
        println("3. Consider security implications and use module system restrictions")
        println("4. Prefer MethodHandles over reflection for repeated invocations")
        println("5. Validate types before assignment to prevent ClassCastException")
        println("6. Be aware of memory overhead and potential memory leaks")
        println("7. Use reflection judiciously - direct method calls are always faster")
        println("8. Consider compile-time alternatives like annotation processing")
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 4 支撑类
// ══════════════════════════════════════════════════════════════════

class CustomClassLoader : ClassLoader() {
    private val classes = mutableMapOf<String, ByteArray>()

    fun defineClass(name: String, bytecode: ByteArray): Class<*> {
        classes[name] = bytecode
        return defineClass(name, bytecode, 0, bytecode.size)
    }

    override fun findClass(name: String): Class<*> {
        val bytecode = classes[name]
            ?: throw ClassNotFoundException("Class $name not found in custom loader")
        return defineClass(name, bytecode, 0, bytecode.size)
    }
}

class AdvancedDIContainer {
    private val beanDefinitions = mutableMapOf<String, BeanDefinition>()
    private val singletons      = mutableMapOf<String, Any>()

    fun registerBean(name: String, clazz: Class<*>) {
        beanDefinitions[name] = BeanDefinition(name, clazz)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getBean(name: String, expectedType: Class<T>): T {
        return singletons.getOrPut(name) {
            val beanDef  = beanDefinitions[name]
                ?: throw IllegalArgumentException("Bean $name not found")
            val instance = beanDef.clazz.getDeclaredConstructor().newInstance()
            injectDependencies(instance)
            invokePostConstruct(instance)
            instance
        } as T
    }

    private fun injectDependencies(instance: Any) {
        instance.javaClass.declaredFields.forEach { field ->
            if (field.isAnnotationPresent(AutowiredAdvanced::class.java)) {
                field.isAccessible = true
                val beanName = findBeanNameByType(field.type)
                if (beanName != null) {
                    field.set(instance, getBean(beanName, field.type))
                }
            }
        }
    }

    private fun invokePostConstruct(instance: Any) {
        instance.javaClass.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(PostConstruct::class.java)) {
                method.isAccessible = true
                method.invoke(instance)
            }
        }
    }

    private fun findBeanNameByType(type: Class<*>): String? {
        return beanDefinitions.entries.find { (_, beanDef) ->
            type.isAssignableFrom(beanDef.clazz)
        }?.key
    }
}

data class BeanDefinition(val name: String, val clazz: Class<*>)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutowiredAdvanced

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostConstruct

interface EmailService {
    fun sendEmail(to: String, subject: String, body: String)
}

class EmailServiceImpl : EmailService {
    override fun sendEmail(to: String, subject: String, body: String) {
        println("📧 Sending email to $to: $subject")
    }
}

interface AuditService {
    fun audit(action: String, user: String)
}

class AuditServiceImpl : AuditService {
    override fun audit(action: String, user: String) {
        println("📝 Audit: $user performed $action")
    }
}

class UserServiceAdvanced {
    @AutowiredAdvanced
    lateinit var emailService: EmailService

    @AutowiredAdvanced
    lateinit var auditService: AuditService

    @PostConstruct
    fun initialize() {
        println("🔧 UserServiceAdvanced initialized")
    }

    fun createUser(name: String, email: String) {
        println("👤 Creating user: $name")
        emailService.sendEmail(email, "Welcome!", "Welcome to our service, $name!")
        auditService.audit("USER_CREATED", name)
    }
}

class MethodInterceptor {
    fun <T> intercept(target: T): T {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                println("🔍 Before ${method?.name}")
                val start  = System.nanoTime()
                val result = method?.invoke(target, *(args ?: emptyArray()))
                println("⏱️  ${method?.name} took ${(System.nanoTime() - start) / 1_000_000}ms")
                return result
            }
        }

        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            target!!::class.java.classLoader,
            target::class.java.interfaces,
            handler
        ) as T
    }
}

class ConfigurationBinder {
    fun <T> bind(properties: Map<String, Any>, targetClass: Class<T>, prefix: String): T {
        val instance = targetClass.getDeclaredConstructor().newInstance()
        targetClass.declaredFields.forEach { field ->
            val value = properties["$prefix.${field.name}"]
            if (value != null) {
                field.isAccessible = true
                field.set(instance, convertValue(value, field.type))
            }
        }
        return instance
    }

    private fun convertValue(value: Any, targetType: Class<*>): Any {
        return when {
            targetType.isAssignableFrom(value.javaClass)          -> value
            targetType == Boolean::class.java && value is String  -> value.toBoolean()
            targetType == Int::class.java     && value is Number  -> value.toInt()
            targetType == Long::class.java    && value is Number  -> value.toLong()
            targetType == String::class.java                      -> value.toString()
            else                                                  -> value
        }
    }
}

data class DatabaseConfig(
    var url:            String = "",
    var username:       String = "",
    var maxConnections: Int    = 0
)

data class CacheConfig(
    var enabled: Boolean = false,
    var ttl:     Int     = 0
)

class ReflectionJsonSerializer {
    fun serialize(obj: Any): String = serializeObject(obj)

    private fun serializeObject(obj: Any): String {
        val clazz  = obj.javaClass
        val fields = mutableListOf<String>()
        clazz.declaredFields.forEach { field ->
            field.isAccessible = true
            val value           = field.get(obj)
            val serializedValue = when (value) {
                null               -> "null"
                is String          -> "\"$value\""
                is Number, is Boolean -> value.toString()
                else               -> serializeObject(value)
            }
            fields.add("\"${field.name}\": $serializedValue")
        }
        return "{${fields.joinToString(", ")}}"
    }
}

class ReflectionJsonDeserializer {
    fun <T> deserialize(json: String, targetClass: Class<T>): T {
        val instance     = targetClass.getDeclaredConstructor().newInstance()
        val fieldPattern = """"([^"]+)":\s*([^,}]+)""".toRegex()
        fieldPattern.findAll(json).forEach { match ->
            val fieldName  = match.groupValues[1]
            val fieldValue = match.groupValues[2].trim()
            try {
                val field = targetClass.getDeclaredField(fieldName)
                field.isAccessible = true
                val convertedValue = when (field.type) {
                    String::class.java  -> fieldValue.removeSurrounding("\"")
                    Int::class.java     -> fieldValue.toIntOrNull()
                    Long::class.java    -> fieldValue.toLongOrNull()
                    Boolean::class.java -> fieldValue.toBooleanStrictOrNull()
                    else                -> null
                }
                if (convertedValue != null) field.set(instance, convertedValue)
            } catch (_: Exception) { }
        }
        return instance
    }
}

class ReflectionDeepCopier {
    fun <T> deepCopy(obj: T): T {
        if (obj == null) return obj
        val clazz = obj!!::class.java

        @Suppress("UNCHECKED_CAST")
        val copy = clazz.getDeclaredConstructor().newInstance() as T

        clazz.declaredFields.forEach { field ->
            field.isAccessible = true
            val value       = field.get(obj)
            val copiedValue = when {
                value == null           -> null
                isPrimitive(value.javaClass) -> value
                value is String         -> value
                else                    -> deepCopy(value)
            }
            field.set(copy, copiedValue)
        }
        return copy
    }

    private fun isPrimitive(clazz: Class<*>): Boolean {
        return clazz.isPrimitive ||
               clazz == java.lang.Boolean::class.java   ||
               clazz == java.lang.Integer::class.java   ||
               clazz == java.lang.Long::class.java      ||
               clazz == java.lang.Double::class.java    ||
               clazz == java.lang.Float::class.java
    }
}

data class User(
    var id:      Long    = 0,
    var name:    String  = "",
    var email:   String  = "",
    var age:     Int     = 0,
    var address: Address? = null
)

data class Address(
    var street:  String = "",
    var city:    String = "",
    var country: String = ""
)

class SimpleMockFramework {
    private val mockInteractions = mutableMapOf<Int, MutableList<MethodInteraction>>()

    fun <T> createMock(interfaceClass: Class<T>): T {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                if (method?.name in listOf("hashCode", "equals", "toString")) {
                    return getDefaultReturn(method!!.returnType)
                }
                recordInteraction(proxy!!, method!!, args ?: emptyArray())
                return getDefaultReturn(method.returnType)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
            handler
        ) as T
    }

    fun <T> createSpy(realObject: T): T {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                if (method?.name in listOf("hashCode", "equals", "toString")) {
                    return method!!.invoke(realObject, *(args ?: emptyArray()))
                }
                recordInteraction(proxy!!, method!!, args ?: emptyArray())
                return method!!.invoke(realObject, *(args ?: emptyArray()))
            }
        }

        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            realObject!!::class.java.classLoader,
            realObject::class.java.interfaces,
            handler
        ) as T
    }

    fun getInteractions(mock: Any): List<MethodInteraction> {
        return mockInteractions[System.identityHashCode(mock)] ?: emptyList()
    }

    private fun recordInteraction(proxy: Any, method: Method, args: Array<out Any>) {
        val key          = System.identityHashCode(proxy)
        val interactions = mockInteractions.getOrPut(key) { mutableListOf() }
        interactions.add(MethodInteraction(method.name, args.toList()))
    }

    private fun getDefaultReturn(returnType: Class<*>): Any? {
        return when {
            returnType == Void.TYPE -> null
            returnType.isPrimitive  -> when (returnType) {
                Boolean::class.java -> false
                Int::class.java     -> 0
                Long::class.java    -> 0L
                Double::class.java  -> 0.0
                Float::class.java   -> 0.0f
                else                -> null
            }
            else -> null
        }
    }
}

data class MethodInteraction(val methodName: String, val args: List<Any>)

class ReflectionAssertions {
    fun assertField(obj: Any, fieldName: String, expected: Any?) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        val actual = field.get(obj)
        if (actual != expected) {
            throw AssertionError("Expected field $fieldName to be $expected, but was $actual")
        }
    }

    fun assertMethodResult(obj: Any, methodName: String, args: Array<Any>, expected: Any?) {
        val paramTypes = args.map { it.javaClass }.toTypedArray()
        val method     = obj.javaClass.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        val actual     = method.invoke(obj, *args)
        if (actual != expected) {
            throw AssertionError("Expected method $methodName to return $expected, but was $actual")
        }
    }
}

data class TestData(
    val name:   String,
    val number: Int,
    val list:   List<String>
)

class ReflectionCache {
    private val methodCache = mutableMapOf<MethodKey, Method>()
    private val fieldCache  = mutableMapOf<FieldKey, java.lang.reflect.Field>()

    fun getMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method {
        val key = MethodKey(clazz, name, paramTypes.toList())
        return methodCache.getOrPut(key) { clazz.getMethod(name, *paramTypes) }
    }

    fun getField(clazz: Class<*>, name: String): java.lang.reflect.Field {
        val key = FieldKey(clazz, name)
        return fieldCache.getOrPut(key) { clazz.getDeclaredField(name) }
    }
}

data class MethodKey(val clazz: Class<*>, val name: String, val paramTypes: List<Class<*>>)
data class FieldKey(val clazz: Class<*>, val name: String)

class ReflectionErrorHandler {
    fun safeGetField(obj: Any, fieldName: String): Any? {
        return try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: NoSuchFieldException) {
            println("Field $fieldName not found"); null
        } catch (e: IllegalAccessException) {
            println("Cannot access field $fieldName"); null
        } catch (e: Exception) {
            println("Error accessing field $fieldName: ${e.message}"); null
        }
    }

    fun safeInvokeMethod(obj: Any, methodName: String, args: Array<Any>): Any? {
        return try {
            val paramTypes = args.map { it.javaClass }.toTypedArray()
            val method     = obj.javaClass.getDeclaredMethod(methodName, *paramTypes)
            method.isAccessible = true
            method.invoke(obj, *args)
        } catch (e: NoSuchMethodException) {
            println("Method $methodName not found"); null
        } catch (e: IllegalAccessException) {
            println("Cannot access method $methodName"); null
        } catch (e: Exception) {
            println("Error invoking method $methodName: ${e.message}"); null
        }
    }
}

class ReflectionTypeSafety {
    fun canAssign(from: Class<*>, to: Class<*>): Boolean = to.isAssignableFrom(from)

    fun isPrimitive(clazz: Class<*>): Boolean {
        return clazz.isPrimitive || WRAPPER_TO_PRIMITIVE.containsKey(clazz)
    }

    companion object {
        private val WRAPPER_TO_PRIMITIVE = mapOf(
            java.lang.Boolean::class.java   to Boolean::class.java,
            java.lang.Integer::class.java   to Int::class.java,
            java.lang.Long::class.java      to Long::class.java,
            java.lang.Double::class.java    to Double::class.java,
            java.lang.Float::class.java     to Float::class.java,
            java.lang.Character::class.java to Char::class.java,
            java.lang.Byte::class.java      to Byte::class.java,
            java.lang.Short::class.java     to Short::class.java
        )
    }
}
