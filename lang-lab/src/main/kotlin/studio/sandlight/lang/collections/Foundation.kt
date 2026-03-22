package studio.sandlight.lang.collections

// LEVEL 1: Foundation — 集合类型、不变性的本质、基本操作

object Foundation {

    fun run() {
        println("\n🏗️  LEVEL 1: FOUNDATION - Collection Types & Basics")
        println("=".repeat(60))

        demo11CollectionTypes()
        demo12ImmutabilityIllusion()
        demo13BasicOperations()
        demo14Destructuring()
    }

    // ──────────────────────────────────────────────────────────────
    // 1.1 集合类型
    //
    // 创建工厂函数与 JVM 实际类型的对应：
    //
    //   listOf(1,2,3)      → Arrays$ArrayList (fixed-size wrapper, NOT ArrayList)
    //                         实现：java.util.Arrays.asList(...)
    //
    //   emptyList<T>()     → Collections.EMPTY_LIST (单例)
    //
    //   mutableListOf()    → java.util.ArrayList   (可扩容)
    //
    //   setOf(1,2,3)       → java.util.LinkedHashSet  (保持插入顺序)
    //   hashSetOf(1,2,3)   → java.util.HashSet        (无序，O(1) 查找)
    //   sortedSetOf(1,2,3) → java.util.TreeSet        (有序，O(log n) 查找)
    //
    //   mapOf("a" to 1)    → java.util.LinkedHashMap  (保持插入顺序)
    //   hashMapOf(...)     → java.util.HashMap        (无序，O(1) 平均)
    //   sortedMapOf(...)   → java.util.TreeMap        (键排序，O(log n))
    //
    // 阅读：kotlin/collections/_Collections.kt  → listOf()
    //       java.base/java/util/Arrays.java      → asList()
    // ──────────────────────────────────────────────────────────────
    private fun demo11CollectionTypes() {
        println("\n--- 1.1 Collection Types & JVM Backing Classes ---")

        // List
        val immList   = listOf(1, 2, 3)
        val mutList   = mutableListOf(1, 2, 3)
        val emptyList = emptyList<Int>()
        println("listOf       → ${immList.javaClass.name}")
        println("mutableListOf→ ${mutList.javaClass.name}")
        println("emptyList    → ${emptyList.javaClass.name}")

        // Set
        val orderedSet = setOf(3, 1, 2, 1)        // 1 duplicate removed, order kept
        val hashSet    = hashSetOf(3, 1, 2)        // no order guarantee
        val sortedSet  = sortedSetOf(3, 1, 2)      // always sorted
        println("\nsetOf(3,1,2,1) → ${orderedSet.javaClass.simpleName}: $orderedSet")
        println("hashSetOf      → ${hashSet.javaClass.simpleName}: $hashSet")
        println("sortedSetOf    → ${sortedSet.javaClass.simpleName}: $sortedSet")

        // Map
        val orderedMap = mapOf("b" to 2, "a" to 1, "c" to 3)  // insertion order
        val hashMap    = hashMapOf("b" to 2, "a" to 1)
        val sortedMap  = sortedMapOf("b" to 2, "a" to 1, "c" to 3)
        println("\nmapOf     → ${orderedMap.javaClass.simpleName}: ${orderedMap.keys}")
        println("hashMapOf → ${hashMap.javaClass.simpleName}:    ${hashMap.keys}")
        println("sortedMapOf→ ${sortedMap.javaClass.simpleName}:     ${sortedMap.keys}")

        // ArrayDeque — Kotlin's own double-ended queue (wraps an Array, not LinkedList)
        val deque = ArrayDeque(listOf(1, 2, 3))
        deque.addFirst(0)
        deque.addLast(4)
        println("\nArrayDeque after addFirst(0)/addLast(4): $deque")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.2 "不变性"的本质
    //
    // Kotlin 的只读集合接口是「编译时」保证，不是「运行时」保证。
    //
    //   val list: List<Int> = listOf(1, 2, 3)
    //
    //   ┌─────────────────────────────────────────────────────────┐
    //   │ Kotlin 视角      │  list: List<Int>  (无 add/remove)   │
    //   │                  │                                      │
    //   │ JVM 视角         │  实际对象: Arrays$ArrayList          │
    //   │                  │  → 它实现了 java.util.List           │
    //   │                  │  → 有 add()，但调用会抛              │
    //   │                  │    UnsupportedOperationException     │
    //   └─────────────────────────────────────────────────────────┘
    //
    // 与 java.util.Collections.unmodifiableList() 的区别：
    //   - unmodifiableList = 包装器，运行时阻止修改（抛异常）
    //   - Kotlin listOf   = 编译时阻止，若强制转型可能在运行时成功或失败
    //
    // 真正不可变的集合需要用第三方库，如 kotlinx.collections.immutable
    // ──────────────────────────────────────────────────────────────
    private fun demo12ImmutabilityIllusion() {
        println("\n--- 1.2 Immutability — Compile-time vs Runtime ---")

        val readOnly = listOf("a", "b", "c")
        println("Read-only list: $readOnly  (type: ${readOnly.javaClass.name})")

        // 编译器阻止：下面这行无法编译
        // readOnly.add("d")  ← compile error: unresolved reference 'add'

        // 但强制转型后 — 运行时行为取决于底层实现
        // Arrays$ArrayList.add() 会抛 UnsupportedOperationException
        try {
            @Suppress("UNCHECKED_CAST")
            (readOnly as java.util.List<String>).add("d")
            println("Mutation succeeded (unexpected!)")
        } catch (e: UnsupportedOperationException) {
            println("UnsupportedOperationException: Arrays\$ArrayList does not support add()")
        }

        // mutableListOf → real ArrayList → can be mutated even after casting to List
        val mutable: List<String> = mutableListOf("x", "y")
        @Suppress("UNCHECKED_CAST")
        (mutable as java.util.List<String>).add("z")
        println("After forceful add to mutableListOf-backed List: $mutable")

        // 真正的不可变：包装为 unmodifiable
        val javaUnmodifiable = java.util.Collections.unmodifiableList(mutableListOf(1, 2, 3))
        try {
            javaUnmodifiable.add(4)
        } catch (e: UnsupportedOperationException) {
            println("Collections.unmodifiableList also throws: UnsupportedOperationException")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 1.3 基本操作
    //
    // List:  get(i) / [i] → O(1) for ArrayList
    //        subList(from, to) → view, not a copy; backed by original
    //
    // Set:   contains() → O(1) HashSet, O(log n) TreeSet
    //
    // Map:   getOrDefault / getValue / getOrElse / getOrPut
    //        entries / keys / values — all are live views
    // ──────────────────────────────────────────────────────────────
    private fun demo13BasicOperations() {
        println("\n--- 1.3 Basic Operations ---")

        val items = listOf(
            Item("Apple",  1.5,  "fruit"),
            Item("Banana", 0.8,  "fruit"),
            Item("Carrot", 0.5,  "vegetable"),
            Item("Durian", 12.0, "fruit")
        )

        println("size: ${items.size}")
        println("isEmpty: ${items.isEmpty()}")
        println("first: ${items.first()}")
        println("last: ${items.last()}")
        println("firstOrNull { price>10 }: ${items.firstOrNull { it.price > 10 }}")
        println("indexOf Banana: ${items.indexOf(items[1])}")

        // subList — backed by original, O(1), no copy
        val sub = items.subList(1, 3)
        println("subList(1,3): $sub  (type: ${sub.javaClass.simpleName})")

        // Map operations
        val priceMap = mapOf("Apple" to 1.5, "Banana" to 0.8)
        println("\ngetOrDefault(\"Mango\", -1.0): ${priceMap.getOrDefault("Mango", -1.0)}")
        println("getOrElse(\"Mango\"){ 0.0 }:   ${priceMap.getOrElse("Mango") { 0.0 }}")

        val mutableMap = mutableMapOf("Apple" to 1.5)
        mutableMap.getOrPut("Banana") { 0.8 }  // inserts if absent
        println("After getOrPut(Banana): $mutableMap")

        // Set membership check
        val fruitSet = setOf("Apple", "Banana", "Cherry")
        println("\n\"Banana\" in fruitSet: ${"Banana" in fruitSet}")
        println("fruitSet.containsAll([Apple, Cherry]): ${fruitSet.containsAll(listOf("Apple", "Cherry"))}")
    }

    // ──────────────────────────────────────────────────────────────
    // 1.4 解构
    //
    // Kotlin 解构依赖 componentN() 函数：
    //   data class → 编译器自动生成 component1(), component2(), …
    //   Map.Entry  → kotlin.collections 提供 component1() = key, component2() = value
    //   List       → component1()..component5() 预定义（仅前5个）
    // ──────────────────────────────────────────────────────────────
    private fun demo14Destructuring() {
        println("\n--- 1.4 Destructuring ---")

        // data class destructuring
        val item = Item("Mango", 3.0, "fruit")
        val (name, price, category) = item
        println("Destructured item: name=$name, price=$price, category=$category")

        // Map entry destructuring
        val prices = mapOf("Apple" to 1.5, "Banana" to 0.8, "Cherry" to 2.0)
        println("\nMap entries:")
        for ((k, v) in prices) {
            println("  $k = $$v")
        }

        // List component destructuring (up to component5)
        val (first, second, third) = listOf("x", "y", "z", "w")
        println("\nList destructuring: first=$first, second=$second, third=$third")

        // withIndex for index + value
        println("\nindexed iteration:")
        for ((index, value) in prices.entries.withIndex()) {
            println("  [$index] $value")
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Level 1 Support Classes
// ══════════════════════════════════════════════════════════════════

data class Item(val name: String, val price: Double, val category: String)
