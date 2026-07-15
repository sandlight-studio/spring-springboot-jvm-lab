package studio.sandlight.lang.concurrency

import studio.sandlight.lang.support.Level
import studio.sandlight.lang.support.Topic

// ══════════════════════════════════════════════════════════════════
// Java / Kotlin Concurrency — Four Levels
//
// Java 并发分层架构（从低层到高层）：
//
//   ┌──────────────────────────────────────────────────────────────┐
//   │  LEVEL 4: java.util.concurrent                              │
//   │    Executors / ThreadPoolExecutor / CompletableFuture        │
//   │    CountDownLatch / CyclicBarrier / Semaphore                │
//   ├──────────────────────────────────────────────────────────────┤
//   │  LEVEL 3: java.util.concurrent.atomic                        │
//   │    AtomicInteger / AtomicReference / LongAdder               │
//   │    CAS (Compare-And-Swap) / Java Memory Model (JMM)         │
//   ├──────────────────────────────────────────────────────────────┤
//   │  LEVEL 2: java.util.concurrent.locks                        │
//   │    ReentrantLock / ReadWriteLock / Condition / StampedLock   │
//   ├──────────────────────────────────────────────────────────────┤
//   │  LEVEL 1: java.lang.Thread / synchronized / volatile        │
//   │    Thread lifecycle / monitor / intrinsic lock               │
//   └──────────────────────────────────────────────────────────────┘
//
// 核心 JDK 入口：
//   java.lang.Thread                     → start() → start0() (native)
//   java.lang.Object                     → wait() / notify() / notifyAll()
//   java.util.concurrent.locks.AbstractQueuedSynchronizer (AQS)
//     → ReentrantLock / CountDownLatch / Semaphore 的骨干
//     → CLH 队列：park/unpark 线程，无需 Object.wait
//   java.util.concurrent.atomic.AtomicInteger
//     → compareAndSet() → Unsafe.compareAndSetInt() (硬件 CAS 指令)
//   java.util.concurrent.CompletableFuture
//     → UniApply / BiApply — 链式完成节点
// ══════════════════════════════════════════════════════════════════

object ConcurrencyBasics : Topic {
    override val name = "concurrency"
    override val description = "Threads, locks, atomics, executors"
    override val levels = listOf(
        Level(1, "threads", "THREADS & BASIC SYNCHRONIZATION - 线程与基础同步", ThreadsAndSync::run),
        Level(2, "locks", "LOCKS & CONDITIONS - ReentrantLock, ReadWriteLock, Condition", LocksAndConditions::run),
        Level(3, "atomic", "ATOMIC OPERATIONS & JAVA MEMORY MODEL - 原子操作与内存模型", AtomicAndMemoryModel::run),
        Level(4, "executors", "EXECUTORS & FUTURES - 线程池、协调工具、异步流水线", ExecutorsAndFutures::run),
    )
}
