package studio.sandlight.lang.kotlin

// ══════════════════════════════════════════════════════════════════
// LEVEL 5: Kotlin DSL & Metaprogramming
//
//   Topics covered:
//     5.1  Lambda with Receiver — T.() -> Unit, builder DSL pattern
//     5.2  Reified Type Parameters — inline + reified beats erasure
//     5.3  Value Classes (Inline Classes) — zero-allocation type safety
//
// JVM / stdlib entry points:
//   kotlin.text.buildString          → StringBuilderScope uses lambda-with-receiver
//   kotlin.collections.buildList     → MutableList receiver in block
//   kotlin.reflect.typeOf            → returns full KType including generics at runtime
//   @JvmInline + value class         → unboxed to primitive on JVM
// ══════════════════════════════════════════════════════════════════

import kotlin.reflect.typeOf

// ── Top-level support types for section 5.1 ────────────────────────

private class HtmlBuilder {
    private val sb = StringBuilder()

    // Each tag method appends its markup and returns Unit.
    // The DSL function is an ordinary member — no magic needed.

    fun title(text: String) {
        sb.append("<title>$text</title>")
    }

    fun paragraph(text: String) {
        sb.append("<p>$text</p>")
    }

    fun link(href: String, label: String) {
        sb.append("<a href=\"$href\">$label</a>")
    }

    fun build(): String = "<html>$sb</html>"
}

// buildHtml — the top-level DSL entry point.
//
// The parameter type `HtmlBuilder.() -> Unit` is a *lambda with receiver*.
// Inside the braces the caller writes `title(...)` with no qualifier;
// the compiler expands that to `this.title(...)` where `this` is the
// HtmlBuilder instance created here.
private fun buildHtml(block: HtmlBuilder.() -> Unit): String {
    val builder = HtmlBuilder()
    builder.block()          // 'this' inside block == builder
    return builder.build()
}

// ── Top-level reified helpers for section 5.2 ─────────────────────
// Local inline functions are not supported inside member functions, so
// these helpers live at the top level and are called from demo52.

private inline fun <reified T> printType() =
    println("Type: ${T::class.simpleName}")

private inline fun <reified T> describeType(): String = typeOf<T>().toString()

private inline fun <reified T> emptyListOfReified(): List<T> = emptyList<T>().also {
    println("Created empty List<${T::class.simpleName}>")
}

private inline fun <reified T> Any.isInstanceOf(): Boolean = this is T

// ── Top-level support types for section 5.3 ────────────────────────

@JvmInline
private value class Amount(val cents: Long) {
    // Methods on a value class compile to static helpers — no wrapper object.
    fun dollars(): Double = cents / 100.0
    override fun toString(): String = "Amount(${dollars()} USD)"
}

@JvmInline
private value class UserId(val id: Long) {
    override fun toString(): String = "UserId($id)"
}

// charge takes Amount and UserId — swapping the arguments is a compile error.
private fun charge(amount: Amount, userId: UserId): String =
    "Charged ${amount.dollars()} USD to user ${userId.id}"

@JvmInline
private value class Email(val value: String) {
    // init validates on construction — no separate factory needed.
    init {
        require("@" in value) { "Invalid email: $value" }
    }

    fun domain(): String = value.substringAfter("@")
    fun localPart(): String = value.substringBefore("@")
    override fun toString(): String = "Email($value)"
}

// ══════════════════════════════════════════════════════════════════

object KotlinDsl {

    fun run() {
        demo51LambdaWithReceiver()
        demo52ReifiedTypeParameters()
        demo53ValueClasses()
    }

