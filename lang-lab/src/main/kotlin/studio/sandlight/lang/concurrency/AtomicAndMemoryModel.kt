package studio.sandlight.lang.concurrency

// LEVEL 3: Atomic Operations & Java Memory Model — 原子操作与内存模型

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicStampedReference
import java.util.concurrent.atomic.LongAdder

object AtomicAndMemoryModel {

    fun run() {
        println("\n⚛️  LEVEL 3: ATOMIC OPERATIONS & JAVA MEMORY MODEL - 原子操作与内存模型")
        println("=".repeat(60))

        demo31AtomicIntegerBasics()
        demo32CasAndAba()
        demo33LongAdderVsAtomicLong()
        demo34AtomicReference()
        demo35JavaMemoryModel()
    }

    // ──────────────────────────────────────────────────────────────
    // 3.1 AtomicInteger Basics
    //
    // AtomicInteger — 无锁原子整数
    //
    //   原理：CPU 的 CAS 硬件指令（x86: CMPXCHG，ARM: LDREX/STREX）
    //         在硬件层面保证"读-比较-写"是不可中断的原子操作
    //
    //   常用方法：
    //     get() / set(v)             → 普通读写（volatile 语义）
    //     getAndSet(v)               → 原子交换，返回旧值
    //     incrementAndGet()          → ++i（原子）
    //     getAndIncrement()          → i++（原子）
    //     getAndAdd(delta)           → i += delta（原子）
    //     compareAndSet(expect, upd) → CAS 核心：仅当 == expect 时才写 upd
    //     updateAndGet { it * 2 }    → 原子 lambda 更新（内部 CAS 循环）
    //
    // 阅读：java.util.concurrent.atomic.AtomicInteger
    //       → compareAndSet() → Unsafe.compareAndSetInt()
    //       → JDK 9+: VarHandle.compareAndSet() 取代 Unsafe
    // ──────────────────────────────────────────────────────────────
    private fun demo31AtomicIntegerBasics() {
        println("\n--- 3.1 AtomicInteger Basics ---")

        // Thread-safe counter: 4 threads × 100_000 increments = 400_000
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(4)
        repeat(4) {
            Thread {
                repeat(100_000) { counter.incrementAndGet() }
                latch.countDown()
            }.start()
        }
        latch.await()
        println("4 threads × 100,000 increments = ${counter.get()} (expected 400000)")

        // Demonstrate all key methods
        val a = AtomicInteger(10)

        println("get()            = ${a.get()}")           // 10

        val old = a.getAndSet(99)
        println("getAndSet(99)    old=$old, now=${a.get()}") // old=10, now=99

        a.set(5)
        println("incrementAndGet()= ${a.incrementAndGet()}") // 6

        println("getAndIncrement()= ${a.getAndIncrement()}") // 6 (returns before)
        println("  after          = ${a.get()}")             // 7

        println("getAndAdd(3)     = ${a.getAndAdd(3)}")      // 7
        println("  after          = ${a.get()}")             // 10

        val casOk = a.compareAndSet(10, 42)
        println("CAS(10→42) ok?   = $casOk, now=${a.get()}") // true, 42

        val casFail = a.compareAndSet(10, 99)
        println("CAS(10→99) ok?   = $casFail, now=${a.get()}") // false, 42

        // updateAndGet: atomic lambda — internally CAS loops until success
        val doubled = a.updateAndGet { it * 2 }
        println("updateAndGet×2   = $doubled") // 84
    }

