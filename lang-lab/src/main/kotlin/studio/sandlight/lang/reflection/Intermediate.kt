package studio.sandlight.lang.reflection

import studio.sandlight.lang.support.Lab

// LEVEL 2: Intermediate — 泛型、注解、数组、嵌套类

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

object Intermediate {

    fun run() {

        demo21GenericTypes()
        demo22Annotations()
        demo23ArrayManipulation()
        demo24NestedClasses()
        demo25AccessControl()
    }

    // ──────────────────────────────────────────────────────────────
    // 2.1 泛型与类型擦除
    //
    // 泛型在 JVM 的生命周期：
    //
    //   源代码：       List<String>
    //       │
    //   编译阶段：     类型检查通过，生成字节码时"擦除"类型参数
    //       │          但类型信息以 Signature 属性存入 .class 文件
    //       │
    //   .class 文件：  方法/字段描述符 = "Ljava/util/List;"  (已擦除)
    //                  Signature 属性  = "Ljava/util/List<Ljava/lang/String;>;"
    //       │
    //   运行时：       Field.getGenericType()
    //                    → 读取 Signature 属性
    //                    → 返回 ParameterizedType（而非裸 Class）
    //
    // ParameterizedType 层次：
    //   Type
    //   └── ParameterizedType
    //         ├── getRawType()            → List.class
    //         └── getActualTypeArguments() → [String.class]
    //
    // TypeVariable（类定义上的 T）：
    //   Type
    //   └── TypeVariable<D>
    //         ├── getName()   → "T"
    //         └── getBounds() → [Object.class]  （无 extends 时默认）
    //
    // 阅读 JDK 源码建议：
    //   java.base/java/lang/reflect/Field.java          → getGenericType()
    //   java.base/sun/reflect/generics/reflectiveObjects/ParameterizedTypeImpl.java
    //   java.base/sun/reflect/generics/parser/SignatureParser.java  ← 解析 Signature 属性
    // ──────────────────────────────────────────────────────────────
    private fun demo21GenericTypes() {
        Lab.section("2.1", "Generic Type Information")

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

        // 带泛型参数的方法
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

        // TypeVariable：class GenericClass<T : Number, U : Comparable<U>>
        val clazz = GenericClass::class.java
        val typeParameters = clazz.typeParameters
        println("\nGeneric class type parameters:")
        typeParameters.forEach { typeVar ->
            println("  ${typeVar.name}: bounds=${typeVar.bounds.joinToString()}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2.2 注解处理
    //
    // 注解的保留策略（RetentionPolicy）：
    //   SOURCE   → 编译后丢弃，不进入 .class
    //   CLASS    → 进入 .class，但 JVM 加载时不读取（默认）
    //   RUNTIME  → JVM 加载后通过 reflection 可读 ← 本节重点
    //
    // 阅读：java.base/java/lang/annotation/Retention.java
    //       java.base/java/lang/reflect/AnnotatedElement.java  ← 获取注解的公共接口
    // ──────────────────────────────────────────────────────────────
    private fun demo22Annotations() {
        Lab.section("2.2", "Annotations Processing")

        val clazz = AnnotatedSample::class.java

        println("Class annotations:")
        clazz.annotations.forEach { annotation ->
            println("  $annotation")
        }

        val entityAnnotation = clazz.getAnnotation(Entity::class.java)
        if (entityAnnotation != null) {
            println("Entity name: ${entityAnnotation.name}")
            println("Entity table: ${entityAnnotation.tableName}")
        }

        println("\nField annotations:")
        clazz.declaredFields.forEach { field ->
            if (field.annotations.isNotEmpty()) {
                println("Field ${field.name}:")
                field.annotations.forEach { annotation ->
                    println("  $annotation")

                    when (annotation) {
                        is Column  -> {
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

        println("\nMethod annotations:")
        val validateMethod = clazz.getDeclaredMethod("validateData")
        validateMethod.annotations.forEach { annotation ->
            println("  $annotation")
        }

        // 参数注解
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

    // ──────────────────────────────────────────────────────────────
    // 2.3 数组操作
    //
    // java.lang.reflect.Array 是操作数组的反射工具类：
    //   - newInstance(componentType, length) → 动态创建数组
    //   - get(array, index) / set(array, index, value)
    //   - getLength(array)
    //
    // 阅读：java.base/java/lang/reflect/Array.java（大量 native 方法）
    // ──────────────────────────────────────────────────────────────
    private fun demo23ArrayManipulation() {
        Lab.section("2.3", "Array Manipulation")

        val intArray    = java.lang.reflect.Array.newInstance(Int::class.java, 5) as IntArray
        val stringArray = java.lang.reflect.Array.newInstance(String::class.java, 3) as Array<String>

        java.lang.reflect.Array.set(intArray, 0, 10)
        java.lang.reflect.Array.set(intArray, 1, 20)
        java.lang.reflect.Array.set(stringArray, 0, "Hello")
        java.lang.reflect.Array.set(stringArray, 1, "World")

        println("Int array length: ${java.lang.reflect.Array.getLength(intArray)}")
        println("Int array[0]: ${java.lang.reflect.Array.get(intArray, 0)}")
        println("Int array[1]: ${java.lang.reflect.Array.get(intArray, 1)}")
        println("String array[0]: ${java.lang.reflect.Array.get(stringArray, 0)}")

        val dimensions  = intArrayOf(2, 3)
        val multiArray  = java.lang.reflect.Array.newInstance(Int::class.java, *dimensions) as Array<IntArray>
        val firstRow    = java.lang.reflect.Array.get(multiArray, 0) as IntArray
        java.lang.reflect.Array.set(firstRow, 0, 100)
        java.lang.reflect.Array.set(firstRow, 1, 200)
        println("Multi-array[0][0]: ${java.lang.reflect.Array.get(firstRow, 0)}")
        println("Multi-array[0][1]: ${java.lang.reflect.Array.get(firstRow, 1)}")

        val arrayField = ArrayContainer::class.java.getDeclaredField("numbers")
        val arrayType  = arrayField.type
        println("\nArray field type: $arrayType")
        println("Is array: ${arrayType.isArray}")
        println("Component type: ${arrayType.componentType}")

        val genericArrayField = ArrayContainer::class.java.getDeclaredField("genericList")
        val genericArrayType  = genericArrayField.genericType
        println("Generic array type: $genericArrayType")
        if (genericArrayType is GenericArrayType) {
            println("Generic component type: ${genericArrayType.genericComponentType}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2.4 嵌套类与内部类
    //
    // 内部类（non-static inner class）的字节码中隐含一个 outer$ 引用：
    //
    //   OuterClass$InnerClass 的构造器实际签名：
    //     <init>(OuterClass outer)
    //
    // 所以通过反射创建内部类实例时必须传入外部类实例。
    //
    // 阅读：java.base/java/lang/Class.java → getDeclaredClasses()
    //                                       → getEnclosingClass()
    //                                       → isMemberClass() / isAnonymousClass()
    // ──────────────────────────────────────────────────────────────
    private fun demo24NestedClasses() {
        Lab.section("2.4", "Nested and Inner Classes")

        val outerClass = OuterClass::class.java

        println("Declared classes in OuterClass:")
        outerClass.declaredClasses.forEach { nestedClass ->
            println("  ${nestedClass.simpleName} (${Modifier.toString(nestedClass.modifiers)})")
        }

        val staticNestedClass = OuterClass.StaticNested::class.java
        println("\nStatic nested class: ${staticNestedClass.simpleName}")
        println("Enclosing class: ${staticNestedClass.enclosingClass?.simpleName}")
        println("Is member class: ${staticNestedClass.isMemberClass}")

        val staticInstance = staticNestedClass.getDeclaredConstructor().newInstance()
        val staticMethod   = staticNestedClass.getDeclaredMethod("getMessage")
        println("Static nested method result: ${staticMethod.invoke(staticInstance)}")

        val innerClass = OuterClass.InnerClass::class.java
        println("\nInner class: ${innerClass.simpleName}")
        println("Enclosing class: ${innerClass.enclosingClass?.simpleName}")

        val outerInstance   = OuterClass("outer-data")
        val innerConstructor = innerClass.getDeclaredConstructor(OuterClass::class.java)
        val innerInstance   = innerConstructor.newInstance(outerInstance)
        val innerMethod     = innerClass.getDeclaredMethod("accessOuter")
        println("Inner class result: ${innerMethod.invoke(innerInstance)}")

        val anonymousField    = OuterClass::class.java.getDeclaredField("anonymousRunnable")
        anonymousField.isAccessible = true
        val anonymousInstance = anonymousField.get(outerInstance)
        val anonymousClass    = anonymousInstance.javaClass
        println("\nAnonymous class: ${anonymousClass.simpleName}")
        println("Is anonymous: ${anonymousClass.isAnonymousClass}")
        println("Enclosing class: ${anonymousClass.enclosingClass?.simpleName}")
        println("Enclosing method: ${anonymousClass.enclosingMethod?.name}")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.5 访问控制
    //
    // setAccessible(true) 的实质：
    //   绕过 Java 访问检查，不改变字段/方法本身的修饰符。
    //   在 Java 9+ 模块系统下，跨模块的 open 权限额外受限：
    //     --add-opens java.base/java.lang.reflect=ALL-UNNAMED
    //
    // final 字段修改：
    //   Java 17+ 已彻底封堵通过 Field.modifiers hack 修改 final 字段的路径。
    //   (JEP 396 / JEP 403 强封装 JDK 内部 API)
    //
    // 阅读：java.base/java/lang/reflect/AccessibleObject.java
    //       → checkAccess() / setAccessible()
    // ──────────────────────────────────────────────────────────────
    private fun demo25AccessControl() {
        Lab.section("2.5", "Advanced Access Control")

        val restrictedClass = RestrictedClass::class.java
        val instance        = restrictedClass.getDeclaredConstructor().newInstance()

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

        val privateMethod = restrictedClass.getDeclaredMethod("privateCalculation", Int::class.java)
        privateMethod.isAccessible = true
        val result = privateMethod.invoke(instance, 5)
        println("Private method result: $result")

        val finalField = restrictedClass.getDeclaredField("finalValue")
        finalField.isAccessible = true
        println("Original final value: ${finalField.get(instance)}")

        try {
            val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(finalField, finalField.modifiers and Modifier.FINAL.inv())
            finalField.set(instance, "modified")
            println("✅ Modified final value: ${finalField.get(instance)}")
        } catch (e: Exception) {
            println("❌ Cannot modify final field (blocked by JVM): ${e.javaClass.simpleName}")
            println("   Modern JVMs prevent modification of final fields for security")
        }

        try {
            val companionClass       = Class.forName("${restrictedClass.name}\$Companion")
            val staticPrivateMethod  = companionClass.getDeclaredMethod("staticPrivateUtility")
            staticPrivateMethod.isAccessible = true
            val companionInstance    = restrictedClass.getDeclaredField("Companion").get(null)
            val staticResult         = staticPrivateMethod.invoke(companionInstance)
            println("Static private method result: $staticResult")
        } catch (e: Exception) {
            println("❌ Companion object method access failed: ${e.javaClass.simpleName}")
            println("   Kotlin companion objects have complex reflection access patterns")
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 2 支撑类
// ══════════════════════════════════════════════════════════════════

class GenericContainer {
    val stringList: List<String> = listOf("a", "b", "c")
    val numberMap:  Map<String, Int> = mapOf("one" to 1, "two" to 2)

    fun <T> processMap(map: Map<String, T>): List<T> = map.values.toList()
}

class GenericClass<T : Number, U : Comparable<U>> {
    fun process(item: T): U? = null
}

// 注解定义
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

class ArrayContainer {
    val numbers:     IntArray           = intArrayOf(1, 2, 3, 4, 5)
    val genericList: Array<List<String>> = arrayOf(listOf("a"), listOf("b"))
}

class OuterClass(private val data: String) {

    val anonymousRunnable = object : Runnable {
        override fun run() {
            println("Anonymous runnable with $data")
        }
    }

    class StaticNested {
        fun getMessage(): String = "Static nested class"
    }

    inner class InnerClass {
        fun accessOuter(): String = "Inner class accessing: $data"
    }
}

class RestrictedClass {
    private val privateData: String  = "secret"
    private val finalValue:  String  = "immutable"

    private fun privateCalculation(x: Int): Int = x * x + 10

    companion object {
        private fun staticPrivateUtility(): String = "Static secret utility"
    }
}