    // ──────────────────────────────────────────────────────────────
    // 5.1 Lambda with Receiver — Foundation of Kotlin DSLs
    //
    // A lambda with receiver has the type  T.() -> Unit  (or T.() -> R).
    // Inside the lambda, `this` is automatically bound to a T instance —
    // callers can call T's members without any qualifier.
    //
    // How buildHtml works:
    //
    //   fun buildHtml(block: HtmlBuilder.() -> Unit): String {
    //     val b = HtmlBuilder()
    //     b.block()   ← block's 'this' is the HtmlBuilder instance
    //     return b.build()
    //   }
    //
    //   Inside { }:
    //     title("x")   ← this.title("x")  — HtmlBuilder method, no qualifier needed
    //     paragraph()  ← this.paragraph() — same
    //
    //   Kotlin stdlib examples using this pattern:
    //     buildString { append("...") }
    //     buildList { add(...) }
    //     apply { ... }
    //
    // Kotlin stdlib: kotlin.text.buildString, kotlin.collections.buildList
    // ──────────────────────────────────────────────────────────────
    private fun demo51LambdaWithReceiver() {
        println("\n--- 5.1 Lambda with Receiver ---")

        // Basic DSL usage — title/paragraph are HtmlBuilder members,
        // but inside the lambda braces we call them without any prefix.
        val html = buildHtml {
            title("My Page")
            paragraph("Hello, World!")
            link("https://kotlinlang.org", "Kotlin")
        }
        println("Built HTML : $html")

        // Demonstrate that 'this' is the builder inside the block.
        val html2 = buildHtml {
            println("Inside block, this = ${this::class.simpleName}")
            title("About")
            paragraph("Kotlin DSLs are clean.")
        }
        println("Built HTML2: $html2")

        // Stdlib parallel — buildString uses the same pattern.
        // StringBuilderScope.(StringBuilderScope.() -> Unit) in stdlib.
        val greeting = buildString {
            append("Hello")
            append(", ")
            append("Kotlin DSL")
            append("!")
        }
        println("buildString: $greeting")

        // buildList — MutableList<T> as receiver.
        val numbers = buildList {
            add(10)
            add(20)
            addAll(listOf(30, 40, 50))
        }
        println("buildList  : $numbers")

        // apply{} is a scope function that also uses lambda-with-receiver.
        // It returns `this` (the receiver) rather than the lambda result.
        data class Config(var host: String = "", var port: Int = 0, var debug: Boolean = false)

        val cfg = Config().apply {
            host  = "localhost"
            port  = 8080
            debug = true
        }
        println("apply{}    : $cfg")
    }

    // ──────────────────────────────────────────────────────────────
    // 5.2 Reified Type Parameters
    //
    // Problem — type erasure at runtime:
    //   fun <T> parseAs(json: String): T { T::class ... }   ← COMPILE ERROR
    //   The JVM erases T; no type information survives at runtime.
    //
    // Solution — inline + reified:
    //   inline fun <reified T> parseAs(json: String): T { T::class ... }  ← OK
    //   The compiler copies the function body to every call site and
    //   substitutes the actual type — so T is known at each call site.
    //
    //   Normal generic:
    //     fun <T> isListOf(list: Any): Boolean = list is List<T>  ← COMPILE ERROR
    //
    //   Reified + inline:
    //     inline fun <reified T> isListOf(list: Any): Boolean = list is List<T>  ← OK
    //
    //   Compiler inlines the function body at each call site, substituting
    //   the actual type T — no erasure at the call site.
    //
    //   Works for: T::class, T::class.java, is T, as T, typeOf<T>()
    //   Does NOT work for: storing T in a variable, passing T to non-inline fns
    //
    // 阅读: kotlin.reflect.typeOf  (returns KType with full generic info)
    // ──────────────────────────────────────────────────────────────
    private fun demo52ReifiedTypeParameters() {
        println("\n--- 5.2 Reified Type Parameters ---")

        // filterIsInstance<T> — built-in reified example from stdlib.
        // The compiler inlines the check `it is String` at the call site.
        val mixed: List<Any> = listOf(1, "two", 3, "four", 5, 6.0, "six")
        val strings = mixed.filterIsInstance<String>()
        val ints    = mixed.filterIsInstance<Int>()
        val doubles = mixed.filterIsInstance<Double>()

        println("mixed   : $mixed")
        println("strings : $strings")
        println("ints    : $ints")
        println("doubles : $doubles")

        // Custom reified helper — T::class is available because the function is inline+reified.
        // printType<T>() is a top-level inline fun (local inline fns are not supported).
        println()
        printType<String>()
        printType<Int>()
        printType<List<*>>()

        // typeOf<T>() — returns a full KType including generic arguments.
        // Unlike T::class, typeOf preserves generic parameters (e.g. List<Int> vs List<String>).
        println()
        println("typeOf<String>()              = ${describeType<String>()}")
        println("typeOf<List<Int>>()           = ${describeType<List<Int>>()}")
        println("typeOf<Map<String, Double>>() = ${describeType<Map<String, Double>>()}")

        // Practical pattern — reified factory.
        // Without reified this would require passing a KClass<T> explicitly.
        println()
        val emptyStrings = emptyListOfReified<String>()
        val emptyInts    = emptyListOfReified<Int>()
        println("emptyStrings isEmpty: ${emptyStrings.isEmpty()}")
        println("emptyInts    isEmpty: ${emptyInts.isEmpty()}")

        // Reified type check — `is T` becomes possible.
        // isInstanceOf<T>() is a top-level extension (same constraint as above).
        println()
        println("\"hello\".isInstanceOf<String>() = ${"hello".isInstanceOf<String>()}")
        println("\"hello\".isInstanceOf<Int>()    = ${"hello".isInstanceOf<Int>()}")
        println("42.isInstanceOf<Number>()       = ${42.isInstanceOf<Number>()}")
    }

