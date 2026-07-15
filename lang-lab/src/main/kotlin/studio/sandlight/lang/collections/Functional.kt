package studio.sandlight.lang.collections

import studio.sandlight.lang.support.Lab

// LEVEL 2: Functional — 变换、聚合、分组、排序

object Functional {

    fun run() {

        demo21Transformations()
        demo22Filtering()
        demo23Aggregation()
        demo24GroupingAndAssociation()
        demo25Sorting()
        demo26ExistenceChecks()
    }

    // ──────────────────────────────────────────────────────────────
    // 2.1 变换
    //
    // map       → 1:1 变换，输出与输入等长
    // mapNotNull → map + 过滤 null，输出可能更短
    // flatMap   → 每个元素映射为集合，然后展平
    //
    //   map vs flatMap：
    //
    //   ["a","bc"].map { it.toList() }
    //   → [ ['a'], ['b','c'] ]   ← 嵌套，List<List<Char>>
    //
    //   ["a","bc"].flatMap { it.toList() }
    //   → [ 'a', 'b', 'c' ]     ← 展平，List<Char>
    //
    // 阅读：kotlin/collections/_Collections.kt → map { } / flatMap { }
    // ──────────────────────────────────────────────────────────────
    private fun demo21Transformations() {
        Lab.section("2.1", "Transformations")

        val products = listOf(
            Item("Apple",  1.5,  "fruit"),
            Item("Banana", 0.8,  "fruit"),
            Item("Carrot", 0.5,  "vegetable"),
            Item("Durian", 12.0, "fruit")
        )

        // map
        val names = products.map { it.name }
        println("map names: $names")

        // mapIndexed
        val indexed = products.mapIndexed { i, p -> "$i:${p.name}" }
        println("mapIndexed: $indexed")

        // mapNotNull — filters nulls in one pass
        val maybeNames = listOf("Apple", null, "Banana", null, "Cherry")
        val nonNull    = maybeNames.mapNotNull { it?.uppercase() }
        println("mapNotNull: $nonNull")

        // flatMap — flatten nested structure
        val words    = listOf("hello world", "foo bar baz")
        val allWords = words.flatMap { it.split(" ") }
        println("flatMap split: $allWords")

        // flatten — when you already have nested collections
        val nested   = listOf(listOf(1, 2), listOf(3, 4), listOf(5))
        val flat     = nested.flatten()
        println("flatten: $flat")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.2 过滤
    //
    // take/drop:     eager, returns new List
    // takeWhile/dropWhile: stops at first non-matching element
    //
    // filterIsInstance<T>() — 类型安全过滤，返回 List<T>
    //   等价于 filter { it is T }.map { it as T }，但更高效
    // ──────────────────────────────────────────────────────────────
    private fun demo22Filtering() {
        Lab.section("2.2", "Filtering")

        val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        println("filter even:     ${numbers.filter { it % 2 == 0 }}")
        println("filterNot even:  ${numbers.filterNot { it % 2 == 0 }}")
        println("take(4):         ${numbers.take(4)}")
        println("drop(7):         ${numbers.drop(7)}")
        println("takeWhile <5:    ${numbers.takeWhile { it < 5 }}")
        println("dropWhile <5:    ${numbers.dropWhile { it < 5 }}")

        // filterIsInstance
        val mixed: List<Any> = listOf(1, "hello", 2.0, "world", 3, true)
        val strings = mixed.filterIsInstance<String>()
        println("filterIsInstance<String>: $strings")

        // filterNotNull
        val withNulls: List<Int?> = listOf(1, null, 2, null, 3)
        val noNulls: List<Int>    = withNulls.filterNotNull()
        println("filterNotNull: $noNulls")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.3 聚合
    //
    // fold vs reduce：
    //   fold(initial) { acc, elem → ... }
    //     ├── 有显式初始值
    //     ├── 空集合返回 initial（安全）
    //     └── 结果类型可与元素类型不同
    //
    //   reduce { acc, elem → ... }
    //     ├── 以第一个元素为初始值
    //     ├── 空集合抛 UnsupportedOperationException（危险！）
    //     └── 结果类型必须与元素类型相同
    //
    //   runningFold / scan = fold 的逐步快照版
    //     [1,2,3].runningFold(0) { a, e → a+e } → [0, 1, 3, 6]
    //
    // 阅读：kotlin/collections/_Collections.kt → fold(), reduce()
    // ──────────────────────────────────────────────────────────────
    private fun demo23Aggregation() {
        Lab.section("2.3", "Aggregation")

        val nums = listOf(1, 2, 3, 4, 5)

        // Basic aggregates
        println("sum:    ${nums.sum()}")
        println("sumOf:  ${nums.sumOf { it * 2 }}")
        println("count:  ${nums.count()}")
        println("count{>3}: ${nums.count { it > 3 }}")
        println("max:    ${nums.max()}")
        println("min:    ${nums.min()}")
        println("average:${nums.average()}")

        // fold — safe on empty, return type can differ
        val sentence = listOf("Kotlin", "collections", "are", "great")
        val joined   = sentence.fold("Start:") { acc, word -> "$acc $word" }
        println("\nfold into string: $joined")

        // reduce — dangerous on empty collection
        val product = nums.reduce { acc, n -> acc * n }
        println("reduce (product): $product")

        try {
            emptyList<Int>().reduce { acc, n -> acc + n }
        } catch (e: UnsupportedOperationException) {
            println("reduce on empty → UnsupportedOperationException (use fold instead)")
        }

        // runningFold — shows intermediate results (useful for running totals)
        val runningSum = nums.runningFold(0) { acc, n -> acc + n }
        println("runningFold sums: $runningSum")

        // scan is an alias for runningFold in modern Kotlin
        val scan = nums.scan(100) { acc, n -> acc + n }
        println("scan starting at 100: $scan")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.4 分组与关联
    //
    //   groupBy { key }       → Map<K, List<V>>  (多值)
    //   associateBy { key }   → Map<K, V>        (单值，后者覆盖前者)
    //   associateWith { val } → Map<V, R>        (以元素为 key)
    //   partition { pred }    → Pair<List, List> (true组 + false组)
    //
    //   zip:
    //     [1,2,3].zip([a,b,c]) → [(1,a),(2,b),(3,c)]
    //     长度取较短者，多余元素丢弃
    //
    //   zipWithNext:
    //     [1,2,3,4].zipWithNext() → [(1,2),(2,3),(3,4)]
    //     适合计算相邻差值
    // ──────────────────────────────────────────────────────────────
    private fun demo24GroupingAndAssociation() {
        Lab.section("2.4", "Grouping & Association")

        val products = listOf(
            Item("Apple",  1.5,  "fruit"),
            Item("Banana", 0.8,  "fruit"),
            Item("Carrot", 0.5,  "vegetable"),
            Item("Durian", 12.0, "fruit"),
            Item("Eggplant", 1.2, "vegetable")
        )

        // groupBy
        val byCategory = products.groupBy { it.category }
        println("groupBy category:")
        byCategory.forEach { (cat, items) ->
            println("  $cat: ${items.map { it.name }}")
        }

        // associateBy — last duplicate wins
        val byName = products.associateBy { it.name }
        println("\nassociateBy name keys: ${byName.keys}")

        // associateWith
        val nameToPriceMap = products.map { it.name }.associateWith { name ->
            products.first { it.name == name }.price
        }
        println("associateWith price: $nameToPriceMap")

        // partition
        val (expensive, cheap) = products.partition { it.price > 2.0 }
        println("\npartition price > 2.0:")
        println("  expensive: ${expensive.map { it.name }}")
        println("  cheap:     ${cheap.map { it.name }}")

        // zip
        val names  = listOf("a", "b", "c", "d")
        val values = listOf(1, 2, 3)            // shorter — "d" is dropped
        val zipped = names.zip(values)
        println("\nzip (d dropped): $zipped")

        // zipWithNext — adjacent pairs
        val temps   = listOf(20, 23, 19, 25, 22)
        val changes = temps.zipWithNext { a, b -> b - a }
        println("zipWithNext (temp changes): $changes")

        // unzip
        val pairs    = listOf("x" to 1, "y" to 2, "z" to 3)
        val (letters, numbers) = pairs.unzip()
        println("unzip: letters=$letters, numbers=$numbers")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.5 排序
    //
    //   sortedBy { key }       → ASC 自然序
    //   sortedByDescending     → DESC
    //   sortedWith(comparator) → 自定义比较器
    //
    //   compareBy { } 构建 Comparator，支持链式：
    //     compareBy({ it.category }, { it.price })
    //     先按 category 字母序，相同则按 price ASC
    //
    //   thenBy / thenByDescending → 追加次要排序键
    //
    // 底层：java.util.Arrays.sort() (TimSort，稳定排序 O(n log n))
    // ──────────────────────────────────────────────────────────────
    private fun demo25Sorting() {
        Lab.section("2.5", "Sorting")

        val products = listOf(
            Item("Apple",    1.5,  "fruit"),
            Item("Banana",   0.8,  "fruit"),
            Item("Carrot",   0.5,  "vegetable"),
            Item("Durian",   12.0, "fruit"),
            Item("Eggplant", 1.2,  "vegetable")
        )

        val byPrice = products.sortedBy { it.price }
        println("sortedBy price: ${byPrice.map { "${it.name}(${it.price})" }}")

        val byPriceDesc = products.sortedByDescending { it.price }
        println("sortedByDesc:   ${byPriceDesc.map { it.name }}")

        // multi-key sort: category ASC, then price DESC within category
        val multiSort = products.sortedWith(
            compareBy<Item> { it.category }.thenByDescending { it.price }
        )
        println("category+price desc: ${multiSort.map { "${it.category}/${it.name}(${it.price})" }}")

        // sortedWith Comparator — using compareBy shorthand
        val byNameLength = products.sortedWith(compareBy({ it.name.length }, { it.name }))
        println("by name length then alpha: ${byNameLength.map { it.name }}")
    }

    // ──────────────────────────────────────────────────────────────
    // 2.6 存在性检查
    //
    //   any  — 至少一个满足（短路求值）
    //   all  — 全部满足（遇 false 立即返回）
    //   none — 全不满足
    //   find / findLast — 找到第一个/最后一个满足条件的元素（或 null）
    //
    // 与 Java Stream 的对比：
    //   any  ↔ stream.anyMatch()
    //   all  ↔ stream.allMatch()
    //   none ↔ stream.noneMatch()
    //   find ↔ stream.filter().findFirst().orElse(null)
    // ──────────────────────────────────────────────────────────────
    private fun demo26ExistenceChecks() {
        Lab.section("2.6", "Existence Checks")

        val products = listOf(
            Item("Apple",  1.5,  "fruit"),
            Item("Banana", 0.8,  "fruit"),
            Item("Carrot", 0.5,  "vegetable"),
            Item("Durian", 12.0, "fruit")
        )

        println("any price > 10:   ${products.any { it.price > 10 }}")
        println("all price > 0:    ${products.all { it.price > 0 }}")
        println("none price < 0:   ${products.none { it.price < 0 }}")
        println("find price > 1:   ${products.find { it.price > 1 }?.name}")
        println("findLast fruit:   ${products.findLast { it.category == "fruit" }?.name}")

        // in operator delegates to contains()
        val names = products.map { it.name }
        println("\n\"Carrot\" in names: ${"Carrot" in names}")
        println("\"Mango\" !in names: ${"Mango" !in names}")
    }
}
