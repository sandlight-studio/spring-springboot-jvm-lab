package studio.sandlight.lang.kotlin

import studio.sandlight.lang.support.Level
import studio.sandlight.lang.support.Topic

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

object KotlinBasics : Topic {
    override val name = "kotlin"
    override val description = "Null safety, sealed, generics, coroutines, DSL"
    override val levels = listOf(
        Level(1, "fundamentals", "FUNDAMENTALS - Null Safety, Data Class, Extensions, When", KotlinFundamentals::run),
        Level(2, "functional", "FUNCTIONAL & TYPES - Lambdas, Sealed Classes, Scope Functions", KotlinFunctional::run),
        Level(3, "typesystem", "TYPE SYSTEM - Generics, Variance, Delegates, Operators", KotlinTypeSystem::run),
        Level(4, "coroutines", "COROUTINES - 协程 / 结构化并发 / Flow", KotlinCoroutines::run),
        Level(5, "dsl", "DSL & METAPROGRAMMING - Lambda with Receiver, Reified, Value Class", KotlinDsl::run),
    )
}