    // ──────────────────────────────────────────────────────────────
    // 3.2 CAS and ABA Problem
    //
    // CAS（Compare-And-Swap）原理与 ABA 问题
    //
    //   CAS 伪代码：
    //     atomic {
    //       if (memory[addr] == expected) {
    //         memory[addr] = newValue
    //         return true
    //       }
    //       return false
    //     }
    //
    //   ABA 问题：
    //     线程 T1 读取值 A
    //     线程 T2 将 A → B → A
    //     线程 T1 做 CAS(A, C)，成功！— 但实际状态已经变化过
    //
    //     例：链表节点复用场景，ABA 可能导致结构损坏
    //
    //   解决：AtomicStampedReference<V>
    //     每次修改同时更新版本号（stamp）
    //     CAS 必须同时匹配值和版本号
    //
    // 阅读：java.util.concurrent.atomic.AtomicStampedReference
    //       → compareAndSet(expected, new, expectedStamp, newStamp)
    // ──────────────────────────────────────────────────────────────
    private fun demo32CasAndAba() {
        println("\n--- 3.2 CAS and ABA Problem ---")

        // Manual CAS retry loop — increment without locks
        val value = AtomicInteger(0)
        fun casIncrement(atomic: AtomicInteger): Int {
            while (true) {
                val current = atomic.get()
                val next = current + 1
                if (atomic.compareAndSet(current, next)) return next
                // Another thread changed value; retry
            }
        }
        val latch1 = CountDownLatch(4)
        repeat(4) {
            Thread {
                repeat(1_000) { casIncrement(value) }
                latch1.countDown()
            }.start()
        }
        latch1.await()
        println("CAS retry loop: 4×1000 = ${value.get()} (expected 4000)")

        // ABA scenario with plain AtomicReference
        //   T1 reads "A", T2 changes A→B→A, T1 CAS(A→C) succeeds — unaware of mutation
        val ref = AtomicReference("A")
        val t1Snapshot = ref.get()  // T1 reads "A"

        // Simulate T2: A → B → A
        ref.set("B")
        ref.set("A")

        val abaSucceeded = ref.compareAndSet(t1Snapshot, "C")
        println("ABA with AtomicReference: CAS succeeded = $abaSucceeded, value = ${ref.get()}")
        println("  (T1 did not notice the A→B→A change!)")

        // Fix: AtomicStampedReference — stamp acts as version counter
        val stampedRef = AtomicStampedReference("A", 0)
        val stampHolder = IntArray(1)
        val t1Value = stampedRef.get(stampHolder)
        val t1Stamp = stampHolder[0]

        // T2: A → B → A, bumping stamp each time
        stampedRef.compareAndSet("A", "B", 0, 1)
        stampedRef.compareAndSet("B", "A", 1, 2)

        val fixedCas = stampedRef.compareAndSet(t1Value, "C", t1Stamp, t1Stamp + 1)
        println("ABA with AtomicStampedReference: CAS succeeded = $fixedCas")
        println("  (stamp mismatch detected — ABA prevented!)")
        println("  current value=${stampedRef.reference}, stamp=${stampedRef.stamp}")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.3 LongAdder vs AtomicLong
    //
    // LongAdder — 高并发下比 AtomicLong 更快
    //
    //   AtomicLong 的问题：高争用时大量 CAS 失败，线程自旋重试
    //
    //   LongAdder 的解法（Striped64）：
    //     ┌────────┬────────┬────────┬────────┐
    //     │ base   │ Cell[0]│ Cell[1]│ Cell[2]│  ← 每个 Cell 对齐到缓存行（@Contended）
    //     └────────┴────────┴────────┴────────┘
    //          ↑        ↑        ↑        ↑
    //       Thread0  Thread1  Thread2  Thread3 各自写自己的 Cell，无竞争
    //
    //     sum() = base + Cell[0] + Cell[1] + ...（非原子，快照）
    //
    //   适合：统计计数、吞吐量指标等
    //   不适合：需要精确实时值的场景（sum() 是近似快照）
    //
    // 阅读：java.util.concurrent.atomic.LongAdder → add()
    //       java.util.concurrent.atomic.Striped64 → longAccumulate()
    // ──────────────────────────────────────────────────────────────
    private fun demo33LongAdderVsAtomicLong() {
        println("\n--- 3.3 LongAdder vs AtomicLong ---")

        val threads = 8
        val increments = 1_000_000

        // AtomicLong benchmark
        val atomicLong = AtomicLong(0)
        val latch1 = CountDownLatch(threads)
        val t1 = System.nanoTime()
        repeat(threads) {
            Thread {
                repeat(increments) { atomicLong.incrementAndGet() }
                latch1.countDown()
            }.start()
        }
        latch1.await()
        val atomicMs = (System.nanoTime() - t1) / 1_000_000

        // LongAdder benchmark
        val longAdder = LongAdder()
        val latch2 = CountDownLatch(threads)
        val t2 = System.nanoTime()
        repeat(threads) {
            Thread {
                repeat(increments) { longAdder.increment() }
                latch2.countDown()
            }.start()
        }
        latch2.await()
        val adderMs = (System.nanoTime() - t2) / 1_000_000

        println("AtomicLong  : ${atomicLong.get()} in ${atomicMs}ms")
        println("LongAdder   : ${longAdder.sum()} in ${adderMs}ms")
        println("LongAdder faster? ${adderMs < atomicMs}")
        println("Note: LongAdder.sum() is a snapshot — not atomic across cells")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.4 AtomicReference — Lock-Free Stack
    //
    // AtomicReference — 无锁数据结构示例
    //
    //   无锁栈（Treiber Stack）：
    //
    //     push(val):
    //       loop:
    //         oldHead = head.get()
    //         newHead = Node(val, next=oldHead)
    //         if (head.CAS(oldHead, newHead)) return  ← CAS 成功则退出
    //         // 否则另一个线程已修改 head，重试
    //
    //     pop():
    //       loop:
    //         oldHead = head.get()
    //         if oldHead == null: return null  ← 空栈
    //         if (head.CAS(oldHead, oldHead.next)) return oldHead.val
    //
    //   特点：完全无锁，但在极高争用下重试次数多
    //
    // 阅读：java.util.concurrent.ConcurrentLinkedQueue — 工业级无锁队列
    //       基于 Michael-Scott 队列算法（双 CAS：head + tail）
    // ──────────────────────────────────────────────────────────────
    private fun demo34AtomicReference() {
        println("\n--- 3.4 AtomicReference — Lock-Free Stack ---")

        val stack = LockFreeStack<Int>()
        val latch = CountDownLatch(4)
        val pushed = AtomicInteger(0)
        val popped = AtomicInteger(0)

        // 2 producer threads push 50 items each
        repeat(2) { tid ->
            Thread {
                repeat(50) { i ->
                    stack.push(tid * 50 + i)
                    pushed.incrementAndGet()
                }
                latch.countDown()
            }.start()
        }

        // 2 consumer threads pop items
        repeat(2) {
            Thread {
                repeat(50) {
                    if (stack.pop() != null) popped.incrementAndGet()
                    else Thread.yield()
                }
                latch.countDown()
            }.start()
        }

        latch.await()
        println("Lock-free stack: pushed=${pushed.get()}, popped=${popped.get()}")
        println("Items remaining on stack: ${stack.size()}")
    }

    // ──────────────────────────────────────────────────────────────
    // 3.5 Java Memory Model (JMM)
    //
    // Java 内存模型（JMM）— happens-before 规则
    //
    //   JMM 定义了多线程程序中内存操作的可见性规则。
    //   "A happens-before B" 意味着 A 的结果对 B 可见。
    //
    //   核心规则：
    //   ┌────────────────────────────────────────────────────────┐
    //   │ 1. 程序顺序：同一线程内，前面的操作 hb 后面的操作     │
    //   │ 2. 监视器锁：unlock hb 后续对同一锁的 lock           │
    //   │ 3. volatile：写操作 hb 后续对同一变量的读操作         │
    //   │ 4. 线程启动：Thread.start() hb 线程内的所有操作       │
    //   │ 5. 线程结束：线程内的所有操作 hb Thread.join() 返回   │
    //   └────────────────────────────────────────────────────────┘
    //
    //   常见可见性问题（无 hb 保证时）：
    //     写：Thread A: data = 42; ready = true   (无 volatile)
    //     读：Thread B: while (!ready); use(data) → 可能看到旧值
    //
    //   修复：将 ready 声明为 @Volatile（建立 hb），
    //         或用 synchronized 保护（unlock hb lock）
    // ──────────────────────────────────────────────────────────────
    private fun demo35JavaMemoryModel() {
        println("\n--- 3.5 Java Memory Model (JMM) ---")

        // Without @Volatile: no happens-before guarantee
        // The JVM / JIT may cache `ready` in a register; reader thread may spin forever.
        // We illustrate the concept with a comment rather than an infinite loop.
        println("Without @Volatile: JIT may cache 'ready' in register — reader sees stale value")
        println("  writer: data = 42; ready = true")
        println("  reader: while (!ready) {}  // may loop forever — no HB guarantee")

        // Fix 1: @Volatile establishes happens-before on 'ready'
        //   write(ready=true) hb read(ready)  → data=42 is also visible
        //   Note: @Volatile must annotate a class-level field (not a local variable)
        //   See: jmmReady / jmmData fields declared at object level below
        jmmData = 0
        jmmReady = false

        val writer = Thread {
            jmmData = 42
            jmmReady = true   // volatile write — publishes jmmData=42
        }
        val reader = Thread {
            while (!jmmReady) { /* spin — safe with @Volatile */ }
            println("@Volatile fix: data visible = $jmmData (expected 42)")
        }
        writer.start(); reader.start()
        writer.join();  reader.join()

        // Fix 2: happens-before via Thread.join()
        //   All writes in worker hb Thread.join() returns in main
        var sharedValue = 0
        val worker = Thread { sharedValue = 99 }
        worker.start()
        worker.join()   // HB rule 5: worker's write is visible after join()
        println("join() HB: sharedValue = $sharedValue (expected 99, always safe)")

        println("\nJMM happens-before summary:")
        println("  volatile write  hb  subsequent volatile read  (same variable)")
        println("  Thread.start()  hb  any action in that thread")
        println("  any action in T hb  Thread.join() of T returns")
        println("  unlock(m)       hb  subsequent lock(m)")
    }

    // ──────────────────────────────────────────────────────────────
    // @Volatile fields for JMM demo (must be class-level — not local vars)
    // ──────────────────────────────────────────────────────────────
    @Volatile private var jmmReady = false
    private var jmmData = 0

    // ──────────────────────────────────────────────────────────────
    // Treiber Lock-Free Stack implementation
    // ──────────────────────────────────────────────────────────────
    private class Node<T>(val value: T, val next: Node<T>?)

    private class LockFreeStack<T> {
        private val head = AtomicReference<Node<T>?>(null)

        fun push(value: T) {
            while (true) {
                val oldHead = head.get()
                val newHead = Node(value, oldHead)
                if (head.compareAndSet(oldHead, newHead)) return
            }
        }

        fun pop(): T? {
            while (true) {
                val oldHead = head.get() ?: return null
                if (head.compareAndSet(oldHead, oldHead.next)) return oldHead.value
            }
        }

        fun size(): Int {
            var count = 0
            var node = head.get()
            while (node != null) { count++; node = node.next }
            return count
        }
    }
}