    // ──────────────────────────────────────────────────────────────
    // 5.3 Value Classes (Inline Classes)
    //
    // Problem — primitive obsession:
    //   fun charge(amount: Double, userId: Long) { ... }
    //   charge(userId, amount)  ← compiles silently, wrong order at runtime
    //
    // Solution — wrap primitives in value classes for compile-time safety:
    //   @JvmInline value class Amount(val cents: Long)
    //   @JvmInline value class UserId(val id: Long)
    //   fun charge(amount: Amount, userId: UserId) { ... }
    //   charge(UserId(1L), Amount(100L))  ← COMPILE ERROR, correct types enforced
    //
    // Value class at JVM level — zero allocation:
    //
    //   @JvmInline value class UserId(val id: Long)
    //
    //   Kotlin:  fun findUser(id: UserId): String
    //   JVM:     String findUser(long id)   ← Long unboxed to primitive long
    //
    //   No object created on the heap for UserId
    //   Exception: when used as generic type param (boxed) or in nullable form UserId?
    //
    //   Constraints:
    //     - exactly one val property
    //     - can have functions and computed properties
    //     - cannot extend classes (can implement interfaces)
    //
    // 阅读: @JvmInline annotation → tells compiler to use JVM primitive representation
    //       javap on compiled class shows unboxed method signatures
    // ──────────────────────────────────────────────────────────────
    private fun demo53ValueClasses() {
        println("\n--- 5.3 Value Classes ---")

        // Amount and UserId — type-safe wrappers around Long.
        val price  = Amount(1999L)   // 19.99 USD in cents
        val userId = UserId(42L)

        println("price  = $price")
        println("userId = $userId")
        println("price.cents   = ${price.cents}")
        println("price.dollars = ${"%.2f".format(price.dollars())}")
        println("userId.id     = ${userId.id}")

        // charge() enforces correct argument order at compile time.
        // Swapping `price` and `userId` would be a compile error.
        val result = charge(price, userId)
        println("\n$result")

        // Email value class — validation in init, computed property.
        println()
        val email = Email("alice@example.com")
        println("email            = $email")
        println("email.domain()   = ${email.domain()}")
        println("email.localPart  = ${email.localPart()}")

        // init block enforces the invariant — invalid email throws IllegalArgumentException.
        println()
        try {
            val bad = Email("not-an-email")
            println("Created: $bad")   // never reached
        } catch (e: IllegalArgumentException) {
            println("Caught expected error: ${e.message}")
        }

        // Demonstrate that value classes participate in standard operations.
        val amounts = listOf(Amount(500L), Amount(1999L), Amount(299L), Amount(750L))
        val total   = amounts.sumOf { it.cents }
        val sorted  = amounts.sortedBy { it.cents }
        println()
        println("amounts         : $amounts")
        println("sorted by cents : $sorted")
        println("total cents     : $total  (${Amount(total).dollars()} USD)")

        // Show that two Amount instances wrapping the same value are equal
        // because value class equality delegates to the wrapped property.
        val a1 = Amount(100L)
        val a2 = Amount(100L)
        println()
        println("Amount(100) == Amount(100) : ${a1 == a2}  (structural equality on wrapped value)")
    }
}
