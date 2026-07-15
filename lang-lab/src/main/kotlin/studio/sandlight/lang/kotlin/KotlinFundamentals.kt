package studio.sandlight.lang.kotlin

import studio.sandlight.lang.support.Lab

// ══════════════════════════════════════════════════════════════════
// LEVEL 1: Kotlin Fundamentals
//
//   Topics covered:
//     1.1  Null Safety        — String vs String?, ?., ?:, !!, smart cast
//     1.2  Data Class         — equals / hashCode / toString / copy / destructuring
//     1.3  Extension Functions — static dispatch, no runtime cost, no class modification
//     1.4  Object & Companion — process-scoped singleton, static factory, @JvmStatic
//     1.5  When Expression    — expression form, type checks, ranges, sealed class
//
// JVM entry points:
//   kotlin.jvm.internal.Intrinsics       → null check injection (Intrinsics.checkNotNullParameter)
//   kotlin.jvm.internal.Reflection       → backing reflection helpers
// ══════════════════════════════════════════════════════════════════

// ── Top-level private types used by the demos ──────────────────────

private data class Point(val x: Int, val y: Int)

private sealed class Shape {
    data class Circle(val radius: Double) : Shape()
    data class Rectangle(val w: Double, val h: Double) : Shape()
    object Unknown : Shape()
}

// Used by demo 1.4 — must be top-level for `object` and `companion object`
private object AppConfig {
    val version = "1.0.0"
    val maxRetries = 3
}

private class Connection private constructor(val host: String, val port: Int) {
    companion object {
        // @JvmStatic would make this a true static method for Java callers
        fun create(host: String, port: Int = 5432): Connection = Connection(host, port)
        fun local(): Connection = Connection("localhost", 5432)

        const val DEFAULT_TIMEOUT_MS = 5000
    }

    override fun toString() = "Connection($host:$port)"
}

// ── Extension functions (compile to static methods, no class modification) ──

private fun String.isPalindrome(): Boolean = this == this.reversed()

private fun Int.isEven(): Boolean = this % 2 == 0

// ══════════════════════════════════════════════════════════════════

object KotlinFundamentals {

    fun run() {
        demo11NullSafety()
        demo12DataClass()
        demo13ExtensionFunctions()
        demo14ObjectAndCompanion()
        demo15WhenExpression()
    }

    // ──────────────────────────────────────────────────────────────
    // 1.1 Null Safety
    //
    // Type system encodes nullability:
    //
    //   String   — compiler guarantees non-null; bytecode receives a null check
    //   String?  — may be null; you must guard before dereferencing
    //
    // Operators:
    //   ?.    safe call       — returns null if receiver is null, skips the call
    //   ?:    Elvis           — provides a default when the left side is null
    //   !!    non-null assert — throws KotlinNullPointerException if null
    //
    // Smart cast — after an explicit null check the compiler widens the type:
    //
    //   var s: String? = "hello"
    //   if (s != null) {
    //       s.length   ← compiler treats s as String (non-null) here
    //   }
    //
    // Bytecode note — for every non-null parameter the compiler injects:
    //
    //   ┌──────────────────────────────────────────────────────────┐
    //   │  fun greet(name: String) { … }                          │
    //   │                                                          │
    //   │  // Generated JVM bytecode (decompiled Java equivalent): │
    //   │  public static void greet(@NotNull String name) {        │
    //   │      Intrinsics.checkNotNullParameter(name, "name");     │
    //   │      …                                                   │
    //   │  }                                                       │
    //   └──────────────────────────────────────────────────────────┘
    //
    // Read: kotlin.jvm.internal.Intrinsics.checkNotNullParameter
    // ──────────────────────────────────────────────────────────────
    private fun demo11NullSafety() {
        Lab.section("1.1", "Null Safety")

        // Non-null vs nullable declaration
        val nonNull: String = "Hello"
        val nullable: String? = null
        val alsoNullable: String? = "World"

        println("nonNull    : String  = \"$nonNull\"")
        println("nullable   : String? = $nullable")
        println("alsoNullable: String? = \"$alsoNullable\"")

        // Safe call ?.
        val lengthOrNull: Int? = nullable?.length          // null (receiver was null)
        val actualLength: Int? = alsoNullable?.length      // 5
        println("\nnullable?.length       = $lengthOrNull  (safe call on null → null)")
        println("alsoNullable?.length   = $actualLength  (safe call on non-null → value)")

        // Elvis ?:
        val safeLength: Int = nullable?.length ?: -1       // -1 as default
        val elvisChain: String = nullable ?: alsoNullable ?: "fallback"
        println("\nnullable?.length ?: -1 = $safeLength   (Elvis provides default)")
        println("nullable ?: alsoNullable ?: \"fallback\" = \"$elvisChain\"")

        // Non-null assert !!  (throws KotlinNullPointerException if null)
        val assured: Int = alsoNullable!!.length           // safe here, alsoNullable != null
        println("\nalsoNullable!!.length  = $assured  (!! asserts non-null)")

        // Smart cast — compiler tracks nullability through control flow
        var smart: String? = "smart cast demo"
        if (smart != null) {
            // Inside this block the compiler promotes smart to String (non-null)
            println("\nSmart cast inside null check: upper = ${smart.uppercase()}")
        }

        // Smart cast after early return
        fun printLength(s: String?) {
            if (s == null) return
            // s is String (non-null) from here onward
            println("Smart cast after return guard: length = ${s.length}")
        }
        printLength("Kotlin")
        printLength(null)   // returns early, nothing printed
    }

