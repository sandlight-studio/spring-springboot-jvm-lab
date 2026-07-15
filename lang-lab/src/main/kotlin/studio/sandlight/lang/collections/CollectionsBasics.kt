package studio.sandlight.lang.collections

import studio.sandlight.lang.support.Level
import studio.sandlight.lang.support.Topic

// ══════════════════════════════════════════════════════════════════
// Kotlin Collection Type Hierarchy:
//
//   kotlin.collections.Iterable<T>
//         │
//   kotlin.collections.Collection<T>          (read-only interface)
//         ├── List<T>     ← ordered, index access, duplicates allowed
//         ├── Set<T>      ← unique elements, no index
//         └── Map<K,V>    ← key-value pairs (not a Collection)
//
//   kotlin.collections.MutableCollection<T>   (read-write interface)
//         ├── MutableList<T>   → add, remove, set, clear
//         ├── MutableSet<T>    → add, remove, clear
//         └── MutableMap<K,V>  → put, remove, clear
//
// JVM reality: Kotlin "read-only" types are compile-time guardrails only.
// The actual objects are plain Java collections:
//   listOf(...)       → java.util.Arrays$ArrayList  (or emptyList for 0)
//   mutableListOf()   → java.util.ArrayList
//   setOf(...)        → java.util.LinkedHashSet      (insertion order preserved)
//   mapOf(...)        → java.util.LinkedHashMap
//
// Nothing stops a cast to MutableList at runtime — the type system just won't
// let you write it directly, enforcing a "pit of success" design.
//
// Kotlin stdlib source (readable on GitHub or bundled sources):
//   kotlin/collections/_Collections.kt  → listOf(), mutableListOf()
//   kotlin/collections/_Sets.kt         → setOf(), mutableSetOf()
//   kotlin/collections/_Maps.kt         → mapOf(), mutableMapOf()
// ══════════════════════════════════════════════════════════════════

object CollectionsBasics : Topic {
    override val name = "collections"
    override val description = "Lists, sets, maps, sequences"
    override val levels = listOf(
        Level(1, "foundation", "FOUNDATION - Collection Types & Basics", Foundation::run),
        Level(2, "functional", "FUNCTIONAL - Transformations, Aggregation, Grouping", Functional::run),
        Level(3, "sequences", "SEQUENCES - Lazy Evaluation & Pipeline Optimization", Sequences::run),
        Level(4, "interop", "INTEROP & ADVANCED - Java Collections, Concurrency, Performance", Interop::run),
    )
}
