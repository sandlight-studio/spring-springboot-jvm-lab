package studio.sandlight.lang.collections

import studio.sandlight.lang.support.Lab
import kotlin.system.measureTimeMillis

// LEVEL 3: Sequences — 惰性求值、流水线优化

object Sequences {

    fun run() {

        demo31EagerVsLazy()
        demo32CreatingSequences()
        demo33InfiniteSequences()
        demo34WhenToUseSequences()
    }

    // ──────────────────────────────────────────────────────────────
    // 3.1 Eager（List）vs Lazy（Sequence）
    //
    // Eager：每个中间操作处理所有元素，创建中间集合
    //
    //   list.map { it*it }          → 创建完整中间 List [1,4,9,16,25,...]
    //       .filter { it % 2 == 0 } → 再创建一个 List [4,16,36,...]
    //       .take(3)                → 最终 List [4,16,36]
    //
    // Lazy：元素逐个流过整条流水线，直到满足终止条件
    //
    //   seq.map { it*it }
    //      .filter { it % 2 == 0 }
    //      .take(3)
    //         ↑
    //         终止操作触发求值（类似 Java Stream 的 terminal operation）
    //         一旦找到 3 个元素，立即停止，不处理后续元素
    //
    //   中间无任何集合创建 — 节省内存，适合大数据或无限序列
    //
    // Kotlin Sequence ≈ Java Stream（单遍、惰性、不可重用）
    // Kotlin stdlib 源码：kotlin/sequences/Sequences.kt
    //                     kotlin/sequences/SequencesJVM.kt
    // ──────────────────────────────────────────────────────────────
    private fun demo31EagerVsLazy() {
        Lab.section("3.1", "Eager vs Lazy Evaluation")

        val data = (1..10).toList()

        // Eager — creates 2 intermediate lists
        println("Eager (List) processing order:")
        val eagerResult = data
            .map    { print("  map($it) "); it * it }
            .filter { print("  filter($it) "); it % 2 == 0 }
            .take(3)
        println("\n  result: $eagerResult")

        // Lazy — each element flows through the whole pipeline before next is fetched
        println("\nLazy (Sequence) processing order:")
        val lazyResult = data.asSequence()
            .map    { print("  map($it) "); it * it }
            .filter { print("  filter($it) "); it % 2 == 0 }
            .take(3)
            .toList()   // ← terminal operation, triggers evaluation
        println("\n  result: $lazyResult")

        println("\nObserve: Sequence stops after finding 3 matches — fewer elements processed!")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.2 创建 Sequence 的几种方式
    //
    //   sequenceOf(...)          → 固定元素
    //   collection.asSequence() → 包装现有集合
    //   generateSequence(seed) { next } → 无限序列（next 返回 null 则终止）
    //   sequence { yield() }    → 协程构建器，支持挂起（最灵活）
    //
    // generateSequence 原理：
    //   每次调用 next() 时才执行 lambda，非提前计算
    //   seed 为 null 或 lambda 返回 null 则终止
    //
    // Kotlin 协程 sequence builder：
    //   用 suspend fun SequenceScope<T>.yield(value: T) 逐个产出元素
    //   支持复杂的状态机和条件分支
    //   阅读：kotlin/coroutines/Sequence.kt → sequence { }
    // ──────────────────────────────────────────────────────────────
    private fun demo32CreatingSequences() {
        Lab.section("3.2", "Creating Sequences")

        // sequenceOf
        val seq1 = sequenceOf(1, 2, 3, 4, 5)
        println("sequenceOf: ${seq1.toList()}")

        // asSequence from collection
        val seq2 = listOf("a", "b", "c").asSequence().map { it.uppercase() }
        println("asSequence + map: ${seq2.toList()}")

        // generateSequence with termination (null stops it)
        val countdown = generateSequence(10) { if (it > 0) it - 1 else null }
        println("countdown: ${countdown.toList()}")

        // generateSequence — infinite (must use take/first to terminate)
        val naturals = generateSequence(1) { it + 1 }
        println("first 5 naturals: ${naturals.take(5).toList()}")

        // sequence builder (coroutine-based)
        val evens = sequence {
            var n = 0
            while (true) {
                yield(n)
                n += 2
            }
        }
        println("first 6 evens via builder: ${evens.take(6).toList()}")

        // sequence builder with conditional logic
        val collatz = sequence {
            var n = 27
            while (n != 1) {
                yield(n)
                n = if (n % 2 == 0) n / 2 else 3 * n + 1
            }
            yield(1)
        }
        val collatzList = collatz.toList()
        println("Collatz(27): length=${collatzList.size}, max=${collatzList.max()}")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.3 无限序列
    //
    // 无限序列的常见模式：
    //   generateSequence(seed) { f(it) }
    //   必须配合 take(n) / first { } / takeWhile { } 终止
    //
    // 注意：Sequence 不可重用（与 Java Stream 相同）
    //   val s = generateSequence(1) { it + 1 }
    //   s.take(3).toList()   → [1,2,3]
    //   s.take(3).toList()   → 仍然从头开始（Sequence 是惰性工厂，每次重新求值）
    //   但由 sequenceOf / collection.asSequence() 创建的是可重用的！
    // ──────────────────────────────────────────────────────────────
    private fun demo33InfiniteSequences() {
        Lab.section("3.3", "Infinite Sequences")

        // Fibonacci sequence
        val fibonacci = generateSequence(Pair(0L, 1L)) { (a, b) -> Pair(b, a + b) }
            .map { it.first }
        println("first 12 Fibonacci: ${fibonacci.take(12).toList()}")

        // Powers of 2
        val powersOf2 = generateSequence(1L) { it * 2 }
        println("powers of 2 < 1000: ${powersOf2.takeWhile { it < 1000 }.toList()}")

        // Prime sequence using trial division (no composite set — avoids OOM)
        fun primes(): Sequence<Int> = sequence {
            yield(2)
            val found = mutableListOf(2)
            var candidate = 3
            while (true) {
                val sqrtC = Math.sqrt(candidate.toDouble()).toInt()
                if (found.none { p -> p <= sqrtC && candidate % p == 0 }) {
                    yield(candidate)
                    found.add(candidate)
                }
                candidate += 2
            }
        }
        println("first 15 primes: ${primes().take(15).toList()}")

        // Reusability note
        val seq = generateSequence(1) { it + 1 }
        val firstCall  = seq.take(3).toList()
        val secondCall = seq.take(3).toList()
        println("\ngenerateSequence reuse: $firstCall vs $secondCall (same — factory re-evaluates)")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.4 何时使用 Sequence
    //
    //   USE Sequence when:
    //   ┌───────────────────────────────────────────────────────┐
    //   │ 1. 多个链式操作（3+ 个 map/filter）                   │
    //   │    → 避免每步产生中间集合                             │
    //   │ 2. 数据量大（千条以上）                               │
    //   │    → 惰性求值节省内存                                 │
    //   │ 3. 无限数据源                                         │
    //   │    → 只有 Sequence 能表达                             │
    //   │ 4. 存在短路操作（take / first / find）               │
    //   │    → Sequence 找到即停，List 必须全部处理             │
    //   └───────────────────────────────────────────────────────┘
    //
    //   AVOID Sequence when:
    //   ┌───────────────────────────────────────────────────────┐
    //   │ 1. 只有单个操作（overhead 不值得）                    │
    //   │ 2. 小集合（< 100 元素，List 更快）                   │
    //   │ 3. 需要多次遍历（Sequence 需要重新求值）              │
    //   └───────────────────────────────────────────────────────┘
    //
    // 性能基准：小集合时 List 通常快于 Sequence（对象创建开销）
    //            大数据、多步操作时 Sequence 明显占优
    // ──────────────────────────────────────────────────────────────
    private fun demo34WhenToUseSequences() {
        Lab.section("3.4", "When to Use Sequences")

        val largeData = (1..1_000_000).toList()

        // Eager: processes ALL elements in each step
        val eagerMs = measureTimeMillis {
            largeData.filter { it % 3 == 0 }.map { it * it }.take(5)
        }

        // Lazy: stops after finding 5 results
        val lazyMs = measureTimeMillis {
            largeData.asSequence().filter { it % 3 == 0 }.map { it * it }.take(5).toList()
        }

        println("Large data (1M elements), filter+map+take(5):")
        println("  Eager (List):    ${eagerMs}ms")
        println("  Lazy (Sequence): ${lazyMs}ms")
        println("  Sequence processes only ~15 elements instead of 1,000,000!")

        // Short-circuit benefit
        val found = (1..Int.MAX_VALUE).asSequence()
            .filter { it % 7 == 0 && it % 11 == 0 }
            .first()
        println("\nFirst number divisible by 7 and 11: $found  (searched lazily from 1)")
    }
}