    // ──────────────────────────────────────────────────────────────
    // 1.2 Data Class
    //
    // Declaring `data class Point(val x: Int, val y: Int)` instructs
    // the compiler to auto-generate the following (Java equivalent):
    //
    //   ┌──────────────────────────────────────────────────────────┐
    //   │  public final class Point {                              │
    //   │      private final int x;                               │
    //   │      private final int y;                               │
    //   │                                                          │
    //   │      // primary constructor                             │
    //   │      public Point(int x, int y) { this.x=x; this.y=y; }│
    //   │                                                          │
    //   │      // equals — compares all primary-constructor props  │
    //   │      public boolean equals(Object other) { … }          │
    //   │                                                          │
    //   │      // hashCode — combines all properties              │
    //   │      public int hashCode() { … }                        │
    //   │                                                          │
    //   │      // toString — "Point(x=1, y=2)"                    │
    //   │      public String toString() { … }                     │
    //   │                                                          │
    //   │      // copy — returns new instance, fields overridable  │
    //   │      public Point copy(int x, int y) { … }              │
    //   │                                                          │
    //   │      // componentN — enables destructuring              │
    //   │      public int component1() { return x; }              │
    //   │      public int component2() { return y; }              │
    //   │  }                                                       │
    //   └──────────────────────────────────────────────────────────┘
    //
    // Note: only properties declared in the PRIMARY constructor
    // participate in equals/hashCode/toString/copy.
    // ──────────────────────────────────────────────────────────────
    private fun demo12DataClass() {
        Lab.section("1.2", "Data Class")

        val p1 = Point(3, 4)
        val p2 = Point(3, 4)
        val p3 = Point(0, 0)

        // toString — auto-generated
        println("p1.toString()          = $p1")

        // equals — structural, not referential
        println("p1 == p2               = ${p1 == p2}   (structural equality)")
        println("p1 === p2              = ${p1 === p2}  (referential equality — different objects)")

        // hashCode — consistent with equals
        println("p1.hashCode()          = ${p1.hashCode()}")
        println("p2.hashCode()          = ${p2.hashCode()}  (same as p1 — equal objects)")
        println("p3.hashCode()          = ${p3.hashCode()}  (different values)")

        // copy — returns a new instance with selective overrides
        val p4 = p1.copy(y = 10)
        println("\np1.copy(y = 10)        = $p4  (x unchanged, y overridden)")
        println("p1 unchanged           = $p1")

        // Destructuring via componentN()
        val (x, y) = p1
        println("\nval (x, y) = p1  →  x=$x, y=$y  (destructuring declaration)")

        // Useful in loops and lambdas
        val points = listOf(Point(1, 2), Point(3, 4))
        println("Points with even x: ${points.filter { (px, _) -> px.isEven() }}")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.3 Extension Functions
    //
    // Extension functions do NOT modify the class and have NO runtime overhead.
    // The compiler transforms them into static methods on a generated class:
    //
    //   ┌──────────────────────────────────────────────────────────┐
    //   │  // Kotlin source                                        │
    //   │  fun String.isPalindrome(): Boolean = this == reversed() │
    //   │                                                          │
    //   │  // Compiled to (Java equivalent)                       │
    //   │  public final class KotlinFundamentalsKt {              │
    //   │      public static boolean isPalindrome(String $this) { │
    //   │          return $this.equals(new StringBuilder($this)   │
    //   │              .reverse().toString());                    │
    //   │      }                                                   │
    //   │  }                                                       │
    //   │                                                          │
    //   │  // Call site compiles to:                              │
    //   │  KotlinFundamentalsKt.isPalindrome("racecar")           │
    //   └──────────────────────────────────────────────────────────┘
    //
    // Because dispatch is STATIC (not virtual), extension functions:
    //   - Cannot be overridden polymorphically
    //   - Cannot access private members of the receiver class
    //   - Resolve at compile time based on the declared type, not the runtime type
    // ──────────────────────────────────────────────────────────────
    private fun demo13ExtensionFunctions() {
        Lab.section("1.3", "Extension Functions")

        // String.isPalindrome
        val words = listOf("racecar", "hello", "level", "kotlin", "civic")
        for (w in words) {
            println("\"$w\".isPalindrome() = ${w.isPalindrome()}")
        }

        // Int.isEven
        println()
        for (n in 1..6) {
            println("$n.isEven() = ${n.isEven()}")
        }

        // Extension on nullable receiver — safe to call even with null
        fun String?.orEmpty2(): String = this ?: "(empty)"
        val nullable: String? = null
        println("\nnullable.orEmpty2() = \"${nullable.orEmpty2()}\"  (extension on String?)")

        // Static dispatch — type at compile time determines which extension runs
        open class Animal
        class Dog : Animal()

        fun Animal.speak() = "Animal speaks"
        fun Dog.speak()    = "Dog barks"

        val animal: Animal = Dog()    // runtime type is Dog, but declared as Animal
        println("\nStatic dispatch: animal.speak() = \"${animal.speak()}\" (Animal extension wins)")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.4 Object & Companion Object
    //
    // `object` declaration — process-scoped singleton:
    //
    //   ┌──────────────────────────────────────────────────────────┐
    //   │  object Singleton { val id = 42 }                        │
    //   │                                                          │
    //   │  // Compiles to:                                         │
    //   │  public final class Singleton {                          │
    //   │      public static final Singleton INSTANCE;             │
    //   │      private static final int id = 42;                  │
    //   │      static { INSTANCE = new Singleton(); }             │  ← class-init, thread-safe
    //   │      public int getId() { return id; }                  │
    //   │  }                                                       │
    //   └──────────────────────────────────────────────────────────┘
    //
    // `companion object` — attached to a class, replaces Java statics:
    //
    //   class Greeter {
    //       companion object {
    //           @JvmStatic fun create(): Greeter = Greeter()
    //       }
    //   }
    //
    //   Kotlin call:  Greeter.create()
    //   Java call:    Greeter.create()   ← @JvmStatic makes it a true static method
    //
    // Without @JvmStatic, Java must call: Greeter.Companion.create()
    // ──────────────────────────────────────────────────────────────
    private fun demo14ObjectAndCompanion() {
        Lab.section("1.4", "Object & Companion Object")

        // AppConfig is a top-level private object — singleton, same instance every time
        println("AppConfig.version    = ${AppConfig.version}")
        println("AppConfig.maxRetries = ${AppConfig.maxRetries}")
        println("AppConfig identity   = ${System.identityHashCode(AppConfig)}  (always the same)")
        println("AppConfig identity   = ${System.identityHashCode(AppConfig)}  (same call, same hash)")

        // Connection uses a companion object — static factory pattern (defined at top-level above)
        val conn1 = Connection.create("db.example.com", 5432)
        val conn2 = Connection.local()
        println("\nConnection.create(\"db.example.com\", 5432) = $conn1")
        println("Connection.local()                         = $conn2")
        println("Connection.DEFAULT_TIMEOUT_MS              = ${Connection.DEFAULT_TIMEOUT_MS}")

        // Object expression — anonymous object (like Java anonymous class)
        val comparator = object : Comparator<String> {
            override fun compare(a: String, b: String): Int = a.length - b.length
        }
        val sorted = listOf("banana", "fig", "apple", "kiwi").sortedWith(comparator)
        println("\nSorted by length (object expression comparator): $sorted")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.5 When Expression
    //
    // `when` is an expression — it returns a value and must be exhaustive
    // when used as one. Contrast with Java switch:
    //
    //   ┌────────────────────────────┬──────────────────────────────┐
    //   │  Java switch (statement)   │  Kotlin when (expression)    │
    //   ├────────────────────────────┼──────────────────────────────┤
    //   │  switch (x) {              │  val result = when (x) {     │
    //   │    case 1:                 │      1    -> "one"           │
    //   │      // FALLS THROUGH ↓   │      2    -> "two"           │  ← no fall-through
    //   │    case 2:                 │      else -> "other"         │
    //   │      result = "one/two";   │  }                           │
    //   │      break;               │                               │
    //   │    default:                │                               │
    //   │      result = "other";    │                               │
    //   │  }                        │                               │
    //   └────────────────────────────┴──────────────────────────────┘
    //
    // On a sealed class, the compiler enforces exhaustiveness — no `else` needed.
    // ──────────────────────────────────────────────────────────────
    private fun demo15WhenExpression() {
        Lab.section("1.5", "When Expression")

        // when as expression — returns a value
        fun describe(n: Int): String = when (n) {
            0          -> "zero"
            1          -> "one"
            in 2..9    -> "single digit (2-9)"
            in 10..99  -> "double digit"
            else       -> "large number"
        }

        for (n in listOf(0, 1, 5, 42, 100)) {
            println("describe($n) = \"${describe(n)}\"")
        }

        // Type-checked branches with smart cast
        fun inspect(value: Any): String = when (value) {
            is Int    -> "Int with doubled value ${value * 2}"      // smart cast: value is Int here
            is String -> "String of length ${value.length}"         // smart cast: value is String here
            is List<*> -> "List with ${value.size} elements"
            else      -> "Unknown type: ${value::class.simpleName}"
        }

        println()
        println("inspect(42)               = \"${inspect(42)}\"")
        println("inspect(\"hello\")          = \"${inspect("hello")}\"")
        println("inspect(listOf(1,2,3))    = \"${inspect(listOf(1, 2, 3))}\"")
        println("inspect(3.14)             = \"${inspect(3.14)}\"")

        // when on sealed class — exhaustive, no else needed
        fun area(shape: Shape): Double = when (shape) {
            is Shape.Circle    -> Math.PI * shape.radius * shape.radius
            is Shape.Rectangle -> shape.w * shape.h
            Shape.Unknown      -> 0.0
            // No else: compiler verifies all subclasses are covered
        }

        val shapes: List<Shape> = listOf(
            Shape.Circle(5.0),
            Shape.Rectangle(3.0, 4.0),
            Shape.Unknown
        )
        println()
        for (shape in shapes) {
            println("area($shape) = ${"%.2f".format(area(shape))}")
        }

        // when without argument — replaces if-else chain
        val temperature = 22
        val weather = when {
            temperature < 0   -> "freezing"
            temperature < 10  -> "cold"
            temperature < 20  -> "cool"
            temperature < 30  -> "warm"
            else              -> "hot"
        }
        println("\ntemperature=$temperature → weather=\"$weather\"  (when without argument)")
    }
}
