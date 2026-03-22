package studio.sandlight.lang.kotlin

// LEVEL 3: Type System — 泛型、型变、委托属性、运算符重载

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

object KotlinTypeSystem {

    fun run() {
        println("\n═══════════════════════════════════════════════════════════")
        println("  LEVEL 3: TYPE SYSTEM — Generics, Variance, Delegates, Operators")
        println("═══════════════════════════════════════════════════════════")

        demoGenericsAndTypeErasure()
        demoVariance()
        demoDelegatedProperties()
        demoOperatorOverloading()
    }

    // ──────────────────────────────────────────────────────────────
    // 3.1 泛型与类型擦除
    //
    // Type Erasure at JVM level:
    //
    //   Kotlin:  fun <T> identity(t: T): T = t
    //   JVM:     Object identity(Object t)   ← T erased to Object
    //
    //   List<String> and List<Int> are both just List at runtime
    //   → Cannot do: if (list is List<String>) — compiler error
    //   → Can do:    if (list is List<*>)      — star projection ok
    //
    // reified type parameters (inline functions only):
    //   inline fun <reified T> filterIsInstance() keeps T at runtime
    //   by embedding the actual class check into the call site bytecode.
    //   This is how stdlib's filterIsInstance<T>() avoids type erasure.
    //
    // Kotlin source pointer:
    //   kotlin/collections/_Collections.kt → filterIsInstance()
    //   kotlin/jvm/internal/Intrinsics.kt   → checkNotNullParameter()
    // ──────────────────────────────────────────────────────────────
    private fun demoGenericsAndTypeErasure() {
        println("\n--- 3.1 Generics & Type Erasure ---")

        // At runtime List<String> and List<Int> are the same erasure
        val strings: List<String> = listOf("hello", "world")
        val ints: List<Int>       = listOf(1, 2, 3)

        // Both pass the star-projection check — erased to List at JVM level
        println("strings is List<*>: ${strings is List<*>}")
        println("ints    is List<*>: ${ints    is List<*>}")

        // JVM runtime class is identical — both are just "java.util.List"
        println("strings JVM class: ${strings.javaClass.name}")
        println("ints    JVM class: ${ints.javaClass.name}")
        println("same class object: ${strings.javaClass == ints.javaClass}")

        // filterIsInstance<T> works because it is an inline reified function.
        // The compiler replaces T with the actual class at each call site, so
        // the check happens against Int.class — no erasure.
        val mixed: List<Any> = listOf(1, 2, "three", 4, "five")
        val intsOnly: List<Int> = mixed.filterIsInstance<Int>()
        println("\nmixed list: $mixed")
        println("filterIsInstance<Int>: $intsOnly")

        // Star projection — accepts any List regardless of element type
        println("\nstar projection printList:")
        printList(strings)
        printList(ints)

        // Generic identity function — T erased to Object at JVM level
        println("\nidentity(42)    = ${identity(42)}")
        println("identity(\"hi\") = ${identity("hi")}")
    }

    // Star-projection: accepts List<String>, List<Int>, List<Any>, …
    private fun printList(list: List<*>) {
        list.forEach { print("  $it") }
        println()
    }

    // At JVM bytecode level this compiles to: Object identity(Object t)
    private fun <T> identity(t: T): T = t

