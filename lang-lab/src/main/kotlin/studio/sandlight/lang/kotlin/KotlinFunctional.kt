package studio.sandlight.lang.kotlin

// ══════════════════════════════════════════════════════════════════
// LEVEL 2: 函数式 & 类型系统
//
//   ┌─────────────────────────────────────────────────────────────┐
//   │  2.1  Lambda & Higher-Order Functions                       │
//   │       Function types / it / trailing lambda / operators     │
//   ├─────────────────────────────────────────────────────────────┤
//   │  2.2  Sequences — Lazy Evaluation                           │
//   │       asSequence / pipeline / eager vs lazy                 │
//   ├─────────────────────────────────────────────────────────────┤
//   │  2.3  Sealed Class                                          │
//   │       Result<T> / exhaustive when / sealed interface        │
//   ├─────────────────────────────────────────────────────────────┤
//   │  2.4  Scope Functions                                       │
//   │       let / run / also / apply / with                       │
//   └─────────────────────────────────────────────────────────────┘
//
// Kotlin stdlib / JVM 入口：
//   kotlin.jvm.functions.Function1            → single-arg lambda interface
//   kotlin.jvm.functions.Function2            → two-arg lambda interface
//   kotlin.sequences.TransformingSequence     → map() on a sequence
//   kotlin.sequences.FilteringSequence        → filter() on a sequence
//   kotlin.sequences.TakeSequence             → take() — terminates the pipeline
//   kotlin.jvm.internal.Lambda                → base class for lambda objects
// ══════════════════════════════════════════════════════════════════

object KotlinFunctional {

    fun run() {
        demo21LambdaAndHigherOrder()
        demo22Sequences()
        demo23SealedClass()
        demo24ScopeFunctions()
    }

