package studio.sandlight.lang

import java.lang.reflect.*
import java.util.*

object ReflectionBasics {
    
    fun run(args: Array<String>) {
        if (args.size < 2) {
            printReflectionUsage()
            return
        }
        
        when (args[1].lowercase()) {
            "basic", "foundation", "1" -> runLevel1Basic()
            "intermediate", "2" -> runLevel2Intermediate()
            "advanced", "3" -> runLevel3Advanced()
            "expert", "4" -> runLevel4Expert()
            "all" -> runAllLevels()
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
        runLevel1Basic()
        println("\n" + "=".repeat(60) + "\n")
        runLevel2Intermediate()
        println("\n" + "=".repeat(60) + "\n")
        runLevel3Advanced()
        println("\n" + "=".repeat(60) + "\n")
        runLevel4Expert()
    }
    
    // ===== LEVEL 1: FOUNDATION (BASIC) =====
    private fun runLevel1Basic() {
        println("🎯 LEVEL 1: FOUNDATION - Basic Reflection")
        println("=".repeat(50))
        
        demo1ClassObjects()
        demo2ClassInformation()
        demo3FieldAccess()
        demo4MethodInvocation()
        demo5ConstructorAccess()
    }
    
    private fun demo1ClassObjects() {
        println("\n--- 1.1 Getting Class Objects ---")
        
        // Method 1: Class.forName()
        val stringClass1 = Class.forName("java.lang.String")
        println("Class.forName: ${stringClass1.name}")
        
        // Method 2: .class literal
        val stringClass2 = String::class.java
        println(".class literal: ${stringClass2.name}")
        
        // Method 3: instance.getClass()
        val str = "Hello"
        val stringClass3 = str.javaClass
        println("getClass(): ${stringClass3.name}")
        
        // They're all the same object!
        println("All three are identical: ${stringClass1 === stringClass2 && stringClass2 === stringClass3}")
        
        // Primitive types
        val intClass = Int::class.java
        val intPrimitive = java.lang.Integer.TYPE
        println("int.class: ${intClass.name}")
        println("Integer.TYPE: ${intPrimitive.name}")
        println("Are they same? ${intClass === intPrimitive}")
    }
    
    private fun demo2ClassInformation() {
        println("\n--- 1.2 Basic Class Information ---")
        
        val clazz = SamplePerson::class.java
        
        println("Simple name: ${clazz.simpleName}")
        println("Canonical name: ${clazz.canonicalName}")
        println("Package: ${clazz.`package`?.name}")
        
        // Modifiers
        val modifiers = clazz.modifiers
        println("Modifiers: ${Modifier.toString(modifiers)}")
        println("Is public? ${Modifier.isPublic(modifiers)}")
        println("Is final? ${Modifier.isFinal(modifiers)}")
        
        // Inheritance
        println("Superclass: ${clazz.superclass?.simpleName}")
        println("Interfaces: ${clazz.interfaces.map { it.simpleName }}")
    }
    
    private fun demo3FieldAccess() {
        println("\n--- 1.3 Field Access and Modification ---")
        
        val person = SamplePerson("John", 25, "secret")
        val clazz = person.javaClass
        
        // Get all declared fields (including private)
        println("All declared fields:")
        clazz.declaredFields.forEach { field ->
            println("  ${Modifier.toString(field.modifiers)} ${field.type.simpleName} ${field.name}")
        }
        
        // Access field (need to make accessible since it's private)
        val nameField = clazz.getDeclaredField("name")
        nameField.isAccessible = true
        println("\nOriginal name: ${nameField.get(person)}")
        nameField.set(person, "Jane")
        println("Modified name: ${nameField.get(person)}")
        
        // Access private field (need setAccessible)
        val secretField = clazz.getDeclaredField("secret")
        
        println("\nTrying to access private field without setAccessible:")
        try {
            secretField.get(person)
        } catch (e: IllegalAccessException) {
            println("❌ Access denied: ${e.message}")
        }
        
        println("\nNow with setAccessible(true):")
        secretField.isAccessible = true
        println("✅ Private secret: ${secretField.get(person)}")
        secretField.set(person, "new-secret")
        println("✅ Modified secret: ${secretField.get(person)}")
        
        // Field type information
        println("\nField types:")
        clazz.declaredFields.forEach { field ->
            println("  ${field.name} -> ${field.type.name}")
        }
    }
    
    private fun demo4MethodInvocation() {
        println("\n--- 1.4 Method Invocation ---")
        
        val person = SamplePerson("Alice", 30, "hidden")
        val clazz = person.javaClass
        
        // Get all methods
        println("All declared methods:")
        clazz.declaredMethods.forEach { method ->
            val params = method.parameterTypes.map { it.simpleName }.joinToString(", ")
            println("  ${method.name}($params) -> ${method.returnType.simpleName}")
        }
        
        // Invoke public method
        val getInfoMethod = clazz.getDeclaredMethod("getInfo")
        val result = getInfoMethod.invoke(person)
        println("\nInvoked getInfo(): $result")
        
        // Invoke method with parameters (using property setter)
        person.age = 35
        println("After setting age to 35: ${person.getInfo()}")
        
        // Invoke private method
        val privateMethod = clazz.getDeclaredMethod("privateMethod")
        privateMethod.isAccessible = true
        val privateResult = privateMethod.invoke(person)
        println("Private method result: $privateResult")
        
        // Static method
        val utilsClass = ReflectionUtils::class.java
        val staticMethod = utilsClass.getDeclaredMethod("staticUtility", String::class.java)
        val staticResult = staticMethod.invoke(null, "test")
        println("Static method result: $staticResult")
    }
    
    private fun demo5ConstructorAccess() {
        println("\n--- 1.5 Constructor Access ---")
        
        val clazz = SamplePerson::class.java
        
        // Get all constructors
        println("All constructors:")
        clazz.declaredConstructors.forEach { constructor ->
            val params = constructor.parameterTypes.map { it.simpleName }.joinToString(", ")
            println("  SamplePerson($params)")
        }
        
        // Create instance using default constructor
        val defaultConstructor = clazz.getDeclaredConstructor()
        val person1 = defaultConstructor.newInstance() as SamplePerson
        println("\nDefault constructor: ${person1.getInfo()}")
        
        // Create instance using parameterized constructor
        val paramConstructor = clazz.getDeclaredConstructor(String::class.java, Int::class.java, String::class.java)
        val person2 = paramConstructor.newInstance("Bob", 28, "password") as SamplePerson
        println("Parameterized constructor: ${person2.getInfo()}")
        
        // Constructor with fewer parameters
        val simpleConstructor = clazz.getDeclaredConstructor(String::class.java)
        val person3 = simpleConstructor.newInstance("Charlie") as SamplePerson
        println("Simple constructor: ${person3.getInfo()}")
    }
    
    // ===== LEVEL 2: INTERMEDIATE =====
    private fun runLevel2Intermediate() {
        println("🚀 LEVEL 2: INTERMEDIATE - Generics, Annotations, Arrays, Nested Classes")
        println("=".repeat(65))
        
        demo6GenericTypes()
        demo7Annotations()
        demo8ArrayManipulation()
        demo9NestedClasses()
        demo10AccessControl()
    }
    
    private fun demo6GenericTypes() {
        println("\n--- 2.1 Generic Type Information ---")
        
        val listField = GenericContainer::class.java.getDeclaredField("stringList")
        val genericType = listField.genericType
        
        println("Field: ${listField.name}")
        println("Raw type: ${listField.type}")
        println("Generic type: $genericType")
        
        if (genericType is ParameterizedType) {
            println("Is ParameterizedType: true")
            println("Raw type: ${genericType.rawType}")
            println("Actual type arguments: ${genericType.actualTypeArguments.joinToString()}")
            
            val typeArg = genericType.actualTypeArguments[0]
            println("First type argument: $typeArg (${typeArg.javaClass.simpleName})")
        }
        
        // Method with generic parameters and return type
        val method = GenericContainer::class.java.getDeclaredMethod("processMap", Map::class.java)
        println("\nMethod: ${method.name}")
        
        method.genericParameterTypes.forEachIndexed { index, type ->
            println("Parameter $index: $type")
            if (type is ParameterizedType) {
                println("  Raw type: ${type.rawType}")
                println("  Type args: ${type.actualTypeArguments.joinToString()}")
            }
        }
        
        val returnType = method.genericReturnType
        println("Return type: $returnType")
        if (returnType is ParameterizedType) {
            println("  Type arguments: ${returnType.actualTypeArguments.joinToString()}")
        }
        
        // Type variables
        val clazz = GenericClass::class.java
        val typeParameters = clazz.typeParameters
        println("\nGeneric class type parameters:")
        typeParameters.forEach { typeVar ->
            println("  ${typeVar.name}: bounds=${typeVar.bounds.joinToString()}")
        }
    }
    
    private fun demo7Annotations() {
        println("\n--- 2.2 Annotations Processing ---")
        
        val clazz = AnnotatedSample::class.java
        
        // Class annotations
        println("Class annotations:")
        clazz.annotations.forEach { annotation ->
            println("  $annotation")
        }
        
        val entityAnnotation = clazz.getAnnotation(Entity::class.java)
        if (entityAnnotation != null) {
            println("Entity name: ${entityAnnotation.name}")
            println("Entity table: ${entityAnnotation.tableName}")
        }
        
        // Field annotations
        println("\nField annotations:")
        clazz.declaredFields.forEach { field ->
            if (field.annotations.isNotEmpty()) {
                println("Field ${field.name}:")
                field.annotations.forEach { annotation ->
                    println("  $annotation")
                    
                    when (annotation) {
                        is Column -> {
                            println("    Column name: ${annotation.name}")
                            println("    Column nullable: ${annotation.nullable}")
                        }
                        is NotNull -> {
                            println("    NotNull message: ${annotation.message}")
                        }
                    }
                }
            }
        }
        
        // Method annotations
        println("\nMethod annotations:")
        val validateMethod = clazz.getDeclaredMethod("validateData")
        validateMethod.annotations.forEach { annotation ->
            println("  $annotation")
        }
        
        // Parameter annotations
        val updateMethod = clazz.getDeclaredMethod("updateField", String::class.java)
        val paramAnnotations = updateMethod.parameterAnnotations
        paramAnnotations.forEachIndexed { paramIndex, annotations ->
            if (annotations.isNotEmpty()) {
                println("Parameter $paramIndex annotations:")
                annotations.forEach { annotation ->
                    println("  $annotation")
                }
            }
        }
    }
    
    private fun demo8ArrayManipulation() {
        println("\n--- 2.3 Array Manipulation ---")
        
        // Create arrays dynamically
        val intArray = java.lang.reflect.Array.newInstance(Int::class.java, 5) as IntArray
        val stringArray = java.lang.reflect.Array.newInstance(String::class.java, 3) as Array<String>
        
        // Set array values
        java.lang.reflect.Array.set(intArray, 0, 10)
        java.lang.reflect.Array.set(intArray, 1, 20)
        java.lang.reflect.Array.set(stringArray, 0, "Hello")
        java.lang.reflect.Array.set(stringArray, 1, "World")
        
        println("Int array length: ${java.lang.reflect.Array.getLength(intArray)}")
        println("Int array[0]: ${java.lang.reflect.Array.get(intArray, 0)}")
        println("Int array[1]: ${java.lang.reflect.Array.get(intArray, 1)}")
        
        println("String array length: ${java.lang.reflect.Array.getLength(stringArray)}")
        println("String array[0]: ${java.lang.reflect.Array.get(stringArray, 0)}")
        
        // Multi-dimensional arrays
        val dimensions = intArrayOf(2, 3)
        val multiArray = java.lang.reflect.Array.newInstance(Int::class.java, *dimensions) as Array<IntArray>
        
        // Access nested array
        val firstRow = java.lang.reflect.Array.get(multiArray, 0) as IntArray
        java.lang.reflect.Array.set(firstRow, 0, 100)
        java.lang.reflect.Array.set(firstRow, 1, 200)
        
        println("Multi-array[0][0]: ${java.lang.reflect.Array.get(firstRow, 0)}")
        println("Multi-array[0][1]: ${java.lang.reflect.Array.get(firstRow, 1)}")
        
        // Array component type
        val arrayField = ArrayContainer::class.java.getDeclaredField("numbers")
        val arrayType = arrayField.type
        println("\nArray field type: $arrayType")
        println("Is array: ${arrayType.isArray}")
        println("Component type: ${arrayType.componentType}")
        
        // Generic array type
        val genericArrayField = ArrayContainer::class.java.getDeclaredField("genericList")
        val genericArrayType = genericArrayField.genericType
        println("Generic array type: $genericArrayType")
        
        if (genericArrayType is GenericArrayType) {
            println("Generic component type: ${genericArrayType.genericComponentType}")
        }
    }
    
    private fun demo9NestedClasses() {
        println("\n--- 2.4 Nested and Inner Classes ---")
        
        val outerClass = OuterClass::class.java
        
        // Get nested classes
        println("Declared classes in OuterClass:")
        outerClass.declaredClasses.forEach { nestedClass ->
            println("  ${nestedClass.simpleName} (${Modifier.toString(nestedClass.modifiers)})")
        }
        
        // Static nested class
        val staticNestedClass = OuterClass.StaticNested::class.java
        println("\nStatic nested class: ${staticNestedClass.simpleName}")
        println("Enclosing class: ${staticNestedClass.enclosingClass?.simpleName}")
        println("Is member class: ${staticNestedClass.isMemberClass}")
        
        val staticInstance = staticNestedClass.getDeclaredConstructor().newInstance()
        val staticMethod = staticNestedClass.getDeclaredMethod("getMessage")
        println("Static nested method result: ${staticMethod.invoke(staticInstance)}")
        
        // Inner class (non-static)
        val innerClass = OuterClass.InnerClass::class.java
        println("\nInner class: ${innerClass.simpleName}")
        println("Enclosing class: ${innerClass.enclosingClass?.simpleName}")
        println("Is member class: ${innerClass.isMemberClass}")
        
        // Create inner class instance (needs outer instance)
        val outerInstance = OuterClass("outer-data")
        val innerConstructor = innerClass.getDeclaredConstructor(OuterClass::class.java)
        val innerInstance = innerConstructor.newInstance(outerInstance)
        val innerMethod = innerClass.getDeclaredMethod("accessOuter")
        println("Inner class result: ${innerMethod.invoke(innerInstance)}")
        
        // Anonymous class
        val anonymousField = OuterClass::class.java.getDeclaredField("anonymousRunnable")
        anonymousField.isAccessible = true
        val anonymousInstance = anonymousField.get(outerInstance)
        val anonymousClass = anonymousInstance.javaClass
        
        println("\nAnonymous class: ${anonymousClass.simpleName}")
        println("Is anonymous: ${anonymousClass.isAnonymousClass}")
        println("Enclosing class: ${anonymousClass.enclosingClass?.simpleName}")
        println("Enclosing method: ${anonymousClass.enclosingMethod?.name}")
    }
    
    private fun demo10AccessControl() {
        println("\n--- 2.5 Advanced Access Control ---")
        
        val restrictedClass = RestrictedClass::class.java
        val instance = restrictedClass.getDeclaredConstructor().newInstance()
        
        // Access private fields
        println("Private fields access:")
        val privateField = restrictedClass.getDeclaredField("privateData")
        println("Before setAccessible - isAccessible: ${privateField.isAccessible}")
        
        try {
            privateField.get(instance)
        } catch (e: IllegalAccessException) {
            println("Cannot access private field: ${e.message}")
        }
        
        privateField.isAccessible = true
        println("After setAccessible - isAccessible: ${privateField.isAccessible}")
        println("Private field value: ${privateField.get(instance)}")
        
        // Access private methods
        val privateMethod = restrictedClass.getDeclaredMethod("privateCalculation", Int::class.java)
        privateMethod.isAccessible = true
        val result = privateMethod.invoke(instance, 5)
        println("Private method result: $result")
        
        // Final field modification
        val finalField = restrictedClass.getDeclaredField("finalValue")
        finalField.isAccessible = true
        
        println("Original final value: ${finalField.get(instance)}")
        
        // Attempt to remove final modifier (blocked in modern JVMs)
        try {
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(finalField, finalField.modifiers and Modifier.FINAL.inv())
            
            finalField.set(instance, "modified")
            println("✅ Modified final value: ${finalField.get(instance)}")
        } catch (e: Exception) {
            println("❌ Cannot modify final field (blocked by JVM): ${e.javaClass.simpleName}")
            println("   Modern JVMs prevent modification of final fields for security")
        }
        
        // Static private access (Kotlin companion object)
        try {
            val companionClass = Class.forName("${restrictedClass.name}\$Companion")
            val staticPrivateMethod = companionClass.getDeclaredMethod("staticPrivateUtility")
            staticPrivateMethod.isAccessible = true
            val companionInstance = restrictedClass.getDeclaredField("Companion").get(null)
            val staticResult = staticPrivateMethod.invoke(companionInstance)
            println("Static private method result: $staticResult")
        } catch (e: Exception) {
            println("❌ Companion object method access failed: ${e.javaClass.simpleName}")
            println("   Kotlin companion objects have complex reflection access patterns")
        }
    }
    
    // ===== LEVEL 3: ADVANCED =====
    private fun runLevel3Advanced() {
        println("⚡ LEVEL 3: ADVANCED - Dynamic Proxies, Performance, Security")
        println("=".repeat(60))
        
        demo11DynamicProxies()
        demo12AdvancedAnnotations()
        demo13PerformanceConsiderations()
        demo14SecurityAspects()
        demo15AdvancedPatterns()
    }
    
    private fun demo11DynamicProxies() {
        println("\n--- 3.1 Dynamic Proxies ---")
        
        // Create a dynamic proxy for an interface
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
        
        // Use the proxy
        println("\nUsing proxy:")
        proxy.doWork("test data")
        val result = proxy.calculate(10, 5)
        println("Calculation result: $result")
        
        // Multiple interfaces proxy
        val multiHandler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                return when (method?.name) {
                    "run" -> { println("Runnable.run() called"); Unit }
                    "call" -> { println("Callable.call() called"); "proxy result" }
                    else -> throw UnsupportedOperationException("Unknown method: ${method?.name}")
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
    
    private fun demo12AdvancedAnnotations() {
        println("\n--- 3.2 Advanced Annotation Processing ---")
        
        // Custom annotation processor
        val processor = AnnotationProcessor()
        val instance = AdvancedAnnotatedClass()
        
        processor.process(instance)
        
        // Annotation inheritance
        val childClass = ChildAnnotatedClass::class.java
        val parentAnnotation = childClass.getAnnotation(Cacheable::class.java)
        println("\nAnnotation inheritance:")
        println("Child has @Cacheable: ${parentAnnotation != null}")
        
        // Annotation on inherited method
        val method = childClass.getMethod("cachedMethod")
        val methodCacheable = method.getAnnotation(Cacheable::class.java)
        println("Inherited method has @Cacheable: ${methodCacheable != null}")
        if (methodCacheable != null) {
            println("Cache timeout: ${methodCacheable.timeout}")
        }
        
        // Meta-annotations
        val auditedMethod = childClass.getMethod("auditedOperation")
        val audited = auditedMethod.getAnnotation(Audited::class.java)
        if (audited != null) {
            println("\nMeta-annotation example:")
            println("Audited level: ${audited.level}")
            
            // Check if the annotation itself has meta-annotations
            val auditedClass = audited.annotationClass.java
            auditedClass.annotations.forEach { metaAnnotation ->
                println("Meta-annotation on @Audited: $metaAnnotation")
            }
        }
        
        // Repeatable annotations
        val repeatedMethod = childClass.getMethod("multiTaggedMethod")
        val tags = repeatedMethod.getAnnotationsByType(Tag::class.java)
        println("\nRepeatable annotations:")
        tags.forEach { tag ->
            println("Tag: ${tag.value}")
        }
    }
    
    private fun demo13PerformanceConsiderations() {
        println("\n--- 3.3 Performance Considerations ---")
        
        val testClass = PerformanceTest::class.java
        val instance = testClass.getDeclaredConstructor().newInstance()
        val iterations = 100_000
        
        // Method lookup performance
        println("Method lookup performance ($iterations iterations):")
        
        // Cached method lookup
        val cachedMethod = testClass.getMethod("fastMethod", String::class.java)
        val start1 = System.nanoTime()
        repeat(iterations) {
            cachedMethod.invoke(instance, "test")
        }
        val duration1 = System.nanoTime() - start1
        println("Cached method: ${duration1 / 1_000_000}ms")
        
        // Repeated method lookup
        val start2 = System.nanoTime()
        repeat(iterations) {
            val method = testClass.getMethod("fastMethod", String::class.java)
            method.invoke(instance, "test")
        }
        val duration2 = System.nanoTime() - start2
        println("Repeated lookup: ${duration2 / 1_000_000}ms")
        println("Performance difference: ${duration2 / duration1.toDouble()}x slower")
        
        // MethodHandle comparison (Java 7+)
        val methodHandles = java.lang.invoke.MethodHandles.lookup()
        val methodHandle = methodHandles.findVirtual(
            testClass,
            "fastMethod", 
            java.lang.invoke.MethodType.methodType(String::class.java, String::class.java)
        )
        
        val start3 = System.nanoTime()
        repeat(iterations) {
            methodHandle.invoke(instance, "test")
        }
        val duration3 = System.nanoTime() - start3
        println("MethodHandle: ${duration3 / 1_000_000}ms")
        println("MethodHandle vs Reflection: ${duration1 / duration3.toDouble()}x faster")
        
        // Field access performance
        val field = testClass.getDeclaredField("data")
        field.isAccessible = true
        
        val start4 = System.nanoTime()
        repeat(iterations) {
            field.set(instance, "new value")
            field.get(instance)
        }
        val duration4 = System.nanoTime() - start4
        println("Field access: ${duration4 / 1_000_000}ms")
        
        // Security manager impact simulation
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
    
    private fun demo14SecurityAspects() {
        println("\n--- 3.4 Security Aspects ---")
        
        val secureClass = SecureClass::class.java
        
        try {
            // Attempt to access private field
            val secretField = secureClass.getDeclaredField("secretKey")
            
            println("Found private field: ${secretField.name}")
            println("Field is accessible: ${secretField.isAccessible}")
            
            // This would normally be blocked by SecurityManager
            secretField.isAccessible = true
            println("Made field accessible: ${secretField.isAccessible}")
            
            val instance = secureClass.getDeclaredConstructor().newInstance()
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
        
        // Demonstrate safer reflection patterns
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
    
    private fun demo15AdvancedPatterns() {
        println("\n--- 3.5 Advanced Reflection Patterns ---")
        
        // Builder pattern with reflection
        val builder = ReflectionBuilder(ReflectionPerson::class.java)
        val person = builder
            .set("name", "John Doe")
            .set("age", 30)
            .set("email", "john@example.com")
            .build()
        
        println("Builder pattern result: $person")
        
        // Simple dependency injection
        val container = SimpleDIContainer()
        container.register(DatabaseService::class.java) { DatabaseServiceImpl() }
        container.register(UserService::class.java) { UserServiceImpl() }
        
        val userService = container.get(UserService::class.java)
        userService.createUser("Alice")
        
        // Object mapper pattern
        val mapper = SimpleObjectMapper()
        val userData = mapOf(
            "name" to "Bob",
            "age" to 25,
            "email" to "bob@example.com"
        )
        val mappedPerson = mapper.map(userData, ReflectionPerson::class.java)
        println("Object mapper result: $mappedPerson")
        
        // Reflection-based toString
        val toStringBuilder = ReflectionToStringBuilder()
        val complexObject = ComplexObject()
        println("Reflection toString: ${toStringBuilder.toString(complexObject)}")
    }
    
    // ===== LEVEL 4: EXPERT =====
    private fun runLevel4Expert() {
        println("🧠 LEVEL 4: EXPERT - ClassLoaders, Frameworks, Real-world Patterns")
        println("=".repeat(68))
        
        demo16CustomClassLoader()
        demo17AdvancedFrameworkPatterns()
        demo18SerializationFramework()
        demo19TestingFramework()
        demo20ReflectionBestPractices()
    }
    
    private fun demo16CustomClassLoader() {
        println("\n--- 4.1 Custom ClassLoader ---")
        
        // Simple memory-based class loader
        val customLoader = CustomClassLoader()
        
        // Load a class dynamically
        val dynamicClassSource = """
            public class DynamicClass {
                private String message = "Hello from dynamic class!";
                
                public String getMessage() {
                    return message;
                }
                
                public void setMessage(String message) {
                    this.message = message;
                }
            }
        """.trimIndent()
        
        try {
            // In a real scenario, you'd compile this to bytecode
            println("Custom ClassLoader demonstration:")
            println("ClassLoader: ${customLoader.javaClass.simpleName}")
            println("Parent: ${customLoader.parent?.javaClass?.simpleName}")
            
            // Show class loader functionality
            println("Custom class loader created successfully")
            
            // Show class loader hierarchy
            println("\nClassLoader hierarchy:")
            var currentLoader: ClassLoader? = customLoader
            var level = 0
            while (currentLoader != null) {
                println("${"  ".repeat(level)}${currentLoader.javaClass.simpleName}")
                currentLoader = currentLoader.parent
                level++
            }
            
        } catch (e: Exception) {
            println("ClassLoader demo: ${e.message}")
        }
        
        // Context class loader example
        val originalContextLoader = Thread.currentThread().contextClassLoader
        println("\nOriginal context loader: ${originalContextLoader.javaClass.simpleName}")
        
        Thread.currentThread().contextClassLoader = customLoader
        println("Set custom context loader: ${Thread.currentThread().contextClassLoader.javaClass.simpleName}")
        
        // Restore original
        Thread.currentThread().contextClassLoader = originalContextLoader
    }
    
    private fun demo17AdvancedFrameworkPatterns() {
        println("\n--- 4.2 Advanced Framework Patterns ---")
        
        // Spring-style dependency injection with reflection
        val advancedContainer = AdvancedDIContainer()
        
        // Register beans
        advancedContainer.registerBean("userService", UserServiceAdvanced::class.java)
        advancedContainer.registerBean("emailService", EmailServiceImpl::class.java)
        advancedContainer.registerBean("auditService", AuditServiceImpl::class.java)
        
        // Get bean with dependency injection
        val userService = advancedContainer.getBean("userService", UserServiceAdvanced::class.java)
        userService.createUser("John Doe", "john@example.com")
        
        // AOP-style method interception (only works with interfaces)
        println("\nAOP-style method interception:")
        println("Note: Java dynamic proxies only work with interfaces")
        println("For concrete class interception, you'd need CGLIB or ByteBuddy")
        
        // Create a service that implements an interface for proxy demonstration
        val emailService = advancedContainer.getBean("emailService", EmailService::class.java)
        val interceptor = MethodInterceptor()
        val interceptedEmailService = interceptor.intercept(emailService)
        interceptedEmailService.sendEmail("intercepted@example.com", "Intercepted", "This call was intercepted")
        
        // Configuration binding
        println("\nConfiguration binding:")
        val config = mapOf(
            "database.url" to "jdbc:postgresql://localhost:5432/mydb",
            "database.username" to "admin",
            "database.maxConnections" to 20,
            "cache.enabled" to true,
            "cache.ttl" to 3600
        )
        
        val configBinder = ConfigurationBinder()
        val dbConfig = configBinder.bind(config, DatabaseConfig::class.java, "database")
        val cacheConfig = configBinder.bind(config, CacheConfig::class.java, "cache")
        
        println("Database config: $dbConfig")
        println("Cache config: $cacheConfig")
    }
    
    private fun demo18SerializationFramework() {
        println("\n--- 4.3 Reflection-based Serialization ---")
        
        // Custom JSON serializer using reflection
        val jsonSerializer = ReflectionJsonSerializer()
        
        val user = User(1, "Alice Johnson", "alice@example.com", 28)
        user.address = Address("123 Main St", "Springfield", "USA")
        
        val json = jsonSerializer.serialize(user)
        println("Serialized JSON:")
        println(json)
        
        // Custom JSON deserializer
        val jsonDeserializer = ReflectionJsonDeserializer()
        val deserializedUser = jsonDeserializer.deserialize(json, User::class.java)
        
        println("\nDeserialized object:")
        println("ID: ${deserializedUser.id}")
        println("Name: ${deserializedUser.name}")
        println("Email: ${deserializedUser.email}")
        println("Age: ${deserializedUser.age}")
        println("Address: ${deserializedUser.address}")
        
        // Deep copy using reflection
        println("\nDeep copy example:")
        val deepCopier = ReflectionDeepCopier()
        val userCopy = deepCopier.deepCopy(user)
        
        println("Original: $user")
        println("Copy: $userCopy")
        println("Are same instance: ${user === userCopy}")
        println("Are equal: ${user == userCopy}")
        
        // Modify copy to verify independence
        userCopy.name = "Alice Modified"
        println("After modifying copy:")
        println("Original name: ${user.name}")
        println("Copy name: ${userCopy.name}")
    }
    
    private fun demo19TestingFramework() {
        println("\n--- 4.4 Testing Framework Patterns ---")
        
        // Mock framework using reflection
        val mockFramework = SimpleMockFramework()
        
        // Create a mock
        val mockEmailService = mockFramework.createMock(EmailService::class.java)
        println("Created mock: ${mockEmailService.javaClass.simpleName}")
        
        // Use the mock
        mockEmailService.sendEmail("test@example.com", "Test Subject", "Test Body")
        
        // Verify interactions
        val interactions = mockFramework.getInteractions(mockEmailService)
        println("Mock interactions: ${interactions.size}")
        interactions.forEach { interaction ->
            println("  Called: ${interaction.methodName} with args: ${interaction.args.joinToString()}")
        }
        
        // Test spy framework
        println("\nTest spy example:")
        val realEmailService: EmailService = EmailServiceImpl()
        val spy = mockFramework.createSpy(realEmailService)
        
        spy.sendEmail("spy@example.com", "Spy Test", "This is a spy test")
        
        val spyInteractions = mockFramework.getInteractions(spy)
        println("Spy interactions: ${spyInteractions.size}")
        
        // Assertion framework
        println("\nAssertion framework:")
        val assertionFramework = ReflectionAssertions()
        
        val testObject = TestData("test", 42, listOf("a", "b", "c"))
        
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
    
    private fun demo20ReflectionBestPractices() {
        println("\n--- 4.5 Best Practices & Performance Optimization ---")
        
        // Reflection cache demonstration
        val reflectionCache = ReflectionCache()
        val testClass = PerformanceTest::class.java
        
        val iterations = 10_000
        
        // Cached reflection
        val start1 = System.nanoTime()
        repeat(iterations) {
            val method = reflectionCache.getMethod(testClass, "fastMethod", String::class.java)
            // method would be used here
        }
        val duration1 = System.nanoTime() - start1
        println("Cached reflection lookups ($iterations): ${duration1 / 1_000_000}ms")
        
        // Non-cached reflection
        val start2 = System.nanoTime()
        repeat(iterations) {
            val method = testClass.getMethod("fastMethod", String::class.java)
            // method would be used here
        }
        val duration2 = System.nanoTime() - start2
        println("Non-cached reflection lookups ($iterations): ${duration2 / 1_000_000}ms")
        println("Cache performance improvement: ${duration2 / duration1.toDouble()}x faster")
        
        // Memory considerations
        println("\nMemory considerations:")
        val memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Create many reflection objects
        val methods = mutableListOf<Method>()
        repeat(1000) {
            methods.add(testClass.getMethod("fastMethod", String::class.java))
        }
        
        val memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println("Memory used by reflection objects: ${(memAfter - memBefore) / 1024}KB")
        
        // Error handling best practices
        println("\nError handling examples:")
        
        val errorHandler = ReflectionErrorHandler()
        
        // Safe field access
        val result1 = errorHandler.safeGetField(PerformanceTest(), "nonExistentField")
        println("Safe field access result: $result1")
        
        // Safe method invocation
        val result2 = errorHandler.safeInvokeMethod(PerformanceTest(), "nonExistentMethod", emptyArray())
        println("Safe method invocation result: $result2")
        
        // Type safety checks
        val typeSafety = ReflectionTypeSafety()
        println("Type safety examples:")
        println("Can assign String to Object: ${typeSafety.canAssign(String::class.java, Any::class.java)}")
        println("Can assign Object to String: ${typeSafety.canAssign(Any::class.java, String::class.java)}")
        println("Is primitive: ${typeSafety.isPrimitive(Int::class.java)}")
        
        // Final recommendations
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

// ===== SAMPLE CLASSES FOR REFLECTION EXPERIMENTS =====

class SamplePerson {
    var name: String = ""
    var age: Int = 0
    private var secret: String = ""
    
    constructor() {
        this.name = "Unknown"
        this.age = 0
        this.secret = "default"
    }
    
    constructor(name: String) {
        this.name = name
        this.age = 18
        this.secret = "none"
    }
    
    constructor(name: String, age: Int, secret: String) {
        this.name = name
        this.age = age
        this.secret = secret
    }
    
    fun getInfo(): String = "Person(name=$name, age=$age)"
    
    private fun privateMethod(): String = "This is private: $secret"
}

object ReflectionUtils {
    @JvmStatic
    fun staticUtility(input: String): String = "Processed: $input"
}

// ===== LEVEL 2 SAMPLE CLASSES =====

// Generic types examples
class GenericContainer {
    val stringList: List<String> = listOf("a", "b", "c")
    val numberMap: Map<String, Int> = mapOf("one" to 1, "two" to 2)
    
    fun <T> processMap(map: Map<String, T>): List<T> {
        return map.values.toList()
    }
}

class GenericClass<T : Number, U : Comparable<U>> {
    fun process(item: T): U? = null
}

// Annotations
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Entity(val name: String, val tableName: String = "")

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val name: String, val nullable: Boolean = true)

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotNull(val message: String = "Field cannot be null")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Transactional

// Annotated sample class
@Entity(name = "User", tableName = "users")
class AnnotatedSample {
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "ID is required")
    var id: Long = 0
    
    @Column(name = "user_name")
    var name: String = ""
    
    @Column(name = "email", nullable = false)
    @NotNull
    var email: String = ""
    
    @Transactional
    fun validateData(): Boolean = true
    
    fun updateField(@NotNull(message = "Value required") value: String) {
        name = value
    }
}

// Array examples
class ArrayContainer {
    val numbers: IntArray = intArrayOf(1, 2, 3, 4, 5)
    val genericList: Array<List<String>> = arrayOf(listOf("a"), listOf("b"))
}

// Nested classes example
class OuterClass(private val data: String) {
    
    val anonymousRunnable = object : Runnable {
        override fun run() {
            println("Anonymous runnable with $data")
        }
    }
    
    // Static nested class
    class StaticNested {
        fun getMessage(): String = "Static nested class"
    }
    
    // Inner class (non-static)
    inner class InnerClass {
        fun accessOuter(): String = "Inner class accessing: $data"
    }
}

// Access control examples
class RestrictedClass {
    private val privateData: String = "secret"
    private val finalValue: String = "immutable"
    
    private fun privateCalculation(x: Int): Int = x * x + 10
    
    companion object {
        private fun staticPrivateUtility(): String = "Static secret utility"
    }
}

// ===== LEVEL 3 SAMPLE CLASSES =====

// Dynamic Proxy examples
interface Service {
    fun doWork(data: String): String
    fun calculate(a: Int, b: Int): Int
}

class ServiceImpl : Service {
    override fun doWork(data: String): String {
        return "Processed: $data"
    }
    
    override fun calculate(a: Int, b: Int): Int {
        return a + b
    }
}

// Advanced Annotations
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

// Annotation Processor
class AnnotationProcessor {
    fun process(instance: Any) {
        val clazz = instance.javaClass
        
        println("Processing annotations for: ${clazz.simpleName}")
        
        // Process class annotations
        clazz.annotations.forEach { annotation ->
            when (annotation) {
                is Cacheable -> println("  Class cached with timeout: ${annotation.timeout}")
                else -> println("  Class annotation: $annotation")
            }
        }
        
        // Process method annotations
        clazz.methods.forEach { method ->
            method.annotations.forEach { annotation ->
                when (annotation) {
                    is Cacheable -> println("  Method ${method.name} cached with timeout: ${annotation.timeout}")
                    is Audited -> println("  Method ${method.name} audited at level: ${annotation.level}")
                    else -> println("  Method ${method.name} annotation: $annotation")
                }
            }
        }
    }
}

// Performance Testing
class PerformanceTest {
    var data: String = "test data"
    
    fun fastMethod(input: String): String = "Result: $input"
}

// Security Examples
class SecureClass {
    private val secretKey: String = "top-secret-key-123"
    val publicData: String = "public information"
}

object SafeReflectionUtils {
    fun getFieldValue(instance: Any, fieldName: String): Any? {
        val clazz = instance.javaClass
        return try {
            val field = clazz.getDeclaredField(fieldName)
            
            // Only access public fields
            if (!Modifier.isPublic(field.modifiers)) {
                throw IllegalAccessException("Field $fieldName is not public")
            }
            
            field.get(instance)
        } catch (e: NoSuchFieldException) {
            throw IllegalArgumentException("Field $fieldName not found in ${clazz.simpleName}")
        }
    }
}

// Advanced Patterns

// Reflection Builder Pattern
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

// Simple Dependency Injection
class SimpleDIContainer {
    private val registry = mutableMapOf<Class<*>, () -> Any>()
    private val instances = mutableMapOf<Class<*>, Any>()
    
    fun <T> register(type: Class<T>, factory: () -> T) {
        @Suppress("UNCHECKED_CAST")
        registry[type] = factory as () -> Any
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: Class<T>): T {
        return instances.getOrPut(type) {
            val factory = registry[type] ?: throw IllegalArgumentException("Type not registered: ${type.simpleName}")
            
            val instance = factory()
            
            // Inject dependencies
            injectDependencies(instance)
            
            instance
        } as T
    }
    
    private fun injectDependencies(instance: Any) {
        val clazz = instance.javaClass
        
        clazz.declaredFields.forEach { field ->
            if (field.isAnnotationPresent(Inject::class.java)) {
                field.isAccessible = true
                val dependency = get(field.type)
                field.set(instance, dependency)
            }
        }
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

// Services for DI example
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

// Object Mapper Pattern
class SimpleObjectMapper {
    fun <T> map(data: Map<String, Any>, targetClass: Class<T>): T {
        val instance = targetClass.getDeclaredConstructor().newInstance()
        
        data.forEach { (key, value) ->
            try {
                val field = targetClass.getDeclaredField(key)
                field.isAccessible = true
                
                // Simple type conversion
                val convertedValue = when {
                    field.type == value.javaClass -> value
                    field.type == Int::class.java && value is Number -> value.toInt()
                    field.type == String::class.java -> value.toString()
                    else -> value
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

// Data classes for examples
data class ReflectionPerson(
    var name: String = "",
    var age: Int = 0,
    var email: String = ""
)

class ComplexObject {
    val id: Long = 12345
    val name: String = "Complex"
    private val secret: String = "hidden"
    val list: List<String> = listOf("a", "b", "c")
}

// Reflection toString builder
class ReflectionToStringBuilder {
    fun toString(obj: Any): String {
        val clazz = obj.javaClass
        val fields = mutableListOf<String>()
        
        clazz.declaredFields.forEach { field ->
            field.isAccessible = true
            try {
                val value = field.get(obj)
                fields.add("${field.name}=$value")
            } catch (e: Exception) {
                fields.add("${field.name}=<inaccessible>")
            }
        }
        
        return "${clazz.simpleName}(${fields.joinToString(", ")})"
    }
}

// ===== LEVEL 4 EXPERT SAMPLE CLASSES =====

// Custom ClassLoader
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

// Advanced DI Container
class AdvancedDIContainer {
    private val beanDefinitions = mutableMapOf<String, BeanDefinition>()
    private val singletons = mutableMapOf<String, Any>()
    
    fun registerBean(name: String, clazz: Class<*>) {
        beanDefinitions[name] = BeanDefinition(name, clazz)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> getBean(name: String, expectedType: Class<T>): T {
        return singletons.getOrPut(name) {
            val beanDef = beanDefinitions[name] 
                ?: throw IllegalArgumentException("Bean $name not found")
            
            val instance = createInstance(beanDef.clazz)
            injectDependencies(instance)
            invokePostConstruct(instance)
            instance
        } as T
    }
    
    private fun createInstance(clazz: Class<*>): Any {
        return clazz.getDeclaredConstructor().newInstance()
    }
    
    private fun injectDependencies(instance: Any) {
        val clazz = instance.javaClass
        
        clazz.declaredFields.forEach { field ->
            if (field.isAnnotationPresent(AutowiredAdvanced::class.java)) {
                field.isAccessible = true
                
                val fieldType = field.type
                val beanName = findBeanNameByType(fieldType)
                if (beanName != null) {
                    val dependency = getBean(beanName, fieldType)
                    field.set(instance, dependency)
                }
            }
        }
    }
    
    private fun invokePostConstruct(instance: Any) {
        val clazz = instance.javaClass
        
        clazz.declaredMethods.forEach { method ->
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

// Advanced services
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

// Method Interceptor (AOP-style)
class MethodInterceptor {
    fun <T> intercept(target: T): T {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                println("🔍 Before ${method?.name}")
                val start = System.nanoTime()
                
                val result = method?.invoke(target, *(args ?: emptyArray()))
                
                val duration = System.nanoTime() - start
                println("⏱️  ${method?.name} took ${duration / 1_000_000}ms")
                
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

// Configuration Binding
class ConfigurationBinder {
    fun <T> bind(properties: Map<String, Any>, targetClass: Class<T>, prefix: String): T {
        val instance = targetClass.getDeclaredConstructor().newInstance()
        
        targetClass.declaredFields.forEach { field ->
            val propertyKey = "$prefix.${field.name}"
            val value = properties[propertyKey]
            
            if (value != null) {
                field.isAccessible = true
                val convertedValue = convertValue(value, field.type)
                field.set(instance, convertedValue)
            }
        }
        
        return instance
    }
    
    private fun convertValue(value: Any, targetType: Class<*>): Any {
        return when {
            targetType.isAssignableFrom(value.javaClass) -> value
            targetType == Boolean::class.java && value is String -> value.toBoolean()
            targetType == Int::class.java && value is Number -> value.toInt()
            targetType == Long::class.java && value is Number -> value.toLong()
            targetType == String::class.java -> value.toString()
            else -> value
        }
    }
}

data class DatabaseConfig(
    var url: String = "",
    var username: String = "",
    var maxConnections: Int = 0
)

data class CacheConfig(
    var enabled: Boolean = false,
    var ttl: Int = 0
)

// Serialization Framework
class ReflectionJsonSerializer {
    fun serialize(obj: Any): String {
        return serializeObject(obj)
    }
    
    private fun serializeObject(obj: Any): String {
        val clazz = obj.javaClass
        val fields = mutableListOf<String>()
        
        clazz.declaredFields.forEach { field ->
            field.isAccessible = true
            val value = field.get(obj)
            val serializedValue = when (value) {
                null -> "null"
                is String -> "\"$value\""
                is Number, is Boolean -> value.toString()
                else -> serializeObject(value)
            }
            fields.add("\"${field.name}\": $serializedValue")
        }
        
        return "{${fields.joinToString(", ")}}"
    }
}

class ReflectionJsonDeserializer {
    fun <T> deserialize(json: String, targetClass: Class<T>): T {
        // Simplified JSON parsing - in reality you'd use a proper JSON parser
        val instance = targetClass.getDeclaredConstructor().newInstance()
        
        val fieldPattern = """"([^"]+)":\s*([^,}]+)""".toRegex()
        val matches = fieldPattern.findAll(json)
        
        matches.forEach { match ->
            val fieldName = match.groupValues[1]
            val fieldValue = match.groupValues[2].trim()
            
            try {
                val field = targetClass.getDeclaredField(fieldName)
                field.isAccessible = true
                
                val convertedValue = when (field.type) {
                    String::class.java -> fieldValue.removeSurrounding("\"")
                    Int::class.java -> fieldValue.toInt()
                    Long::class.java -> fieldValue.toLong()
                    Boolean::class.java -> fieldValue.toBoolean()
                    else -> null
                }
                
                if (convertedValue != null) {
                    field.set(instance, convertedValue)
                }
            } catch (e: Exception) {
                // Field not found or conversion error
            }
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
            val value = field.get(obj)
            
            val copiedValue = when {
                value == null -> null
                isPrimitive(value.javaClass) -> value
                value is String -> value // Strings are immutable
                else -> deepCopy(value) // Recursive copy
            }
            
            field.set(copy, copiedValue)
        }
        
        return copy
    }
    
    private fun isPrimitive(clazz: Class<*>): Boolean {
        return clazz.isPrimitive || 
               clazz == java.lang.Boolean::class.java ||
               clazz == java.lang.Integer::class.java ||
               clazz == java.lang.Long::class.java ||
               clazz == java.lang.Double::class.java ||
               clazz == java.lang.Float::class.java
    }
}

// User and Address for serialization example
data class User(
    var id: Long = 0,
    var name: String = "",
    var email: String = "",
    var age: Int = 0,
    var address: Address? = null
)

data class Address(
    var street: String = "",
    var city: String = "",
    var country: String = ""
)

// Mock Framework  
class SimpleMockFramework {
    private val mockInteractions = mutableMapOf<Int, MutableList<MethodInteraction>>()
    
    fun <T> createMock(interfaceClass: Class<T>): T {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                // Skip recording for Object methods to avoid infinite recursion
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
                // Skip recording for Object methods to avoid infinite recursion
                if (method?.name in listOf("hashCode", "equals", "toString")) {
                    return method!!.invoke(realObject, *(args ?: emptyArray()))
                }
                recordInteraction(proxy!!, method!!, args ?: emptyArray())
                return method.invoke(realObject, *(args ?: emptyArray()))
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
        val key = System.identityHashCode(proxy)
        val interactions = mockInteractions.getOrPut(key) { mutableListOf() }
        interactions.add(MethodInteraction(method.name, args.toList()))
    }
    
    private fun getDefaultReturn(returnType: Class<*>): Any? {
        return when {
            returnType == Void.TYPE -> null
            returnType.isPrimitive -> when (returnType) {
                Boolean::class.java -> false
                Int::class.java -> 0
                Long::class.java -> 0L
                Double::class.java -> 0.0
                Float::class.java -> 0.0f
                else -> null
            }
            else -> null
        }
    }
}

data class MethodInteraction(val methodName: String, val args: List<Any>)

// Assertion Framework
class ReflectionAssertions {
    fun assertField(obj: Any, fieldName: String, expected: Any?) {
        val clazz = obj.javaClass
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        val actual = field.get(obj)
        
        if (actual != expected) {
            throw AssertionError("Expected field $fieldName to be $expected, but was $actual")
        }
    }
    
    fun assertMethodResult(obj: Any, methodName: String, args: Array<Any>, expected: Any?) {
        val clazz = obj.javaClass
        val paramTypes = args.map { it.javaClass }.toTypedArray()
        val method = clazz.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        val actual = method.invoke(obj, *args)
        
        if (actual != expected) {
            throw AssertionError("Expected method $methodName to return $expected, but was $actual")
        }
    }
}

data class TestData(
    val name: String,
    val number: Int,
    val list: List<String>
)

// Performance and Best Practices
class ReflectionCache {
    private val methodCache = mutableMapOf<MethodKey, Method>()
    private val fieldCache = mutableMapOf<FieldKey, Field>()
    
    fun getMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method {
        val key = MethodKey(clazz, name, paramTypes.toList())
        return methodCache.getOrPut(key) {
            clazz.getMethod(name, *paramTypes)
        }
    }
    
    fun getField(clazz: Class<*>, name: String): Field {
        val key = FieldKey(clazz, name)
        return fieldCache.getOrPut(key) {
            clazz.getDeclaredField(name)
        }
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
            println("Field $fieldName not found")
            null
        } catch (e: IllegalAccessException) {
            println("Cannot access field $fieldName")
            null
        } catch (e: Exception) {
            println("Error accessing field $fieldName: ${e.message}")
            null
        }
    }
    
    fun safeInvokeMethod(obj: Any, methodName: String, args: Array<Any>): Any? {
        return try {
            val paramTypes = args.map { it.javaClass }.toTypedArray()
            val method = obj.javaClass.getDeclaredMethod(methodName, *paramTypes)
            method.isAccessible = true
            method.invoke(obj, *args)
        } catch (e: NoSuchMethodException) {
            println("Method $methodName not found")
            null
        } catch (e: IllegalAccessException) {
            println("Cannot access method $methodName")
            null
        } catch (e: Exception) {
            println("Error invoking method $methodName: ${e.message}")
            null
        }
    }
}

class ReflectionTypeSafety {
    fun canAssign(from: Class<*>, to: Class<*>): Boolean {
        return to.isAssignableFrom(from)
    }
    
    fun isPrimitive(clazz: Class<*>): Boolean {
        return clazz.isPrimitive || WRAPPER_TO_PRIMITIVE.containsKey(clazz)
    }
    
    companion object {
        private val WRAPPER_TO_PRIMITIVE = mapOf(
            java.lang.Boolean::class.java to Boolean::class.java,
            java.lang.Integer::class.java to Int::class.java,
            java.lang.Long::class.java to Long::class.java,
            java.lang.Double::class.java to Double::class.java,
            java.lang.Float::class.java to Float::class.java,
            java.lang.Character::class.java to Char::class.java,
            java.lang.Byte::class.java to Byte::class.java,
            java.lang.Short::class.java to Short::class.java
        )
    }
}