    // ──────────────────────────────────────────────────────────────
    // 3.2 型变 (Variance)
    //
    // Variance — PECS rule (Producer Extends, Consumer Super):
    //
    //   Java:    <? extends T>  →  Kotlin: out T  (covariant,    Producer)
    //   Java:    <? super T>    →  Kotlin: in T   (contravariant, Consumer)
    //
    //   out T: can only be returned (produced), never accepted as param
    //   in T:  can only be accepted (consumed), never returned
    //
    //   Example:
    //     List<out Any>  ← can hold List<String>, List<Int> (safe to read)
    //     Comparator<in String> ← Comparator<Any> can compare strings too
    //
    // Declaration-site variance (Kotlin) vs use-site variance (Java):
    //   Kotlin marks variance on the interface/class declaration itself.
    //   Java requires wildcards at every use site (<? extends T>).
    //
    // Kotlin source pointer:
    //   kotlin/collections/Collections.kt → interface List<out E>
    //   kotlin/Comparable.kt              → interface Comparable<in T>
    // ──────────────────────────────────────────────────────────────
    private fun demoVariance() {
        println("\n--- 3.2 Variance (in/out) ---")

        // out T — covariant Producer: Producer<String> is a subtype of Producer<Any>
        val stringProducer: Producer<String> = StringProducer("hello from producer")
        val anyProducer: Producer<Any>       = stringProducer     // safe — only reads T
        println("Producer<String> assigned to Producer<Any>: ${anyProducer.produce()}")

        // in T — contravariant Consumer: Consumer<Any> is a subtype of Consumer<String>
        val anyConsumer: Consumer<Any>       = PrintConsumer()
        val stringConsumer: Consumer<String> = anyConsumer         // safe — only writes T
        stringConsumer.consume("consumed by anyConsumer")

        // Kotlin's own List is covariant (out E) — List<String> can be used as List<Any>
        val listOfStrings: List<String> = listOf("a", "b", "c")
        val listOfAny: List<Any>        = listOfStrings             // compiles — out variance
        println("List<String> as List<Any>: $listOfAny")

        // Use-site variance with out projection
        println("\nreadFirst (out projection): ${readFirst(listOfStrings)}")

        // Use-site variance with in projection
        val dest = mutableListOf<Any>()
        copyInto(listOfStrings, dest)
        println("copyInto result: $dest")
    }

    // Use-site out projection — caller can provide List<String>, List<Int>, etc.
    private fun readFirst(list: List<out Any>): Any? = list.firstOrNull()

    // Use-site in projection — dest accepts MutableList<Any>, MutableList<CharSequence>, etc.
    private fun copyInto(src: List<String>, dest: MutableList<in String>) {
        src.forEach { dest.add(it) }
    }

