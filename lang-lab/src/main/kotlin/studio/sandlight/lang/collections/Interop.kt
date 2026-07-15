package studio.sandlight.lang.collections

import studio.sandlight.lang.support.Lab

// LEVEL 4: Interop & Advanced — Java 互操作、并发集合、性能特性

import java.util.Collections
import java.util.LinkedList
import java.util.PriorityQueue
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis

object Interop {

    fun run() {

        demo41KotlinJavaConversion()
        demo42JavaCollectionTypes()
        demo43UnmodifiableVsReadOnly()
        demo44ConcurrentCollections()
        demo45ArrayDequeAsStackQueue()
        demo46PerformanceCharacteristics()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.1 Kotlin ↔ Java 集合转换
    //
    // Kotlin 集合本身就是 Java 集合（无包装层），传给 Java 方法零开销。
    //
    //   Kotlin List<T>  IS-A  java.util.List<T>
    //   Kotlin Set<T>   IS-A  java.util.Set<T>
    //   Kotlin Map<K,V> IS-A  java.util.Map<K,V>
    //
    // 所以 Kotlin 集合可以直接传给接受 java.util.List 的 Java 方法：
    //
    //   fun javaMethod(list: java.util.List<String>) { ... }
    //   javaMethod(kotlinList)   // ✅ 直接传入，无需转换
    //
    // 显式转换（用于改变底层类型）：
    //   kotlinList.toMutableList()  → 创建新的 ArrayList
    //   kotlinList.toList()         → 创建新的只读快照
    //   kotlinList.toTypedArray()   → Array<T>
    //   javaList.toList()           → Kotlin 只读 List（新对象）
    //
    // 阅读：kotlin/collections/_Collections.kt → toMutableList()
    // ──────────────────────────────────────────────────────────────
    private fun demo41KotlinJavaConversion() {
        Lab.section("4.1", "Kotlin ↔ Java Collection Conversion")

        val kotlinList = listOf("a", "b", "c")

        // 直接 IS-A 关系
        val javaList: java.util.List<String> = kotlinList as java.util.List<String>
        println("Kotlin List IS Java List: ${javaList.javaClass.name}")

        // Conversions
        val mutable   = kotlinList.toMutableList()   // new ArrayList
        val snapshot  = mutable.toList()             // new immutable snapshot
        val array     = kotlinList.toTypedArray()    // Array<String>
        val set       = kotlinList.toSet()           // LinkedHashSet
        val sortedSet = kotlinList.toSortedSet()     // TreeSet

        println("toMutableList → ${mutable.javaClass.simpleName}")
        println("toTypedArray  → ${array.javaClass.simpleName}: ${array.contentToString()}")
        println("toSet         → ${set.javaClass.simpleName}")
        println("toSortedSet   → ${sortedSet.javaClass.simpleName}")

        // Java → Kotlin
        val javaArrayList = java.util.ArrayList(listOf(1, 2, 3))
        val kotlinImmutable: List<Int> = javaArrayList.toList()  // snapshot
        javaArrayList.add(4)
        println("\nJava list after add: $javaArrayList")
        println("Kotlin snapshot unchanged: $kotlinImmutable")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.2 Java 集合类型
    //
    //   ArrayList     — 动态数组，随机访问 O(1)，尾插 O(1) 均摊
    //   LinkedList    — 双向链表，实现 Deque，头尾操作 O(1)，随机访问 O(n)
    //   TreeMap       — 红黑树，键有序，操作 O(log n)
    //   PriorityQueue — 二叉堆，peek/poll 最小值 O(1)/O(log n)
    //   ArrayDeque    — Kotlin 内置双端队列（基于数组，非 LinkedList）
    //
    // 阅读：java.base/java/util/ArrayList.java     → grow() 扩容逻辑
    //       java.base/java/util/LinkedList.java    → Node 内部类
    //       java.base/java/util/TreeMap.java       → Entry 红黑树节点
    //       java.base/java/util/PriorityQueue.java → siftDown() 堆化
    // ──────────────────────────────────────────────────────────────
    private fun demo42JavaCollectionTypes() {
        Lab.section("4.2", "Java Collection Types")

        // LinkedList as Deque (double-ended queue)
        val deque = LinkedList<String>()
        deque.addFirst("middle")
        deque.addFirst("front")
        deque.addLast("back")
        println("LinkedList as Deque: $deque")
        println("  peekFirst: ${deque.peekFirst()}, peekLast: ${deque.peekLast()}")
        println("  pollFirst: ${deque.pollFirst()}, remaining: $deque")

        // TreeMap — sorted by key
        val treeMap = TreeMap<String, Int>()
        treeMap["banana"] = 2
        treeMap["apple"] = 1
        treeMap["cherry"] = 3
        println("\nTreeMap (sorted): $treeMap")
        println("  firstKey: ${treeMap.firstKey()}, lastKey: ${treeMap.lastKey()}")
        println("  headMap(<cherry): ${treeMap.headMap("cherry")}")
        println("  tailMap(>=banana): ${treeMap.tailMap("banana")}")

        // PriorityQueue — min-heap by default
        val pq = PriorityQueue<Int>()
        pq.addAll(listOf(5, 1, 3, 2, 4))
        print("\nPriorityQueue poll order (min-heap): ")
        while (pq.isNotEmpty()) print("${pq.poll()} ")
        println()

        // Max-heap using reversed comparator
        val maxPq = PriorityQueue<Int>(compareByDescending { it })
        maxPq.addAll(listOf(5, 1, 3, 2, 4))
        print("Max-heap poll order:                ")
        while (maxPq.isNotEmpty()) print("${maxPq.poll()} ")
        println()
    }

    // ──────────────────────────────────────────────────────────────
    // 4.3 不可修改视图 vs Kotlin 只读
    //
    //   java.util.Collections.unmodifiableList(list)
    //     → 包装器，包装原始 list
    //     → 调用 add/set/remove 抛 UnsupportedOperationException（运行时）
    //     → 原始 list 被修改时，视图也跟着变（共享底层数组）
    //
    //   Kotlin listOf(...)
    //     → 编译时类型系统阻止调用 add/remove
    //     → 实际对象可能是可修改的 ArrayList（仅接口不同）
    //     → 无法保证运行时不可修改（见 Level 1 demo12）
    //
    //   真正安全的不可变集合：
    //     • java.util.List.of(...)     (Java 9+，真正不可修改)
    //     • com.google.common.collect.ImmutableList (Guava)
    //     • kotlinx.collections.immutable (推荐第三方库)
    // ──────────────────────────────────────────────────────────────
    private fun demo43UnmodifiableVsReadOnly() {
        Lab.section("4.3", "Unmodifiable Views vs Kotlin Read-Only")

        val mutableSource = mutableListOf(1, 2, 3)

        // Java unmodifiable wraps the same backing list
        val unmodifiable = Collections.unmodifiableList(mutableSource)
        println("unmodifiable: $unmodifiable")

        // Modification via original source IS visible through the view
        mutableSource.add(4)
        println("After source.add(4), unmodifiable view: $unmodifiable  ← changes with source!")

        // Direct modification of the view throws
        try {
            unmodifiable.add(5)
        } catch (e: UnsupportedOperationException) {
            println("unmodifiable.add() → UnsupportedOperationException ✓")
        }

        // Java 9+ List.of() — truly immutable, no backing reference
        val trueImmutable = java.util.List.of(1, 2, 3)
        println("\nList.of() type: ${trueImmutable.javaClass.name}")
        try {
            trueImmutable.add(4)
        } catch (e: UnsupportedOperationException) {
            println("List.of().add() → UnsupportedOperationException ✓")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4.4 并发集合
    //
    //   问题：多线程下普通集合不安全
    //     ArrayList.add() 扩容时非原子，并发写可能丢失数据或抛异常
    //
    //   方案对比：
    //   ┌────────────────────────────────────────────────────────┐
    //   │ Collections.synchronizedList(list)                     │
    //   │   → 全局 synchronized 锁，每次操作锁整个列表           │
    //   │   → 简单但性能差，迭代时需要手动 synchronized           │
    //   │                                                        │
    //   │ CopyOnWriteArrayList                                   │
    //   │   → 写时复制整个数组，读不加锁                         │
    //   │   → 适合：读多写少（观察者列表、事件监听器）           │
    //   │   → 不适合：频繁写（每次写 O(n) 内存分配）             │
    //   │                                                        │
    //   │ ConcurrentHashMap                                      │
    //   │   → 分段锁（Java 8+ 用 CAS + synchronized 桶级锁）     │
    //   │   → 读几乎无锁，写锁定单个桶                           │
    //   │   → 适合：高并发读写 Map                               │
    //   └────────────────────────────────────────────────────────┘
    //
    // 阅读：java.base/java/util/concurrent/ConcurrentHashMap.java
    //           → putVal() — 查看 CAS + synchronized 细节
    //       java.base/java/util/concurrent/CopyOnWriteArrayList.java
    //           → add() — 查看 setArray(Arrays.copyOf(...))
    // ──────────────────────────────────────────────────────────────
    private fun demo44ConcurrentCollections() {
        Lab.section("4.4", "Concurrent Collections")

        // CopyOnWriteArrayList — safe for concurrent reads
        val cowList = CopyOnWriteArrayList(listOf("a", "b", "c"))
        println("CopyOnWriteArrayList: $cowList")
        println("  type: ${cowList.javaClass.simpleName}")

        // Simulate concurrent read during write
        val reader = Thread {
            repeat(3) {
                println("  reader sees: $cowList")
                Thread.sleep(10)
            }
        }
        val writer = Thread {
            Thread.sleep(5)
            cowList.add("d")  // creates a new internal array, reader still sees old
            println("  writer added 'd'")
        }
        reader.start(); writer.start()
        reader.join();  writer.join()
        println("  final: $cowList")

        // ConcurrentHashMap
        val concurrentMap = ConcurrentHashMap<String, Int>()
        concurrentMap["a"] = 1
        concurrentMap["b"] = 2

        // merge — atomic read-modify-write
        concurrentMap.merge("a", 10) { old, new -> old + new }
        println("\nConcurrentHashMap after merge: $concurrentMap")

        // compute — atomic compute
        concurrentMap.compute("c") { _, v -> (v ?: 0) + 5 }
        println("After compute: $concurrentMap")

        // Collections.synchronizedMap — simpler but coarser lock
        val syncMap = Collections.synchronizedMap(mutableMapOf("x" to 1))
        println("\nsynchronizedMap type: ${syncMap.javaClass.simpleName}")
        println("  Note: iteration still needs external synchronization")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.5 ArrayDeque 作为栈/队列
    //
    // Kotlin ArrayDeque（kotlin.collections.ArrayDeque）基于数组实现，
    // 头尾操作均摊 O(1)。比 java.util.LinkedList 更节省内存（无 Node 对象）。
    //
    //   用作 Stack（LIFO）：
    //     push(e)  = addLast(e)   ← 压栈
    //     pop()    = removeLast() ← 弹栈
    //     peek()   = last()
    //
    //   用作 Queue（FIFO）：
    //     enqueue = addLast(e)
    //     dequeue = removeFirst()
    //     peek    = first()
    //
    // 阅读：kotlin/collections/ArrayDeque.kt（Kotlin 源码，使用 copyInto 扩容）
    // ──────────────────────────────────────────────────────────────
    private fun demo45ArrayDequeAsStackQueue() {
        Lab.section("4.5", "ArrayDeque as Stack and Queue")

        // Stack (LIFO)
        val stack = ArrayDeque<String>()
        stack.addLast("first")   // push
        stack.addLast("second")
        stack.addLast("third")
        println("Stack after pushes: $stack")
        println("  pop: ${stack.removeLast()}")
        println("  pop: ${stack.removeLast()}")
        println("  peek: ${stack.last()}")
        println("  stack: $stack")

        // Queue (FIFO)
        val queue = ArrayDeque<Int>()
        queue.addLast(1)    // enqueue
        queue.addLast(2)
        queue.addLast(3)
        println("\nQueue after enqueues: $queue")
        println("  dequeue: ${queue.removeFirst()}")
        println("  dequeue: ${queue.removeFirst()}")
        println("  peek: ${queue.first()}")
        println("  queue: $queue")

        // BFS example using deque as queue
        println("\nBFS traversal (tree levels):")
        val tree = mapOf(
            1 to listOf(2, 3),
            2 to listOf(4, 5),
            3 to listOf(6),
            4 to emptyList(),
            5 to emptyList(),
            6 to emptyList()
        )
        val bfsQueue = ArrayDeque<Int>()
        bfsQueue.addLast(1)
        val visited = mutableListOf<Int>()
        while (bfsQueue.isNotEmpty()) {
            val node = bfsQueue.removeFirst()
            visited.add(node)
            tree[node]?.forEach { bfsQueue.addLast(it) }
        }
        println("  BFS order: $visited")
    }

    // ──────────────────────────────────────────────────────────────
    // 4.6 集合性能特性速查表
    //
    // ┌──────────────────┬────────┬────────┬────────┬────────┐
    // │ 集合             │ get(i) │ add    │ remove │ search │
    // ├──────────────────┼────────┼────────┼────────┼────────┤
    // │ ArrayList        │ O(1)   │ O(1)*  │ O(n)** │ O(n)   │
    // │ LinkedList       │ O(n)   │ O(1)†  │ O(1)†  │ O(n)   │
    // │ HashSet/HashMap  │ N/A    │ O(1)*  │ O(1)*  │ O(1)*  │
    // │ LinkedHashSet    │ N/A    │ O(1)*  │ O(1)*  │ O(1)*  │
    // │ TreeSet/TreeMap  │ N/A    │ O(log n)│O(log n)│O(log n)│
    // │ ArrayDeque       │ O(1)   │ O(1)*† │ O(1)*† │ O(n)   │
    // └──────────────────┴────────┴────────┴────────┴────────┘
    //  * = amortized (均摊)
    // ** = remove by index at end O(1), at start O(n)
    //  † = at head or tail
    //
    // 内存占用：
    //   ArrayList      → 连续数组，内存紧凑，缓存友好
    //   LinkedList     → 每个节点额外存 prev/next 指针（24 bytes overhead/node）
    //   HashMap        → 负载因子 0.75，预留 25% 空间防哈希碰撞
    //   ConcurrentHashMap → 比 HashMap 更多元数据，但并发安全
    // ──────────────────────────────────────────────────────────────
    private fun demo46PerformanceCharacteristics() {
        Lab.section("4.6", "Performance Characteristics")

        val n = 100_000

        // ArrayList vs LinkedList: random access
        val arrayList = ArrayList<Int>((1..n).toList())
        val linkedList = LinkedList<Int>((1..n).toList())

        val alTime = measureTimeMillis { repeat(1000) { arrayList[n / 2] } }
        val llTime = measureTimeMillis { repeat(1000) { linkedList[n / 2] } }
        println("Random access (index n/2), 1000 times:")
        println("  ArrayList:   ${alTime}ms")
        println("  LinkedList:  ${llTime}ms  (O(n) traversal each time)")

        // HashSet vs TreeSet: contains()
        val hashSet   = HashSet<Int>((1..n).toList())
        val treeSet   = java.util.TreeSet<Int>((1..n).toList())
        val hsTime = measureTimeMillis { repeat(10_000) { hashSet.contains(n / 2) } }
        val tsTime = measureTimeMillis { repeat(10_000) { treeSet.contains(n / 2) } }
        println("\ncontains() 10,000 times:")
        println("  HashSet:  ${hsTime}ms  (O(1) hash lookup)")
        println("  TreeSet:  ${tsTime}ms  (O(log n) tree traversal)")

        // ArrayList prepend — expensive due to shifting
        val al = ArrayList<Int>((1..1000).toList())
        val alPrependTime = measureTimeMillis { repeat(100) { al.add(0, -1) } }
        val deque = ArrayDeque<Int>((1..1000).toList())
        val dqPrependTime = measureTimeMillis { repeat(100) { deque.addFirst(-1) } }
        println("\nPrepend 100 times:")
        println("  ArrayList[0, elem]: ${alPrependTime}ms  (O(n) shift)")
        println("  ArrayDeque.addFirst: ${dqPrependTime}ms  (O(1) amortized)")
    }

}
