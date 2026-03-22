package studio.sandlight.lang.concurrency

// LEVEL 1: Threads & Basic Synchronization — 线程、synchronized、volatile

import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object ThreadsAndSync {

    fun run() {
        println("\n  LEVEL 1: THREADS & BASIC SYNCHRONIZATION - 线程与基础同步")
        println("=".repeat(60))

        demo11ThreadLifecycle()
        demo12ThreadCreation()
        demo13ThreadCoordination()
        demo14Synchronized()
        demo15Volatile()
    }

    // ──────────────────────────────────────────────────────────────
    // 1.1 线程生命周期 (Thread Lifecycle)
    //
    // Thread State Machine:
    //
    //   ┌─────────┐   start()    ┌───────────┐
    //   │   NEW   │ ──────────► │  RUNNABLE  │ ◄─── scheduler decides
    //   └─────────┘             └─────┬──────┘
    //                                 │
    //           ┌────────────────┬────┴────────────────┐
    //           ▼                ▼                     ▼
    //   ┌──────────────┐  ┌───────────────┐  ┌──────────────────┐
    //   │   BLOCKED    │  │    WAITING    │  │  TIMED_WAITING   │
    //   │ (waiting for │  │ Object.wait() │  │ sleep(ms)        │
    //   │  monitor)    │  │ join(noArg)   │  │ wait(ms)         │
    //   │              │  │ LockSupport   │  │ join(ms)         │
    //   └──────┬───────┘  └──────┬────────┘  └────────┬─────────┘
    //          └─────────────────┴────────────────┐   │
    //                                             ▼   ▼
    //                                       ┌──────────────┐
    //                                       │  TERMINATED  │
    //                                       └──────────────┘
    //
    // 状态由 JVM 管理，通过 Thread.getState() 查询。
    // 阅读：java.lang.Thread.State (enum, JDK 5+)
    //       java.lang.Thread#start() → native start0()
    // ──────────────────────────────────────────────────────────────
    private fun demo11ThreadLifecycle() {
        println("\n--- 1.1 Thread Lifecycle & States ---")

        val t = Thread {
            Thread.sleep(80)   // TIMED_WAITING during sleep
        }

        println("  Before start: state = ${t.state}")          // NEW

        t.start()
        Thread.sleep(20)   // give the thread time to reach sleep()
        println("  While sleeping: state = ${t.state}")        // TIMED_WAITING

        t.join()
        println("  After join:    state = ${t.state}")         // TERMINATED
    }

    // ──────────────────────────────────────────────────────────────
    // 1.2 线程创建方式 (Thread Creation)
    //
    //   方式一：继承 Thread 类      → 单继承限制，一般不推荐
    //   方式二：实现 Runnable 接口  → 推荐，分离任务与线程
    //   方式三：Kotlin kotlin.concurrent.thread { } → 语法糖，返回已启动线程
    //
    // 守护线程 (Daemon Thread)：
    //   - isDaemon = true → JVM 退出时不等待守护线程完成
    //   - 适合后台服务（GC 线程本身就是守护线程）
    //   - 必须在 start() 之前设置，否则抛 IllegalThreadStateException
    //
    // 阅读：java.lang.Thread#isDaemon() / setDaemon()
    //       kotlin.concurrent.thread() → Thread.kt in kotlin-stdlib
    // ──────────────────────────────────────────────────────────────
    private fun demo12ThreadCreation() {
        println("\n--- 1.2 Thread Creation — 3 styles ---")

        // Style 1: Thread subclass — 继承 Thread
        class WorkerThread : Thread("worker-subclass") {
            override fun run() {
                println("  [Style 1] name=$name, isDaemon=$isDaemon")
            }
        }
        val t1 = WorkerThread()
        t1.start(); t1.join()

        // Style 2: Runnable interface — 实现 Runnable，传入 Thread 构造器
        val t2 = Thread(
            { println("  [Style 2] name=${Thread.currentThread().name}, isDaemon=${Thread.currentThread().isDaemon}") },
            "worker-runnable"
        )
        t2.start(); t2.join()

        // Style 3: Kotlin stdlib convenience — kotlin.concurrent.thread { }
        // Returns an already-started Thread; no need to call start() manually.
        val t3 = thread(name = "worker-lambda") {
            println("  [Style 3] name=${Thread.currentThread().name}, isDaemon=${Thread.currentThread().isDaemon}")
        }
        t3.join()

        // Daemon thread — JVM exits without waiting for it
        val daemon = thread(name = "daemon-bg", isDaemon = true) {
            println("  [Daemon]  name=${Thread.currentThread().name}, isDaemon=${Thread.currentThread().isDaemon}")
        }
        daemon.join()
        println("  Daemon threads: JVM will NOT wait for them at shutdown")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.3 线程协调 (Thread Coordination)
    //
    //   Thread.sleep(ms) → 当前线程放弃 CPU，进入 TIMED_WAITING
    //                       不释放持有的锁（与 Object.wait() 的关键区别）
    //
    //   thread.join()    → 调用方阻塞，直到目标线程 TERMINATED
    //
    //   thread.interrupt() → 设置中断标志位，不强制停止线程！
    //     - 若线程正在 sleep/wait/join → 立即抛 InterruptedException
    //     - 若线程正在运行 → isInterrupted() 变为 true，线程自行检查
    //
    // 协作式取消模式 (Cooperative Cancellation)：
    //   ┌─────────────────────────────────────────────────┐
    //   │  while (!Thread.currentThread().isInterrupted)  │
    //   │      doWork()   // 定期检查中断标志              │
    //   └─────────────────────────────────────────────────┘
    //
    // 阅读：java.lang.Thread#interrupt() / isInterrupted()
    //       java.lang.InterruptedException
    // ──────────────────────────────────────────────────────────────
    private fun demo13ThreadCoordination() {
        println("\n--- 1.3 Thread Coordination: sleep / join / interrupt ---")

        // join() — wait for completion
        val worker = thread(name = "coord-worker", start = false) {
            Thread.sleep(30)
            println("  [join] worker finished after 30ms sleep")
        }
        worker.start()
        worker.join()   // main blocks here until worker is TERMINATED
        println("  [join] main resumed after worker.join()")

        // interrupt() + cooperative cancellation
        val cancellable = thread(name = "cancellable", start = false) {
            var iterations = 0
            while (!Thread.currentThread().isInterrupted) {
                iterations++
                if (iterations >= 500_000) break  // safety bound
            }
            println("  [interrupt] cooperative loop exited after $iterations iterations" +
                    " (interrupted=${Thread.currentThread().isInterrupted})")
        }
        cancellable.start()
        Thread.sleep(5)
        cancellable.interrupt()  // sets flag; loop will notice it on next check
        cancellable.join()

        // InterruptedException: thrown when sleeping thread is interrupted
        val sleeper = thread(name = "sleeper", start = false) {
            try {
                Thread.sleep(10_000)  // would sleep 10 s without interruption
            } catch (e: InterruptedException) {
                println("  [interrupt] InterruptedException caught — thread was sleeping when interrupted")
                // NOTE: catching InterruptedException CLEARS the flag; re-interrupt if needed
            }
        }
        sleeper.start()
        Thread.sleep(20)
        sleeper.interrupt()
        sleeper.join()
    }

    // ──────────────────────────────────────────────────────────────
    // 1.4 synchronized — 内置锁 / 监视器锁 (Intrinsic Lock / Monitor)
    //
    // 每个 Java/Kotlin 对象都持有一个隐式 monitor（监视器）。
    // synchronized 在字节码层面编译为：
    //
    //   MONITORENTER  ← 获取对象 monitor（独占）
    //   ...body...
    //   MONITOREXIT   ← 释放 monitor（异常路径也有一条 MONITOREXIT）
    //
    // 临界区 (Critical Section)：
    //   同一时刻只有一个线程可执行 synchronized 块 / 方法。
    //
    // Kotlin 注解等价：
    //   @Synchronized fun foo() { ... }  ≡  synchronized(this) { ... }
    //
    // 阅读：java.lang.Object#wait() / notify() / notifyAll()
    //       JVM Spec §6.5: monitorenter / monitorexit
    // ──────────────────────────────────────────────────────────────
    private fun demo14Synchronized() {
        println("\n--- 1.4 synchronized — Race Condition vs Monitor Lock ---")

        // ── Race condition (unsynchronized) ──
        var unsafeCounter = 0
        val t1 = Thread { repeat(100_000) { unsafeCounter++ } }
        val t2 = Thread { repeat(100_000) { unsafeCounter++ } }
        t1.start(); t2.start(); t1.join(); t2.join()
        println("  Unsynchronized counter (expect 200000): $unsafeCounter  <- likely WRONG")

        // ── Fix 1: synchronized block ──
        val lock = Any()
        var safeCounter = 0
        val t3 = Thread { repeat(100_000) { synchronized(lock) { safeCounter++ } } }
        val t4 = Thread { repeat(100_000) { synchronized(lock) { safeCounter++ } } }
        t3.start(); t4.start(); t3.join(); t4.join()
        println("  synchronized block   (expect 200000): $safeCounter  <- always correct")

        // ── Fix 2: @Synchronized method ──
        val counter = SynchronizedCounter()
        val t5 = Thread { repeat(100_000) { counter.increment() } }
        val t6 = Thread { repeat(100_000) { counter.increment() } }
        t5.start(); t6.start(); t5.join(); t6.join()
        println("  @Synchronized method (expect 200000): ${counter.value}  <- always correct")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.5 volatile — 可见性保证 (Visibility Guarantee)
    //
    // 现代 CPU 有多级缓存，每个核心可能将变量缓存在寄存器中，
    // 导致一个线程的写操作对另一个线程不可见（可见性问题）。
    //
    //   ┌─────────┐  write flag=true  ┌────────────┐
    //   │ Thread A│ ────────────────► │ CPU Cache  │  未刷入主存
    //   └─────────┘                   └────────────┘
    //                                       ≠
    //   ┌─────────┐  read flag(stale) ┌────────────┐
    //   │ Thread B│ ◄───────────────  │ CPU Cache  │  读到旧值
    //   └─────────┘                   └────────────┘
    //
    // @Volatile 语义：
    //   - 写：立即刷入主内存，插入 StoreLoad 内存屏障
    //   - 读：直接从主内存读取，绕过 CPU cache
    //   ✓ 保证可见性 (Visibility)
    //   ✗ 不保证原子性 (Atomicity) — count++ 是 读-改-写 三步操作
    //
    // 对比：AtomicInteger（Level 3）使用 CAS 指令，同时保证可见性和原子性。
    //
    // 阅读：JVM Spec §8.3.1.4: volatile fields
    //       java.lang.invoke.VarHandle (JDK 9+) → 更精细的内存语义控制
    // ──────────────────────────────────────────────────────────────
    private fun demo15Volatile() {
        println("\n--- 1.5 volatile — Visibility vs Atomicity ---")

        // ── Visibility: volatile flag stops a spinning loop in another thread ──
        val shared = VolatileFlag()
        val checker = thread(name = "checker") {
            var spins = 0
            while (!shared.running) { spins++ }
            println("  [volatile] checker saw running=true after $spins spins")
        }
        Thread.sleep(10)
        shared.running = true   // volatile write → immediately visible to checker
        checker.join()

        // ── Atomicity: volatile does NOT make ++ atomic ──
        // count++ compiles to: READ count, ADD 1, WRITE count (3 separate ops).
        // Two threads can interleave those ops and lose increments.
        var volatileCount = 0
        val a = Thread { repeat(50_000) { volatileCount++ } }
        val b = Thread { repeat(50_000) { volatileCount++ } }
        a.start(); b.start(); a.join(); b.join()
        println("  volatile count++ (expect 100000): $volatileCount  <- likely WRONG (not atomic)")

        // ── AtomicInteger: both visible AND atomic (preview of Level 3) ──
        // incrementAndGet() → single CAS instruction, hardware-level atomicity
        val atomicCount = AtomicInteger(0)
        val c = Thread { repeat(50_000) { atomicCount.incrementAndGet() } }
        val d = Thread { repeat(50_000) { atomicCount.incrementAndGet() } }
        c.start(); d.start(); c.join(); d.join()
        println("  AtomicInteger    (expect 100000): ${atomicCount.get()}  <- always correct (CAS)")
        println("  (Full AtomicInteger / CAS coverage -> Level 3: AtomicAndMemoryModel)")
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 1 Support Classes
// ══════════════════════════════════════════════════════════════════

/** Demonstrates @Synchronized on a method — compiles to synchronized(this) { } */
private class SynchronizedCounter {
    var value = 0
        private set

    @Synchronized
    fun increment() { value++ }
}

/** Holds a @Volatile flag to demonstrate cross-thread visibility */
private class VolatileFlag {
    @Volatile var running: Boolean = false
}
