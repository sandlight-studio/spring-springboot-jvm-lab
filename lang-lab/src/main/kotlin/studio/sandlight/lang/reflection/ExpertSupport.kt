package studio.sandlight.lang.reflection

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.time.measureTimedValue

// Support types for the Expert (level 4) reflection demos:
// classloader, DI/config/serialization/mock mini-frameworks, caches.
// Kept in the same package so demo code reads unqualified names.

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
                val (result, duration) = measureTimedValue {
                    method?.invoke(target, *(args ?: emptyArray()))
                }
                println("⏱️  ${method?.name} took ${duration.inWholeMilliseconds}ms")
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
