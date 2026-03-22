package studio.sandlight.lang.concurrency

// LEVEL 4: Executors & Futures — 线程池、协调工具、CompletableFuture

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ExecutorsAndFutures {

    fun run() {
        println("\n\uD83D\uDE80 LEVEL 4: EXECUTORS & FUTURES - 线程池、协调工具、异步流水线")
        println("=".repeat(65))

        demo41ThreadPoolVariants()
        demo42ThreadPoolExecutor()
        demo43CountDownLatch()
        demo44CyclicBarrier()
        demo45Semaphore()
        demo46CompletableFuture()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.1 线程池的四种标准类型
    //
    //   Executors.newFixedThreadPool(n)
    //     → 固定 n 个线程，无界 LinkedBlockingQueue
    //     → 适合：CPU 密集型任务，已知并发度上限
    //     → 风险：无界队列可能 OOM（任务堆积时）
    //
    //   Executors.newCachedThreadPool()
    //     → 线程数 0 ~ Integer.MAX_VALUE，空闲线程 60s 回收
    //     → SynchronousQueue（不存储任务，直接交给线程）
    //     → 适合：大量短生命周期任务
    //     → 风险：任务暴增时可创建数万线程 → OOM
    //
    //   Executors.newScheduledThreadPool(n)
    //     → 支持 schedule(delay) / scheduleAtFixedRate
    //     → 内部用 DelayedWorkQueue（最小堆）
    //
    //   Executors.newWorkStealingPool()
    //     → ForkJoinPool，每个线程有自己的双端队列
    //     → 空闲线程"偷"其他队列尾部的任务
    //     → 适合：递归分治任务（fork/join 框架）
    //
    // 阅读：java.util.concurrent.Executors   → 工厂方法
    //       java.util.concurrent.ThreadPoolExecutor → 实现
    //       java.util.concurrent.ForkJoinPool → work-stealing 双端队列
    // ──────────────────────────────────────────────────────────────
    private fun demo41ThreadPoolVariants() {
        println("\n--- 4.1 Thread Pool Variants ---")

        // Fixed — predictable thread count
        val fixed = Executors.newFixedThreadPool(3)
        println("Fixed pool type: ${fixed.javaClass.simpleName}")
        val fixedLatch = CountDownLatch(6)
        repeat(6) { i ->
            fixed.submit {
                Thread.sleep(20)
                fixedLatch.countDown()
            }
        }
        fixedLatch.await()
        fixed.shutdown()
        println("Fixed(3): 6 tasks completed with max 3 threads")

        // Cached — grows unboundedly, recycles idle threads
        val cached = Executors.newCachedThreadPool()
        println("Cached pool type: ${cached.javaClass.simpleName}")
        val cachedLatch = CountDownLatch(5)
        repeat(5) {
            cached.submit {
                Thread.sleep(10)
                cachedLatch.countDown()
            }
        }
        cachedLatch.await()
        cached.shutdown()
        println("Cached: 5 quick tasks done (threads created on demand)")

        // Scheduled — delay and periodic tasks
        val scheduled = Executors.newScheduledThreadPool(1)
        println("Scheduled pool type: ${scheduled.javaClass.simpleName}")
        val scheduledLatch = CountDownLatch(1)
        scheduled.schedule({
            println("  Scheduled task fired after 50ms delay")
            scheduledLatch.countDown()
        }, 50, TimeUnit.MILLISECONDS)
        scheduledLatch.await()
        scheduled.shutdown()

        // Work-stealing — backed by ForkJoinPool
        val workStealing = Executors.newWorkStealingPool()
        println("WorkStealing pool type: ${workStealing.javaClass.simpleName}")
        workStealing.shutdown()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.2 ThreadPoolExecutor 七个核心参数
    //
    //   ThreadPoolExecutor(
    //     corePoolSize,     // 核心线程数：长期保留，不超时
    //     maximumPoolSize,  // 最大线程数 = 核心 + 临时线程
    //     keepAliveTime,    // 临时线程空闲超时时间
    //     unit,             // 超时时间单位
    //     workQueue,        // 任务队列
    //     threadFactory,    // 线程工厂（命名/daemon）
    //     handler           // 拒绝策略
    //   )
    //
    //   任务提交流程：
    //   ┌──────────────────────────────────────────────────────┐
    //   │ 提交任务                                             │
    //   │   ↓  线程数 < coreSize → 创建核心线程               │
    //   │   ↓  队列未满          → 入队等待                   │
    //   │   ↓  线程数 < maxSize  → 创建临时线程               │
    //   │   ↓  执行拒绝策略                                    │
    //   └──────────────────────────────────────────────────────┘
    //
    //   拒绝策略：
    //     AbortPolicy       → 抛 RejectedExecutionException（默认）
    //     CallerRunsPolicy  → 调用者线程直接执行（背压，不丢任务）
    //     DiscardPolicy     → 静默丢弃新任务
    //     DiscardOldestPolicy → 丢弃最旧的任务
    //
    // 阅读：java.util.concurrent.ThreadPoolExecutor → execute()
    //       → addWorker() → Worker.run() → runWorker()
    // ──────────────────────────────────────────────────────────────
    private fun demo42ThreadPoolExecutor() {
        println("\n--- 4.2 ThreadPoolExecutor Custom Config ---")

        val completed = AtomicInteger(0)
        val callerRan = AtomicInteger(0)

        val pool = ThreadPoolExecutor(
            2,                            // corePoolSize
            4,                            // maximumPoolSize
            10L, TimeUnit.SECONDS,        // keepAliveTime
            ArrayBlockingQueue(2),        // bounded queue (capacity=2)
            { r -> Thread(r, "pool-worker-${System.nanoTime() % 1000}") },
            ThreadPoolExecutor.CallerRunsPolicy()  // reject → caller executes
        )

        val latch = CountDownLatch(8)
        repeat(8) { i ->
            val submitter = Thread.currentThread().name
            pool.execute {
                val threadName = Thread.currentThread().name
                if (threadName == submitter) callerRan.incrementAndGet()
                Thread.sleep(80)
                completed.incrementAndGet()
                latch.countDown()
            }
        }
        latch.await()
        pool.shutdown()

        println("  8 tasks submitted: core=2, max=4, queue=2")
        println("  All completed=${completed.get()}")
        println("  Tasks run by caller (CallerRunsPolicy): ${callerRan.get()}")
        println("  → tasks beyond (core + queue + extra) are run by the submitting thread")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.3 CountDownLatch — 一次性倒计时门闩
    //
    //   用途：等待 N 个并发操作全部完成（fan-out + fan-in）
    //
    //   ┌─────────┐              ┌───────────┐
    //   │  Main   │  latch.await()│  Worker1  │ latch.countDown()
    //   │ Thread  │ ◄────────────┤  Worker2  │ latch.countDown()
    //   │ (waits) │  until 0    │  Worker3  │ latch.countDown()
    //   └─────────┘             └───────────┘
    //
    //   特点：
    //     • 一次性，count 到 0 后不能重置
    //     • 内部用 AQS.state 存储计数
    //     • countDown() → releaseShared(1) → 唤醒所有 await() 线程
    //
    // 阅读：java.util.concurrent.CountDownLatch → Sync extends AQS
    //       → tryAcquireShared() 等到 state==0 才返回
    // ──────────────────────────────────────────────────────────────
    private fun demo43CountDownLatch() {
        println("\n--- 4.3 CountDownLatch (fan-out + fan-in) ---")

        val workerCount = 5
        val latch = CountDownLatch(workerCount)
        val start = System.currentTimeMillis()

        repeat(workerCount) { i ->
            Thread {
                val sleepMs = (i + 1) * 30L
                Thread.sleep(sleepMs)
                println("  worker-$i done after ${sleepMs}ms")
                latch.countDown()
            }.also { it.isDaemon = true; it.start() }
        }

        latch.await()
        val elapsed = System.currentTimeMillis() - start
        println("  All $workerCount workers done, elapsed=${elapsed}ms")
        println("  (max worker was 150ms — parallel execution confirmed)")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.4 CyclicBarrier — 可重用的集合点屏障
    //
    //   vs CountDownLatch：
    //     CountDownLatch：一次性，主线程等子线程
    //     CyclicBarrier ：可重用，所有参与方互相等待（同步点）
    //
    //   barrier.await() — 每个线程到达后阻塞，
    //     当第 N 个线程到达时，所有线程同时释放，
    //     并执行 barrierAction（由最后到达的线程运行）
    //
    // 阅读：java.util.concurrent.CyclicBarrier
    //       → dowait() — 递减 count，最后一个到达时 nextGeneration()
    // ──────────────────────────────────────────────────────────────
    private fun demo44CyclicBarrier() {
        println("\n--- 4.4 CyclicBarrier (multi-phase computation) ---")

        val parties = 3
        var phase = 0

        val barrier = CyclicBarrier(parties) {
            phase++
            println("  ── Barrier action: all reached phase $phase, advancing ──")
        }

        val doneLatch = CountDownLatch(parties)
        repeat(parties) { i ->
            Thread {
                // Phase 1 work
                Thread.sleep((i + 1) * 20L)
                println("  [thread-$i] finished phase 1 work, waiting at barrier")
                barrier.await()

                // Phase 2 work
                Thread.sleep((parties - i) * 20L)
                println("  [thread-$i] finished phase 2 work, waiting at barrier")
                barrier.await()

                doneLatch.countDown()
            }.also { it.isDaemon = true; it.start() }
        }

        doneLatch.await()
        println("  All $parties threads completed both phases (barrier reused ✓)")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.5 Semaphore — 信号量（限流 / 资源池）
    //
    //   概念：permits 计数器
    //     acquire() → permits-- （若为 0 则阻塞）
    //     release() → permits++
    //
    //   典型用途：限制同时访问资源的线程数（如连接池 permits=10）
    //
    //   ┌──────────────────────────────────────────────┐
    //   │  Connection Pool (permits = 2)               │
    //   │                                              │
    //   │  Thread1 ─► acquire() ─► use ─► release()  │
    //   │  Thread2 ─► acquire() ─► use ─► release()  │
    //   │  Thread3 ─► acquire()  BLOCKED until above   │
    //   └──────────────────────────────────────────────┘
    //
    // 阅读：java.util.concurrent.Semaphore → Sync extends AQS
    //       → tryAcquireShared() — 尝试递减 permits
    // ──────────────────────────────────────────────────────────────
    private fun demo45Semaphore() {
        println("\n--- 4.5 Semaphore (connection pool simulation) ---")

        val permits = 2
        val semaphore = Semaphore(permits)
        val active = AtomicInteger(0)
        var maxObserved = 0
        val lock = Any()

        val doneLatch = CountDownLatch(5)
        repeat(5) { i ->
            Thread {
                semaphore.acquire()
                val current = active.incrementAndGet()
                synchronized(lock) { if (current > maxObserved) maxObserved = current }
                println("  [thread-$i] acquired (active=$current/${permits})")
                Thread.sleep(60)
                active.decrementAndGet()
                semaphore.release()
                println("  [thread-$i] released")
                doneLatch.countDown()
            }.also { it.isDaemon = true; it.start() }
        }

        doneLatch.await()
        println("  Max concurrent threads in critical section: $maxObserved (limit=$permits) ✓")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.6 CompletableFuture — 异步流水线（Java 8+）
    //
    //   CompletableFuture ≈ Promise + callback chain
    //
    //   核心 API：
    //     supplyAsync { }          → 异步执行，返回结果（ForkJoinPool）
    //     thenApply { }            → 同步变换（类似 map）
    //     thenApplyAsync { }       → 异步变换（新线程）
    //     thenCompose { }          → 链接另一个 CF（flatMap，避免嵌套 CF）
    //     thenCombine(cf2) { }     → 合并两个独立 CF 的结果
    //     allOf(cf1, cf2, ...)     → 等待所有完成
    //     anyOf(cf1, cf2, ...)     → 第一个完成即返回
    //     exceptionally { }        → 异常兜底
    //     handle { res, ex }       → 无论成功失败都执行
    //
    //   ┌────────────────────────────────────────────────────────┐
    //   │  supplyAsync          thenApply     thenApply          │
    //   │  [fetch number] ───► [double it] ► [to string] ──┐    │
    //   │                                                   │    │
    //   │  supplyAsync                         thenCombine ◄┘    │
    //   │  [fetch config] ──────────────────► [merge both]  ──► │
    //   └────────────────────────────────────────────────────────┘
    //
    // 阅读：java.util.concurrent.CompletableFuture
    //       → UniApply (thenApply 节点) → tryFire() → fn.apply()
    //       → BiApply  (thenCombine 节点)
    // ──────────────────────────────────────────────────────────────
    private fun demo46CompletableFuture() {
        println("\n--- 4.6 CompletableFuture Pipeline ---")

        // thenApply chain: fetch → double → to string
        val chain = CompletableFuture.supplyAsync {
            Thread.sleep(30); 21
        }.thenApply { it * 2
        }.thenApply { "result=$it" }
        println("thenApply chain: ${chain.get()}")

        // thenCompose: flatMap — avoid nested CF<CF<T>>
        val composed = CompletableFuture.supplyAsync {
            Thread.sleep(20); "userId-42"
        }.thenCompose { userId ->
            CompletableFuture.supplyAsync {
                Thread.sleep(20); "User{id=$userId, name=Alice}"
            }
        }
        println("thenCompose: ${composed.get()}")

        // thenCombine: merge two independent futures
        val price  = CompletableFuture.supplyAsync { Thread.sleep(30); 100 }
        val tax    = CompletableFuture.supplyAsync { Thread.sleep(20); 8  }
        val total  = price.thenCombine(tax) { p, t -> p + t }
        println("thenCombine (price+tax): ${total.get()}")

        // allOf: wait for all 3 parallel tasks
        val tasks = (1..3).map { i ->
            CompletableFuture.supplyAsync { Thread.sleep(i * 20L); "task-$i done" }
        }
        val allDone = CompletableFuture.allOf(*tasks.toTypedArray())
        allDone.get()
        println("allOf: ${tasks.map { it.get() }}")

        // exceptionally: handle failure
        val risky = CompletableFuture.supplyAsync<String> {
            Thread.sleep(10)
            throw RuntimeException("network error")
        }.exceptionally { ex -> "fallback: ${ex.message}" }
        println("exceptionally: ${risky.get()}")

        // handle: always runs (success or failure)
        val withHandle = CompletableFuture.supplyAsync { Thread.sleep(10); 42 }
            .handle { result, ex ->
                if (ex != null) "error: ${ex.message}" else "success: $result"
            }
        println("handle (success): ${withHandle.get()}")
    }
}
