package studio.sandlight.lang.kotlin

// ══════════════════════════════════════════════════════════════════
// Kotlin Language Features — Five Levels
//
//   ┌─────────────────────────────────────────────────────────────┐
//   │  LEVEL 5: DSL & 元编程                                      │
//   │    Lambda with Receiver / reified / Value Class / 反射      │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 4: 协程 (Coroutines)                                 │
//   │    suspend / launch / async / Flow / structured concurrency │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 3: 泛型 & 委托                                       │
//   │    in/out variance / by lazy / by observable / by map       │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 2: 函数式 & 类型系统                                 │
//   │    lambda / 高阶函数 / sealed class / scope functions       │
//   ├─────────────────────────────────────────────────────────────┤
//   │  LEVEL 1: 基础特性                                          │
//   │    null safety / data class / extension fn / object / when  │
//   └─────────────────────────────────────────────────────────────┘
//
// 核心 stdlib/JVM 入口：
//   kotlin.jvm.internal.Intrinsics      → null check injection at call sites
//   kotlin.sequences.SequencesKt        → lazy sequence operators
//   kotlin.properties.Delegates         → observable, vetoable, notNull
//   kotlinx.coroutines.internal.BaseContinuationImpl → coroutine state machine
//   kotlinx.coroutines.flow.FlowKt      → flow builder and operators
// ══════════════════════════════════════════════════════════════════

object KotlinBasics {

    fun run() {
        KotlinFundamentals.run()
        KotlinFunctional.run()
        KotlinTypeSystem.run()
        KotlinCoroutines.run()
        KotlinDsl.run()
    }
}
