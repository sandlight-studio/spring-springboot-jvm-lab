package studio.sandlight.lang.concurrency

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.StampedLock
import java.util.LinkedList

// ══════════════════════════════════════════════════════════════════
// Locks & Conditions — java.util.concurrent.locks
//
// 本文件覆盖 Level 2 锁工具：
//   2.1 ReentrantLock vs synchronized
//   2.2 ReadWriteLock  — 读写分离
//   2.3 Condition      — 精确的等待/通知
//   2.4 Deadlock       — 死锁检测与预防
//   2.5 StampedLock    — 乐观读锁
// ══════════════════════════════════════════════════════════════════

object LocksAndConditions {

    fun run() {
        println("=== 2.1 ReentrantLock vs synchronized ===")
        demo21ReentrantLock()

        println("\n=== 2.2 ReadWriteLock ===")
        demo22ReadWriteLock()

        println("\n=== 2.3 Condition (Producer/Consumer) ===")
        demo23Condition()

        println("\n=== 2.4 Deadlock Detection & Prevention ===")
        demo24Deadlock()

        println("\n=== 2.5 StampedLock (Optimistic Read) ===")
        demo25StampedLock()
    }

    // ──────────────────────────────────────────────────────────────
    // 2.1 ReentrantLock vs synchronized
    //
    // ReentrantLock vs synchronized 对比：
    //
    //   synchronized:
    //     ✓ 简单，JVM 管理
    //     ✓ 自动释放（异常时也释放）
    //     ✗ 无法尝试获取（tryLock）
    //     ✗ 无法响应中断
    //     ✗ 无法有限等待
    //
    //   ReentrantLock:
    //     ✓ tryLock(timeout) — 超时获取
    //     ✓ lockInterruptibly() — 可中断
    //     ✓ 公平锁选项 ReentrantLock(fair=true)
    //     ✓ 可关联多个 Condition
    //     ✗ 必须手动 unlock()（用 try/finally）
    //
    // 可重入：同一线程可多次 lock()，必须 unlock() 相同次数
    // 阅读：java.util.concurrent.locks.ReentrantLock → Sync.nonfairTryAcquire()
    // ──────────────────────────────────────────────────────────────
    private fun demo21ReentrantLock() {
        val lock = ReentrantLock()

        // ── Demo A: tryLock with timeout ──────────────────────────
        // holder 线程持有锁 300ms；tryThread 尝试 100ms 内获取 → 失败
        // 注意：ReentrantLock.unlock() 必须由持锁线程调用
        val holder = Thread {
            lock.lock()   // holder 自己获取锁
            try {
                Thread.sleep(300)
            } finally {
                lock.unlock()
                println("  [holder] released lock")
            }
        }
        holder.start()
        Thread.sleep(20)  // give holder time to acquire the lock

        val tryThread = Thread {
            val acquired = lock.tryLock(100, TimeUnit.MILLISECONDS)
            if (acquired) {
                try {
                    println("  [tryThread] acquired lock (unexpected)")
                } finally {
                    lock.unlock()
                }
            } else {
                println("  [tryThread] tryLock timed out — lock held by another thread ✓")
            }
        }
        tryThread.start()
        tryThread.join()
        holder.join()

        // ── Demo B: Reentrancy ────────────────────────────────────
        // 同一线程连续 lock() 两次，必须 unlock() 两次
        val reentrantLock = ReentrantLock()
        reentrantLock.lock()
        try {
            println("  [main] hold count after 1st lock: ${reentrantLock.holdCount}")
            reentrantLock.lock()   // 可重入，不会死锁
            try {
                println("  [main] hold count after 2nd lock: ${reentrantLock.holdCount}")
            } finally {
                reentrantLock.unlock()  // 释放第 2 次
                println("  [main] hold count after 1st unlock: ${reentrantLock.holdCount}")
            }
        } finally {
            reentrantLock.unlock()  // 释放第 1 次
            println("  [main] hold count after 2nd unlock: ${reentrantLock.holdCount}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2.2 ReadWriteLock — 读写分离
    //
    // ReentrantReadWriteLock — 读写锁
    //
    //   规则：
    //   ┌────────────────┬──────────────────────────────┐
    //   │                │ 当前已有读锁   当前已有写锁   │
    //   ├────────────────┼──────────────────────────────┤
    //   │ 新读锁请求      │  ✓ 共存       ✗ 等待        │
    //   │ 新写锁请求      │  ✗ 等待       ✗ 等待        │
    //   └────────────────┴──────────────────────────────┘
    //
    //   适合：读多写少的共享数据（缓存、配置）
    //   阅读：ReentrantReadWriteLock → Sync.tryAcquireShared() / tryAcquire()
    //         读写计数压缩到同一个 int：高16位=读计数，低16位=写计数
    // ──────────────────────────────────────────────────────────────
    private fun demo22ReadWriteLock() {
        val rwLock = ReentrantReadWriteLock()
        var sharedData = "initial"
        val startTime = System.currentTimeMillis()

        fun elapsed() = System.currentTimeMillis() - startTime

        // 3 个读线程并发读取（读锁共存）
        val readers = (1..3).map { id ->
            Thread {
                rwLock.readLock().lock()
                try {
                    println("  [reader-$id] +${elapsed()}ms acquired read lock, data='$sharedData'")
                    Thread.sleep(150)   // 模拟读取耗时
                    println("  [reader-$id] +${elapsed()}ms releasing read lock")
                } finally {
                    rwLock.readLock().unlock()
                }
            }
        }

        // 1 个写线程，需等所有读锁释放
        val writer = Thread {
            Thread.sleep(50)   // 让读线程先启动
            println("  [writer]   +${elapsed()}ms requesting write lock (will wait for readers)...")
            rwLock.writeLock().lock()
            try {
                println("  [writer]   +${elapsed()}ms acquired write lock, updating data")
                sharedData = "updated"
                Thread.sleep(50)
            } finally {
                rwLock.writeLock().unlock()
                println("  [writer]   +${elapsed()}ms released write lock, data='$sharedData'")
            }
        }

        readers.forEach { it.start() }
        writer.start()
        readers.forEach { it.join() }
        writer.join()

        // Readers start at ~same time → their log lines overlap in time,
        // proving concurrent read access. Writer waits until all reads finish.
        println("  [info] readers overlapped; writer waited for all read locks ✓")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.3 Condition — 精确的等待/通知（Producer/Consumer）
    //
    // Condition = synchronized 时代 Object.wait/notify 的现代替代
    //
    //   Object.wait()    ≈  condition.await()
    //   Object.notify()  ≈  condition.signal()
    //
    // 优势：一个 Lock 可以关联多个 Condition
    //   例：BlockingQueue 有 notFull 和 notEmpty 两个条件
    //       生产者等 notFull，消费者等 notEmpty（避免不必要的唤醒）
    //
    // 阅读：java.util.concurrent.ArrayBlockingQueue
    //   → put() / take() 使用两个 Condition
    // ──────────────────────────────────────────────────────────────
    private fun demo23Condition() {
        val lock = ReentrantLock()
        val notFull = lock.newCondition()
        val notEmpty = lock.newCondition()
        val buffer = LinkedList<Int>()
        val capacity = 3

        val producer = Thread {
            for (item in 1..5) {
                lock.lock()
                try {
                    while (buffer.size == capacity) {
                        println("  [producer] buffer full ($capacity), awaiting notFull...")
                        notFull.await()
                    }
                    buffer.add(item)
                    println("  [producer] produced $item, buffer=${buffer.toList()}")
                    notEmpty.signal()
                } finally {
                    lock.unlock()
                }
                Thread.sleep(60)
            }
        }

        val consumer = Thread {
            for (i in 1..5) {
                lock.lock()
                try {
                    while (buffer.isEmpty()) {
                        println("  [consumer] buffer empty, awaiting notEmpty...")
                        notEmpty.await()
                    }
                    val item = buffer.poll()
                    println("  [consumer] consumed $item, buffer=${buffer.toList()}")
                    notFull.signal()
                } finally {
                    lock.unlock()
                }
                Thread.sleep(100)  // 消费者比生产者慢，会触发 notFull 等待
            }
        }

        producer.start()
        consumer.start()
        producer.join()
        consumer.join()
        println("  [info] all 5 items produced and consumed in order ✓")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.4 Deadlock — 检测与预防
    //
    // 死锁（Deadlock）的四个必要条件（Coffman 条件）：
    //   1. 互斥：资源同一时间只能被一个线程持有
    //   2. 占有并等待：线程持有资源的同时等待其他资源
    //   3. 非抢占：资源只能由持有者主动释放
    //   4. 循环等待：线程 A 等 B 的资源，B 等 A 的资源
    //
    // 预防死锁：
    //   ✓ 固定加锁顺序（按对象 id 或名称排序）
    //   ✓ tryLock 超时获取，获取失败则回退释放已持有的锁
    //   ✓ 使用单一全局锁（牺牲并发度）
    //
    // 注意：本示例使用 tryLock 超时，不会真正死锁挂起程序
    // ──────────────────────────────────────────────────────────────
    private fun demo24Deadlock() {
        val lockA = ReentrantLock()
        val lockB = ReentrantLock()

        // ── Part 1: 演示死锁检测（tryLock 回退）────────────────────
        // Thread A: lockA → lockB
        // Thread B: lockB → lockA  ← 循环等待，但 tryLock 超时退出
        val threadA = Thread {
            val gotA = lockA.tryLock(500, TimeUnit.MILLISECONDS)
            if (!gotA) { println("  [threadA] could not acquire lockA"); return@Thread }
            try {
                println("  [threadA] acquired lockA, trying lockB...")
                Thread.sleep(80)   // 让 threadB 有机会先拿到 lockB
                val gotB = lockB.tryLock(500, TimeUnit.MILLISECONDS)
                if (gotB) {
                    try {
                        println("  [threadA] acquired lockB — no deadlock (lucky ordering)")
                    } finally {
                        lockB.unlock()
                    }
                } else {
                    println("  [threadA] tryLock(lockB) timed out — backing off to avoid deadlock ✓")
                }
            } finally {
                lockA.unlock()
            }
        }

        val threadB = Thread {
            val gotB = lockB.tryLock(500, TimeUnit.MILLISECONDS)
            if (!gotB) { println("  [threadB] could not acquire lockB"); return@Thread }
            try {
                println("  [threadB] acquired lockB, trying lockA...")
                Thread.sleep(80)   // 让 threadA 有机会先拿到 lockA
                val gotA = lockA.tryLock(500, TimeUnit.MILLISECONDS)
                if (gotA) {
                    try {
                        println("  [threadB] acquired lockA — no deadlock (lucky ordering)")
                    } finally {
                        lockA.unlock()
                    }
                } else {
                    println("  [threadB] tryLock(lockA) timed out — backing off to avoid deadlock ✓")
                }
            } finally {
                lockB.unlock()
            }
        }

        threadA.start()
        threadB.start()
        threadA.join()
        threadB.join()

        // ── Part 2: 预防 — 固定加锁顺序（lockA 始终先于 lockB）──────
        println("  [fix] enforcing lock ordering: always lockA before lockB")
        val fixA = Thread {
            lockA.lock()
            try {
                lockB.lock()
                try {
                    println("  [fixA] acquired lockA then lockB in order ✓")
                    Thread.sleep(30)
                } finally {
                    lockB.unlock()
                }
            } finally {
                lockA.unlock()
            }
        }

        val fixB = Thread {
            // 与 fixA 相同顺序：lockA → lockB（不再循环等待）
            lockA.lock()
            try {
                lockB.lock()
                try {
                    println("  [fixB] acquired lockA then lockB in order ✓")
                    Thread.sleep(30)
                } finally {
                    lockB.unlock()
                }
            } finally {
                lockA.unlock()
            }
        }

        fixA.start()
        fixB.start()
        fixA.join()
        fixB.join()
        println("  [info] consistent lock ordering prevents deadlock ✓")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.5 StampedLock — 乐观读锁（Java 8+）
    //
    // StampedLock (Java 8+) — 乐观读锁
    //
    //   比 ReadWriteLock 更进一步：
    //   tryOptimisticRead() 不加任何锁，直接读
    //   validate(stamp)     验证读取期间是否有写操作
    //   → 如果验证失败，升级到悲观读锁重试
    //
    //   适合：读取频繁、写入极少且读取时间短的场景
    //
    //   注意：StampedLock 不可重入！不要在已持有锁的方法中再次获取
    //
    // 阅读：java.util.concurrent.locks.StampedLock → tryOptimisticRead() → validate()
    // ──────────────────────────────────────────────────────────────
    private fun demo25StampedLock() {
        // Point 类：持有 x, y 坐标，用 StampedLock 保护并发访问
        class Point {
            val sl = StampedLock()
            var x: Double = 0.0
            var y: Double = 0.0

            // 乐观读：不加锁直接读，再验证；失败则升级悲观读锁
            fun distanceFromOrigin(): Double {
                var stamp = sl.tryOptimisticRead()        // 不加锁，获取乐观戳
                var cx = x
                var cy = y
                if (!sl.validate(stamp)) {               // 验证期间是否有写
                    // 乐观读失败 → 升级为悲观读锁
                    stamp = sl.readLock()
                    try {
                        cx = x
                        cy = y
                        println("  [reader] optimistic read failed, fell back to read lock")
                    } finally {
                        sl.unlockRead(stamp)
                    }
                } else {
                    println("  [reader] optimistic read succeeded (no write interference)")
                }
                return Math.sqrt(cx * cx + cy * cy)
            }

            // 写锁：更新两个坐标（必须原子性）
            fun move(newX: Double, newY: Double) {
                val stamp = sl.writeLock()
                try {
                    x = newX
                    y = newY
                } finally {
                    sl.unlockWrite(stamp)
                }
            }
        }

        val point = Point()
        point.move(3.0, 4.0)   // 初始化

        // 读线程：乐观读距离
        val reader = Thread {
            repeat(3) { i ->
                val dist = point.distanceFromOrigin()
                println("  [reader] iteration $i — distance = $dist")
                Thread.sleep(50)
            }
        }

        // 写线程：并发更新坐标（触发部分乐观读验证失败）
        val writer = Thread {
            Thread.sleep(30)   // 让 reader 先启动进入乐观读
            point.move(6.0, 8.0)
            println("  [writer] moved point to (6, 8), distance should be 10.0")
        }

        reader.start()
        writer.start()
        reader.join()
        writer.join()
        println("  [info] StampedLock: optimistic read with fallback to pessimistic read ✓")
    }
}
