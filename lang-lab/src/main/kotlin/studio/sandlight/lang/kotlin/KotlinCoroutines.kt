package studio.sandlight.lang.kotlin

// LEVEL 4: Coroutines — suspend / launch / async / Flow / structured concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis

object KotlinCoroutines {

    fun run() {
        println("\n\n  LEVEL 4: KOTLIN COROUTINES - 协程 / 结构化并发 / Flow")
        println("=".repeat(60))

        demo41SuspendFunctions()
        demo42CoroutineBuilders()
        demo43StructuredConcurrency()
        demo44Flow()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.1 suspend Functions — 挂起函数与状态机
    //
    // A suspend function compiles to a state machine via CPS transform.
    // The compiler inserts a Continuation parameter and a label switch:
    //
    //   suspend fun fetchUser(): User {
    //     delay(100)      ← State 0: schedule resume after 100ms, return
    //     return api()    ← State 1: resumed here, call api, return
    //   }
    //
    //   JVM equivalent (simplified):
    //     Object fetchUser(Continuation cont) {
    //       switch (cont.label) {
    //         case 0: cont.label = 1; return COROUTINE_SUSPENDED;
    //         case 1: return cont.result;
    //       }
    //     }
    //
    // Key point: suspension releases the thread — no thread is blocked
    // while a coroutine waits for delay() or I/O.
    //
    // Sequential vs Concurrent:
    //
    //   Sequential  (2 × 100ms ≈ 200ms):
    //     val a = fetchSimulated("A", 100)   ← suspends, resumes
    //     val b = fetchSimulated("B", 100)   ← then suspends, resumes
    //
    //   Concurrent  (both at once ≈ 100ms):
    //     val a = async { fetchSimulated("A", 100) }
    //     val b = async { fetchSimulated("B", 100) }
    //     a.await() + b.await()              ← both run in parallel
    //
    // 阅读: kotlinx.coroutines.internal.BaseContinuationImpl.resumeWith()
    // ──────────────────────────────────────────────────────────────
    private fun demo41SuspendFunctions() {
        println("\n--- 4.1 suspend Functions ---")

        val seqMs = measureTimeMillis {
            runBlocking {
                val a = fetchSimulated("A", 100)
                val b = fetchSimulated("B", 100)
                println("  Sequential results: $a | $b")
            }
        }

        val conMs = measureTimeMillis {
            runBlocking {
                val a = async { fetchSimulated("A", 100) }
                val b = async { fetchSimulated("B", 100) }
                println("  Concurrent results: ${a.await()} | ${b.await()}")
            }
        }

        println("  Sequential elapsed : ~${seqMs}ms  (two 100ms delays in series)")
        println("  Concurrent elapsed : ~${conMs}ms  (two 100ms delays in parallel)")
        println("  Speed-up           : ~${seqMs - conMs}ms saved by async/await")
    }

    /** Simulates an async fetch: suspends for [delayMs] then returns a value. */
    private suspend fun fetchSimulated(name: String, delayMs: Long): String {
        delay(delayMs)
        return "data-$name"
    }

    // ──────────────────────────────────────────────────────────────
    // 4.2 Coroutine Builders — 四种启动方式
    //
    //   Coroutine Builders:
    //
    //   launch { }          → Job (no result, fire-and-forget)
    //   async { }           → Deferred<T> (has result, use .await())
    //   coroutineScope { }  → suspends until all children finish; throws if any fails
    //   runBlocking { }     → blocks current thread; bridges blocking → suspending
    //   withContext(D) { }  → switches dispatcher for a block
    //
    // Dispatchers:
    //   Dispatchers.Default  → CPU-intensive work (thread count = CPU cores)
    //   Dispatchers.IO       → blocking I/O (elastic pool, up to 64 threads)
    //   Dispatchers.Main     → UI thread (Android/JavaFX — not available on JVM CLI)
    //   Dispatchers.Unconfined → starts in caller thread, resumes wherever
    //
    // 阅读: kotlinx.coroutines.CoroutineScope
    //       kotlinx.coroutines.Builders.kt (launch, async, runBlocking)
    // ──────────────────────────────────────────────────────────────
    private fun demo42CoroutineBuilders() {
        println("\n--- 4.2 Coroutine Builders ---")

        // launch — Job, fire-and-forget
        runBlocking {
            val job: Job = launch {
                delay(50)
                println("  [launch] fire-and-forget job completed")
            }
            println("  [launch] job.isActive=${job.isActive}  (job is running)")
            job.join()   // wait so output appears before next demo
            println("  [launch] job.isCompleted=${job.isCompleted}")
        }

        // async / await — Deferred<T>, carries a result
        runBlocking {
            val deferred: Deferred<Int> = async {
                delay(50)
                42
            }
            println("  [async]  deferred.isCompleted=${deferred.isCompleted}  (not yet)")
            val result = deferred.await()
            println("  [async]  result=$result, isCompleted=${deferred.isCompleted}")
        }

        // coroutineScope — structured scope; all children must finish
        runBlocking {
            coroutineScope {
                launch { delay(30); println("  [coroutineScope] child-1 done") }
                launch { delay(50); println("  [coroutineScope] child-2 done") }
            }
            println("  [coroutineScope] parent resumes only after both children finish")
        }

        // withContext — switch dispatcher without leaving the coroutine
        runBlocking {
            val thread1 = Thread.currentThread().name
            val thread2 = withContext(Dispatchers.Default) { Thread.currentThread().name }
            println("  [withContext] before=${thread1}  inside Default=${thread2}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4.3 Structured Concurrency — 结构化并发
    //
    //   Structured Concurrency guarantees:
    //
    //   1. A parent scope does not complete until all children finish
    //   2. If a child fails, the parent (and siblings) are cancelled
    //   3. Cancellation propagates down the scope tree
    //   4. No coroutine can "leak" outside its scope
    //
    //   SupervisorJob / supervisorScope: child failures are independent
    //
    // Scope tree example:
    //
    //   coroutineScope {                  ← parent scope
    //     launch { child-A }             ← child, participates in scope
    //     launch { child-B }             ← child, cancelled if parent cancelled
    //   }                                ← waits for BOTH before returning
    //
    // SupervisorJob scope tree:
    //
    //   supervisorScope {
    //     launch { child-A throws }      ← fails; siblings NOT cancelled
    //     launch { child-B continues }   ← still runs to completion
    //   }
    //
    // 阅读: kotlinx.coroutines.Job
    //       kotlinx.coroutines.SupervisorJob
    //       kotlinx.coroutines.CoroutineScope.cancel()
    // ──────────────────────────────────────────────────────────────
    private fun demo43StructuredConcurrency() {
        println("\n--- 4.3 Structured Concurrency ---")

        // Parent-child: parent waits for all children
        runBlocking {
            val results = mutableListOf<String>()
            coroutineScope {
                launch { delay(30); results += "child-1" }
                launch { delay(10); results += "child-2" }
                launch { delay(20); results += "child-3" }
            }
            println("  Parent-child completion order: $results")
            println("  (parent resumed only after all 3 children finished)")
        }

        // Cancellation propagates: cancel parent → all children cancelled
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default + Job())
            val child1Cancelled = CompletableDeferred<Boolean>()
            val child2Cancelled = CompletableDeferred<Boolean>()

            scope.launch {
                launch {
                    try { delay(5_000) }
                    catch (e: CancellationException) { child1Cancelled.complete(true) }
                }
                launch {
                    try { delay(5_000) }
                    catch (e: CancellationException) { child2Cancelled.complete(true) }
                }
                delay(30)       // let children start
                scope.cancel()  // cancel the whole scope
            }

            println("  child-1 cancelled=${child1Cancelled.await()}")
            println("  child-2 cancelled=${child2Cancelled.await()}")
            println("  Cancellation propagated from parent down to both children")
        }

        // SupervisorJob: one child failure does NOT cancel siblings
        runBlocking {
            val sibling2Ran = CompletableDeferred<Boolean>()
            val handler = CoroutineExceptionHandler { _, e ->
                println("  [handler] child-1 threw: ${e.message}")
            }

            supervisorScope {
                launch(handler) {
                    delay(10)
                    throw RuntimeException("child-1 failed on purpose")
                }
                launch {
                    delay(60)
                    sibling2Ran.complete(true)
                }
            }

            println("  SupervisorJob: sibling-2 completed=${sibling2Ran.await()}")
            println("  (child-1 failure did NOT cancel child-2)")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4.4 Flow — Cold Reactive Stream (冷流)
    //
    //   Flow vs Sequence vs List:
    //
    //   List<T>       → eager, all in memory, synchronous
    //   Sequence<T>   → lazy, synchronous (blocking)
    //   Flow<T>       → lazy, asynchronous (suspend-based)
    //
    // Flow operators mirror Sequence operators but are suspend-aware:
    //   flow.map { }     ← suspend lambda ok
    //   flow.filter { }
    //   flow.take(n)     ← cancels upstream after n elements
    //   flow.collect { } ← terminal, suspends until complete
    //   flow.flowOn(D)   ← switches context for upstream only
    //
    // Cold stream: no work happens until collect {} is called.
    // Each new collect {} re-runs the producer from scratch.
    //
    // flowOn(Dispatcher) affects only the upstream operators —
    // collect{} always runs in the caller's context.
    //
    //   flow { emit(1); emit(2) }     ← upstream: runs on Default
    //     .map { it * 2 }             ← upstream: still on Default
    //     .flowOn(Dispatchers.Default)
    //     .collect { }                ← downstream: runs on caller
    //
    // 阅读: kotlinx.coroutines.flow.FlowKt
    //       kotlinx.coroutines.flow.SafeFlow (flow { } builder impl)
    //       kotlinx.coroutines.flow.operators (map, filter, flowOn, …)
    // ──────────────────────────────────────────────────────────────
    private fun demo44Flow() {
        println("\n--- 4.4 Flow — Cold Reactive Stream ---")

        // Basic flow: emit 1..5, filter odds, square, collect
        println("  Flow pipeline (odd squares from 1..5):")
        runBlocking {
            val collected = mutableListOf<Int>()
            (1..5).asFlow()
                .onEach { delay(50) }           // simulate async delay per element
                .filter { it % 2 != 0 }         // keep 1, 3, 5
                .map { it * it }                 // square: 1, 9, 25
                .collect { value ->
                    collected += value
                    println("    collected: $value")
                }
            println("  All collected: $collected")
        }

        // take(n): cancels upstream after n elements (no resource leak)
        println("  Flow with take(2) — stops after 2 elements:")
        runBlocking {
            flow {
                for (i in 1..100) {
                    println("    emitting $i")
                    emit(i)
                }
            }.take(2)
             .collect { println("    collected $it") }
            println("  (producer stopped at 2 — no wasted work)")
        }

        // flowOn: run upstream on Dispatchers.Default
        println("  flowOn(Dispatchers.Default) — upstream context switch:")
        runBlocking {
            val callerThread = Thread.currentThread().name
            var upstreamThread = ""
            flow {
                upstreamThread = Thread.currentThread().name
                emit(1)
            }.flowOn(Dispatchers.Default)
             .collect { }
            println("  caller  thread: $callerThread")
            println("  upstream thread: $upstreamThread  (different — flowOn switched context)")
        }

        // Cold stream: each collect re-runs the producer
        println("  Cold stream — each collect is independent:")
        runBlocking {
            val counter = flow {
                println("    producer started")
                emit(1); emit(2)
            }
            counter.collect { }   // first collect
            counter.collect { }   // second collect — producer runs again
            println("  (producer ran twice — flow is cold)")
        }
    }
}