    // ──────────────────────────────────────────────────────────────
    // 2.1 Lambda & Higher-Order Functions
    //
    // Function type syntax:  (ParamType, ...) -> ReturnType
    //
    //   val add: (Int, Int) -> Int = { a, b -> a + b }
    //                                 ↑ lambda literal
    //
    // Single-parameter shorthand: `it`
    //   val double: (Int) -> Int = { it * 2 }
    //
    // Trailing lambda — when the last parameter is a function, it can
    // be moved outside the parentheses (SAM / higher-order convention):
    //   listOf(1,2,3).map({ it * 2 })   ← standard
    //   listOf(1,2,3).map { it * 2 }    ← trailing lambda (idiomatic)
    //
    // Under the hood:
    //   ┌──────────────────────────────────────────────────────────┐
    //   │  Lambda  ──bytecode──►  anonymous class  OR             │
    //   │                         Function1<Int,Int> instance     │
    //   │                                                          │
    //   │  inline fun map(f: (T)->R)                               │
    //   │    ↑ copies lambda body to call site → NO object alloc  │
    //   └──────────────────────────────────────────────────────────┘
    //
    // Key collection operators demonstrated below:
    //   map        → 1:1 transform
    //   filter     → keep matching elements
    //   reduce     → fold into single value (no initial value)
    //   flatMap    → map then flatten one level
    //   groupBy    → Map<K, List<V>>
    //   partition  → Pair<List, List>  (true | false bucket)
    //   associateBy→ Map<K, V>  (element as key via selector)
    //   windowed   → sliding sub-lists of fixed size
    //   chunked    → non-overlapping sub-lists of fixed size
    //
    // Kotlin stdlib source: kotlin/collections/_Collections.kt
    // ──────────────────────────────────────────────────────────────
    private fun demo21LambdaAndHigherOrder() {
        println("\n--- 2.1 Lambda & Higher-Order Functions ---")

        // --- function types ---
        val add: (Int, Int) -> Int = { a, b -> a + b }
        println("add(3, 4) = ${add(3, 4)}")

        val square: (Int) -> Int = { it * it }
        println("square(5) = ${square(5)}")

        // --- higher-order function: function as parameter ---
        fun applyTwice(f: (Int) -> Int, x: Int): Int = f(f(x))
        println("applyTwice(square, 3) = ${applyTwice(square, 3)}")

        // --- function as return value ---
        fun multiplier(factor: Int): (Int) -> Int = { it * factor }
        val triple = multiplier(3)
        println("triple(7) = ${triple(7)}")

        // --- trailing lambda syntax ---
        val doubled = listOf(1, 2, 3).map { it * 2 }
        println("map { it * 2 }: $doubled")

        // --- collection operators ---
        val nums = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        val evens = nums.filter { it % 2 == 0 }
        println("filter even: $evens")

        val mapped = nums.map { it * it }
        println("map squared: $mapped")

        val sum = nums.reduce { acc, n -> acc + n }
        println("reduce sum: $sum")

        val nested = listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6))
        val flat = nested.flatMap { it }
        println("flatMap flatten: $flat")

        val grouped = nums.groupBy { if (it % 2 == 0) "even" else "odd" }
        println("groupBy even/odd: $grouped")

        val (gt5, le5) = nums.partition { it > 5 }
        println("partition > 5: gt5=$gt5, le5=$le5")

        // associateBy — element itself becomes key via selector
        val assoc = nums.take(6).associateBy { it % 3 }
        println("associateBy { it % 3 }: $assoc")

        // windowed — overlapping sliding windows
        val windows = listOf(1, 2, 3, 4, 5).windowed(3)
        println("windowed(3): $windows")

        // chunked — non-overlapping fixed-size chunks
        val chunks = listOf(1, 2, 3, 4, 5, 6, 7).chunked(3)
        println("chunked(3): $chunks")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.2 Sequences — Lazy Evaluation
    //
    // Eager (List):
    //   Each operator materialises a full intermediate List before the
    //   next operator starts.  For take(5) on 1_000_000 elements, the
    //   entire filtered+mapped list is built just to discard most of it.
    //
    // Lazy (Sequence):
    //   Each element flows through the ENTIRE pipeline before the next
    //   element is even fetched.  Once take(5) is satisfied, iteration
    //   stops immediately.
    //
    //   ┌──────────────────────────────────────────────────────────┐
    //   │  element 1 → filter → (pass) → map → take(count=1)      │
    //   │  element 2 → filter → (skip)                            │
    //   │  element 3 → filter → (skip)                            │
    //   │  element 4 → filter → (pass) → map → take(count=2)      │
    //   │  ...                                                     │
    //   │  element N → filter → (pass) → map → take(count=5) STOP │
    //   └──────────────────────────────────────────────────────────┘
    //
    // Rule of thumb:
    //   Use sequences when:
    //     • chain has 3+ operators  AND
    //     • early termination (take / first / any) can short-circuit
    //   Avoid sequences for small collections — object overhead is
    //   worse than the intermediate-list cost on < ~100 elements.
    //
    // Kotlin stdlib entry points:
    //   kotlin.sequences.TransformingSequence  → wraps map()
    //   kotlin.sequences.FilteringSequence     → wraps filter()
    //   kotlin.sequences.TakeSequence          → wraps take(), terminates
    // ──────────────────────────────────────────────────────────────
    private fun demo22Sequences() {
        println("\n--- 2.2 Sequences — Lazy Evaluation ---")

        // --- lazy pipeline on a large range ---
        val lazyResult = (1..1_000_000)
            .asSequence()
            .filter { it % 2 == 0 }
            .map { it * 3 }
            .take(5)
            .toList()
        println("lazy (1..1_000_000) filter even, map *3, take(5): $lazyResult")

        // --- eager equivalent for comparison ---
        val eagerResult = (1..1_000_000)
            .toList()
            .filter { it % 2 == 0 }
            .map { it * 3 }
            .take(5)
        println("eager same pipeline result:                        $eagerResult")

        // --- demonstrate early termination with print tracing ---
        println("\nElement-flow trace (small range to show ordering):")
        println("Eager processes ALL elements in each step:")
        val eagerTrace = listOf(1, 2, 3, 4, 5)
            .filter { print("  filter($it)"); it % 2 == 0 }
            .map    { print("  map($it)");    it * 3      }
            .take(1)
        println("\n  result: $eagerTrace")

        println("Lazy — each element crosses ALL operators before the next:")
        val lazyTrace = listOf(1, 2, 3, 4, 5)
            .asSequence()
            .filter { print("  filter($it)"); it % 2 == 0 }
            .map    { print("  map($it)");    it * 3      }
            .take(1)
            .toList()
        println("\n  result: $lazyTrace")

        // --- short-circuit: find first multiple of both 7 and 13 ---
        val firstMultiple = (1..Int.MAX_VALUE)
            .asSequence()
            .first { it % 7 == 0 && it % 13 == 0 }
        println("\nfirst multiple of 7 and 13 (lazy search from 1): $firstMultiple")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.3 Sealed Class
    //
    // sealed class — all direct subclasses must be in the same
    // compilation unit (file in Kotlin 1.1+, package in 1.5+).
    // This lets the compiler prove exhaustiveness of `when`.
    //
    //   sealed class Result<out T>
    //     ├── data class Success<T>(val value: T) : Result<T>()
    //     ├── data class Failure(val error: String) : Result<Nothing>()
    //     └── object Loading : Result<Nothing>()
    //
    // out T = covariant: Result<String> is a subtype of Result<Any>
    //
    // Exhaustive when — no `else` branch needed:
    //   when (result) {
    //     is Success -> ...   // smart cast: result.value available
    //     is Failure -> ...
    //     Loading    -> ...
    //   }
    //
    // sealed interface (Kotlin 1.5+):
    //   Works like sealed class but allows multi-inheritance.
    //   Useful when you want subclasses to also extend other classes:
    //
    //     sealed interface ApiResponse<out T>
    //     data class Ok<T>(val body: T)      : ApiResponse<T>
    //     data class Err(val code: Int)      : ApiResponse<Nothing>
    //     data class Redirect(val url: String) : ApiResponse<Nothing>
    //
    // ──────────────────────────────────────────────────────────────
    private fun demo23SealedClass() {
        println("\n--- 2.3 Sealed Class ---")

        fun divide(a: Int, b: Int): Result<Int> =
            if (b == 0) Result.Failure("Division by zero") else Result.Success(a / b)

        fun describe(r: Result<Int>): String = when (r) {
            is Result.Success -> "Success: ${r.value}"
            is Result.Failure -> "Failure: ${r.error}"
            Result.Loading    -> "Loading…"
            // No `else` needed — compiler knows all subclasses
        }

        println(describe(divide(10, 2)))
        println(describe(divide(5, 0)))
        println(describe(Result.Loading))

        // --- chaining with map-like helper ---
        fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> = when (this) {
            is Result.Success -> Result.Success(transform(value))
            is Result.Failure -> this
            Result.Loading    -> Result.Loading
        }

        val chained = divide(20, 4).mapSuccess { it * 100 }
        println("chained mapSuccess (*100): ${describe(chained as Result<Int>)}")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.4 Scope Functions
    //
    // All five scope functions execute a lambda in the context of an
    // object, but differ on two axes:
    //   • How the object is referenced inside the lambda: this vs it
    //   • What the function returns: lambda result vs the receiver
    //
    // Scope function  receiver  return        canonical use
    // ─────────────────────────────────────────────────────
    // let   { it }   it        lambda result  null-safe block
    // run   { this } this      lambda result  init + compute
    // also  { it }   it        receiver       logging, side-effects
    // apply { this } this      receiver       builder / config
    // with  (obj) {} this      lambda result  batch ops, no repeat
    //
    // Memory model: all five are inline functions — no extra object
    // allocation; the lambda body is copied to the call site.
    //
    // Kotlin stdlib: kotlin/Util.kt → let, apply, also, run, with
    // ──────────────────────────────────────────────────────────────
    private fun demo24ScopeFunctions() {
        println("\n--- 2.4 Scope Functions ---")

        // Print reference table
        println("""
            |Scope fn  receiver  returns        canonical use
            |─────────────────────────────────────────────────────────
            |let       it        lambda result  null-safe transformation
            |run       this      lambda result  compute value from object
            |also      it        receiver       logging / side-effects
            |apply     this      receiver       builder / object config
            |with(obj) this      lambda result  batch ops, no repetition
        """.trimMargin())

        // --- let: null-safe transformation ---
        println("\n-- let: null-safe block --")
        val rawInput: String? = "  hello world  "
        val wordCount = rawInput?.let { it.trim().split(" ").size }
        println("rawInput?.let { word count } = $wordCount")

        val nullInput: String? = null
        val safeCount = nullInput?.let { it.trim().split(" ").size }
        println("nullInput?.let { word count } = $safeCount")

        // --- apply: builder / init block ---
        println("\n-- apply: builder pattern --")
        data class Config(var host: String = "", var port: Int = 0, var debug: Boolean = false)

        val config = Config().apply {
            host  = "localhost"
            port  = 8080
            debug = true
        }
        println("Config via apply: $config")

        // --- also: side-effect / logging, returns receiver unchanged ---
        println("\n-- also: logging side-effect --")
        val numbers = mutableListOf(3, 1, 4, 1, 5, 9)
            .also { println("before sort: $it") }
            .also { it.sort() }
            .also { println("after sort:  $it") }
        println("final list:  $numbers")

        // --- run: compute a value from the object's context ---
        println("\n-- run: compute from context --")
        data class Circle(val radius: Double)

        val area = Circle(5.0).run {
            val circumference = 2 * Math.PI * radius
            val area          = Math.PI * radius * radius
            "radius=$radius, circumference=%.2f, area=%.2f".format(circumference, area)
        }
        println("Circle.run result: $area")

        // --- with: batch operations without repeating the receiver name ---
        println("\n-- with: batch ops --")
        data class User(val name: String, val email: String, val age: Int)

        val user = User("Alice", "alice@example.com", 30)
        val summary = with(user) {
            // name, email, age are all in scope as `this.*`
            "User[$name] email=$email age=$age eligible=${age >= 18}"
        }
        println("with(user) summary: $summary")
    }
}

// ──────────────────────────────────────────────────────────────────
// Supporting sealed class definition (file-level, outside the object)
// ──────────────────────────────────────────────────────────────────
sealed class Result<out T> {
    data class Success<T>(val value: T)        : Result<T>()
    data class Failure(val error: String)      : Result<Nothing>()
    object Loading                             : Result<Nothing>()
}