    // ──────────────────────────────────────────────────────────────
    // 3.3 委托属性 (Delegated Properties)
    //
    // Kotlin delegated properties desugar to:
    //
    //   val p: T by SomeDelegate()
    //
    //   ─► private val p$delegate = SomeDelegate()
    //      val p: T get() = p$delegate.getValue(this, ::p)
    //
    // The delegate object must implement:
    //   operator fun getValue(thisRef: R, property: KProperty<*>): T
    //   operator fun setValue(thisRef: R, property: KProperty<*>, value: T)  // for var
    //
    // Kotlin source pointer:
    //   kotlin/properties/Delegates.kt  → lazy(), observable(), vetoable()
    //   kotlin/reflect/KProperty.kt     → KProperty, KMutableProperty
    // ──────────────────────────────────────────────────────────────
    private fun demoDelegatedProperties() {
        println("\n--- 3.3 Delegated Properties ---")

        // ── by lazy ──────────────────────────────────────────────
        println("\n[by lazy]")
        val lazyHolder = LazyHolder()
        println("  before first access")
        println("  first  access: ${lazyHolder.expensive}")
        println("  second access: ${lazyHolder.expensive}")   // no recompute

        // ── by Delegates.observable ───────────────────────────────
        println("\n[by Delegates.observable]")
        val counter = ObservableCounter()
        counter.count = 1
        counter.count = 5
        counter.count = 10

        // ── by map ────────────────────────────────────────────────
        println("\n[by map]")
        val cfg = Config(mapOf("host" to "localhost", "port" to 8080))
        println("  host: ${cfg.host}")
        println("  port: ${cfg.port}")

        // ── custom delegate ───────────────────────────────────────
        println("\n[custom delegate — UpperCaseDelegate]")
        val styled = StyledText()
        styled.title = "kotlin type system"
        println("  title stored as: ${styled.title}")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.4 运算符重载 (Operator Overloading)
    //
    // Operator overloading — compiled to named methods:
    //
    //   a + b   → a.plus(b)
    //   a - b   → a.minus(b)
    //   a * b   → a.times(b)
    //   a[i]    → a.get(i)
    //   -a      → a.unaryMinus()
    //   a in b  → b.contains(a)
    //   a..b    → a.rangeTo(b)
    //   a()     → a.invoke()
    //
    // All operator functions must be marked with the `operator` keyword.
    // They can be member functions or extension functions.
    //
    // Kotlin source pointer:
    //   kotlin/Operator.kt (annotations)
    //   kotlin/ranges/Ranges.kt → ClosedRange, rangeTo()
    // ──────────────────────────────────────────────────────────────
    private fun demoOperatorOverloading() {
        println("\n--- 3.4 Operator Overloading ---")

        val v1 = Vec2(3.0, 4.0)
        val v2 = Vec2(1.0, 2.0)

        // plus — vector addition
        val sum = v1 + v2
        println("v1 = $v1")
        println("v2 = $v2")
        println("v1 + v2 = $sum                    ← plus()")

        // times — scalar multiplication
        val scaled = v1 * 2.0
        println("v1 * 2.0 = $scaled                  ← times()")

        // unaryMinus — negation
        val negated = -v1
        println("-v1 = $negated                  ← unaryMinus()")

        // get — index accessor (0 → x, 1 → y)
        println("v1[0] = ${v1[0]}  v1[1] = ${v1[1]}             ← get()")

        // contains — membership test
        val box = BoundingBox(Vec2(0.0, 0.0), Vec2(5.0, 5.0))
        val inside  = Vec2(2.0, 3.0)
        val outside = Vec2(6.0, 1.0)
        println("$inside in box:  ${inside  in box}           ← contains()")
        println("$outside in box: ${outside in box}          ← contains()")

        // rangeTo — create a Vec2Range ordered by magnitude
        val small = Vec2(0.0, 0.0)
        val large = Vec2(10.0, 10.0)
        val range = small..large
        println("magnitude range: ${"%.2f".format(range.start.magnitude())} .. ${"%.2f".format(range.end.magnitude())}")
        println("v1.magnitude() = ${"%.2f".format(v1.magnitude())}            ← in range? ${v1 in range}")
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 3 Support Interfaces & Classes
// ══════════════════════════════════════════════════════════════════

// ── 3.2 Variance support ─────────────────────────────────────────

// out T — covariant: Producer<String> is a subtype of Producer<Any>
interface Producer<out T> {
    fun produce(): T
}

// in T — contravariant: Consumer<Any> is a subtype of Consumer<String>
interface Consumer<in T> {
    fun consume(t: T)
}

class StringProducer(private val value: String) : Producer<String> {
    override fun produce(): String = value
}

class PrintConsumer : Consumer<Any> {
    override fun consume(t: Any) {
        println("  PrintConsumer received: $t")
    }
}

// ── 3.3 Delegated Properties support ─────────────────────────────

class LazyHolder {
    val expensive: String by lazy {
        println("  [lazy] computing...")
        "computed-value"
    }
}

class ObservableCounter {
    var count: Int by Delegates.observable(0) { prop, old, new ->
        println("  ${prop.name}: $old → $new")
    }
}

// Properties backed by a Map — keys must match property names exactly
class Config(val map: Map<String, Any?>) {
    val host: String by map
    val port: Int    by map
}

// Custom delegate — stores String values as UPPER CASE
class UpperCaseDelegate {
    private var stored: String = ""

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = stored

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        stored = value.uppercase()
    }
}

class StyledText {
    var title: String by UpperCaseDelegate()
}

// ── 3.4 Operator Overloading support ─────────────────────────────

data class Vec2(val x: Double, val y: Double) {

    // a + b  → a.plus(b)
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    // a - b  → a.minus(b)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    // a * scalar  → a.times(scalar)
    operator fun times(scalar: Double): Vec2 = Vec2(x * scalar, y * scalar)

    // -a  → a.unaryMinus()
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    // a[i]  → a.get(i)  — 0 = x component, 1 = y component
    operator fun get(index: Int): Double = when (index) {
        0    -> x
        1    -> y
        else -> throw IndexOutOfBoundsException("Vec2 index must be 0 or 1, got $index")
    }

    // a..b  → a.rangeTo(b)  — creates a Vec2Range ordered by magnitude
    operator fun rangeTo(other: Vec2): Vec2Range = Vec2Range(this, other)

    fun magnitude(): Double = kotlin.math.sqrt(x * x + y * y)

    override fun toString(): String = "Vec2(%.1f, %.1f)".format(x, y)
}

// Vec2Range — range ordered by vector magnitude (used by Vec2..Vec2 rangeTo operator)
class Vec2Range(val start: Vec2, val end: Vec2) {
    operator fun contains(v: Vec2): Boolean =
        v.magnitude() in start.magnitude()..end.magnitude()
}

// BoundingBox demonstrates `in` operator via contains()
class BoundingBox(private val min: Vec2, private val max: Vec2) {

    // a in box  → box.contains(a)
    operator fun contains(v: Vec2): Boolean =
        v.x in min.x..max.x && v.y in min.y..max.y
}
