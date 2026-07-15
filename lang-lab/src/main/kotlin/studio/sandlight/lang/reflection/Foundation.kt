package studio.sandlight.lang.reflection

import studio.sandlight.lang.support.Lab

// LEVEL 1: Foundation — Class 对象、字段、方法、构造器

import java.lang.reflect.Modifier

object Foundation {

    fun run() {

        demo11ClassObjects()
        demo12ClassInformation()
        demo13FieldAccess()
        demo14MethodInvocation()
        demo15ConstructorAccess()
    }

    // ──────────────────────────────────────────────────────────────
    // 1.1 获取 Class 对象
    //
    // JVM Class 对象模型：
    //
    //   "Hello".javaClass        ──┐
    //   String::class.java       ──┼──► Class<String>  (同一个 ClassLoader 下唯一)
    //   Class.forName("j.l.S")  ──┘
    //
    // 三种方式得到的是同一个对象（===）。
    // 原因：JVM 在加载一个类时只创建一个 Class 实例，存放在方法区。
    //
    // 阅读 JDK 源码建议：
    //   java.base/java/lang/Class.java → forName() / forName0() (native)
    //   java.base/java/lang/ClassLoader.java → loadClass()
    // ──────────────────────────────────────────────────────────────
    private fun demo11ClassObjects() {
        Lab.section("1.1", "Getting Class Objects")

        val stringClass1 = Class.forName("java.lang.String")
        println("Class.forName: ${stringClass1.name}")

        val stringClass2 = String::class.java
        println(".class literal: ${stringClass2.name}")

        val str = "Hello"
        val stringClass3 = str.javaClass
        println("getClass(): ${stringClass3.name}")

        println("All three are identical: ${stringClass1 === stringClass2 && stringClass2 === stringClass3}")

        // Kotlin 的 Int::class.java 是 int（原始类型）
        // java.lang.Integer.TYPE 也是 int 原始类型 — 两者相同
        val intClass = Int::class.java
        val intPrimitive = java.lang.Integer.TYPE
        println("int.class: ${intClass.name}")
        println("Integer.TYPE: ${intPrimitive.name}")
        println("Are they same? ${intClass === intPrimitive}")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.2 读取类元数据
    //
    // Modifier 位掩码：public=1, private=2, protected=4, static=8,
    //                  final=16, synchronized=32 …
    // 阅读：java.base/java/lang/reflect/Modifier.java
    // ──────────────────────────────────────────────────────────────
    private fun demo12ClassInformation() {
        Lab.section("1.2", "Basic Class Information")

        val clazz = SamplePerson::class.java

        println("Simple name: ${clazz.simpleName}")
        println("Canonical name: ${clazz.canonicalName}")
        println("Package: ${clazz.`package`?.name}")

        val modifiers = clazz.modifiers
        println("Modifiers: ${Modifier.toString(modifiers)}")
        println("Is public? ${Modifier.isPublic(modifiers)}")
        println("Is final? ${Modifier.isFinal(modifiers)}")

        println("Superclass: ${clazz.superclass?.simpleName}")
        println("Interfaces: ${clazz.interfaces.map { it.simpleName }}")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.3 字段访问
    //
    // Field 对象的内部结构（简化）：
    //
    //   Field
    //   ├── name          : String      字段名
    //   ├── type          : Class<?>    声明类型
    //   ├── modifiers     : int         访问标志位
    //   ├── isAccessible  : boolean     是否跳过访问检查
    //   ├── get(obj)      → Object      读取值
    //   └── set(obj,val)              写入值
    //
    // 阅读：java.base/java/lang/reflect/Field.java
    //       → setAccessible() 实际调用 AccessibleObject.checkAccess()
    // ──────────────────────────────────────────────────────────────
    private fun demo13FieldAccess() {
        Lab.section("1.3", "Field Access and Modification")

        val person = SamplePerson("John", 25, "secret")
        val clazz = person.javaClass

        println("All declared fields:")
        clazz.declaredFields.forEach { field ->
            println("  ${Modifier.toString(field.modifiers)} ${field.type.simpleName} ${field.name}")
        }

        val nameField = clazz.getDeclaredField("name")
        nameField.isAccessible = true
        println("\nOriginal name: ${nameField.get(person)}")
        nameField.set(person, "Jane")
        println("Modified name: ${nameField.get(person)}")

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

        println("\nField types:")
        clazz.declaredFields.forEach { field ->
            println("  ${field.name} -> ${field.type.name}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 1.4 方法调用
    //
    // Method.invoke() 调用链（简化）：
    //
    //   method.invoke(obj, args)
    //       │
    //       └── MethodAccessor.invoke()   ← 前 15 次走字节码解释器
    //               │                       第 16 次生成专用 accessor 类
    //               └── NativeMethodAccessor / GeneratedMethodAccessor
    //                       │
    //                       └── 实际 JNI / 字节码调用
    //
    // 阅读：java.base/java/lang/reflect/Method.java
    //       → invoke() → acquireMethodAccessor() → MethodAccessorImpl
    // ──────────────────────────────────────────────────────────────
    private fun demo14MethodInvocation() {
        Lab.section("1.4", "Method Invocation")

        val person = SamplePerson("Alice", 30, "hidden")
        val clazz = person.javaClass

        println("All declared methods:")
        clazz.declaredMethods.forEach { method ->
            val params = method.parameterTypes.map { it.simpleName }.joinToString(", ")
            println("  ${method.name}($params) -> ${method.returnType.simpleName}")
        }

        val getInfoMethod = clazz.getDeclaredMethod("getInfo")
        val result = getInfoMethod.invoke(person)
        println("\nInvoked getInfo(): $result")

        person.age = 35
        println("After setting age to 35: ${person.getInfo()}")

        val privateMethod = clazz.getDeclaredMethod("privateMethod")
        privateMethod.isAccessible = true
        val privateResult = privateMethod.invoke(person)
        println("Private method result: $privateResult")

        // 静态方法：receiver 传 null
        val utilsClass = ReflectionUtils::class.java
        val staticMethod = utilsClass.getDeclaredMethod("staticUtility", String::class.java)
        val staticResult = staticMethod.invoke(null, "test")
        println("Static method result: $staticResult")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.5 构造器访问
    //
    // Constructor<T>.newInstance() 与 Class.newInstance() 的区别：
    //   - Class.newInstance() 只能调用无参构造，且会透传已检查异常（已废弃）
    //   - Constructor.newInstance() 更安全，支持任意参数，包装为 InvocationTargetException
    //
    // 阅读：java.base/java/lang/reflect/Constructor.java
    // ──────────────────────────────────────────────────────────────
    private fun demo15ConstructorAccess() {
        Lab.section("1.5", "Constructor Access")

        val clazz = SamplePerson::class.java

        println("All constructors:")
        clazz.declaredConstructors.forEach { constructor ->
            val params = constructor.parameterTypes.map { it.simpleName }.joinToString(", ")
            println("  SamplePerson($params)")
        }

        val defaultConstructor = clazz.getDeclaredConstructor()
        val person1 = defaultConstructor.newInstance() as SamplePerson
        println("\nDefault constructor: ${person1.getInfo()}")

        val paramConstructor = clazz.getDeclaredConstructor(String::class.java, Int::class.java, String::class.java)
        val person2 = paramConstructor.newInstance("Bob", 28, "password") as SamplePerson
        println("Parameterized constructor: ${person2.getInfo()}")

        val simpleConstructor = clazz.getDeclaredConstructor(String::class.java)
        val person3 = simpleConstructor.newInstance("Charlie") as SamplePerson
        println("Simple constructor: ${person3.getInfo()}")
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 1 支撑类
// ══════════════════════════════════════════════════════════════════

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
