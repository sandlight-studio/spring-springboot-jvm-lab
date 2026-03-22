package studio.sandlight.lang.collections

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

object CollectionsBasics {

    fun run() {
        println("=".repeat(65))
        println("Kotlin Collections — 4 levels")
        println("=".repeat(65))

        Foundation.run()
        Functional.run()
        Sequences.run()
        Interop.run()
    }
}
