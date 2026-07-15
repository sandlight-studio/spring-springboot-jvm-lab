package studio.sandlight.lang.reflection

import studio.sandlight.lang.support.Lab
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.system.measureNanoTime

// LEVEL 4: Expert — 自定义 ClassLoader、框架级模式、序列化、测试框架
// (support classes live in ExpertSupport.kt)

object Expert {

    fun run() {

        demo41CustomClassLoader()
        demo42AdvancedFrameworkPatterns()
        demo43SerializationFramework()
        demo44TestingFramework()
        demo45ReflectionBestPractices()
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
    private fun demo41CustomClassLoader() {
        Lab.section("4.1", "Custom ClassLoader")

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
    private fun demo42AdvancedFrameworkPatterns() {
        Lab.section("4.2", "Advanced Framework Patterns")

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
    private fun demo43SerializationFramework() {
        Lab.section("4.3", "Reflection-based Serialization")

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
    private fun demo44TestingFramework() {
        Lab.section("4.4", "Testing Framework Patterns")

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
    private fun demo45ReflectionBestPractices() {
        Lab.section("4.5", "Best Practices & Performance Optimization")

        val reflectionCache = ReflectionCache()
        val testClass       = PerformanceTest::class.java
        val iterations      = 10_000

        val duration1 = measureNanoTime {
            repeat(iterations) {
                reflectionCache.getMethod(testClass, "fastMethod", String::class.java)
            }
        }
        println("Cached reflection lookups ($iterations): ${duration1 / 1_000_000}ms")

        val duration2 = measureNanoTime {
            repeat(iterations) {
                testClass.getMethod("fastMethod", String::class.java)
            }
        }
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
