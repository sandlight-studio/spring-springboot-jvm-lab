package studio.sandlight.lang.reflection

import java.lang.reflect.Method
import java.lang.reflect.Modifier

// Support types for the Advanced (level 3) reflection demos:
// proxy targets, annotated classes, a mini DI container, and mappers.
// Kept in the same package so demo code reads unqualified names.

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
