package studio.sandlight.lang

data class Person(val name: String, val age: Int)

object CollectionsBasics {
    fun run() {
        println("-- Collections basics --")

        // Immutable vs mutable
        val list = listOf(1, 2, 3)
        val mlist = mutableListOf(1, 2, 3)
        mlist += 4
        println("list=$list, mlist=$mlist")

        val set = setOf(1, 2, 2, 3)
        println("set (unique): $set")

        val map = mapOf("a" to 1, "b" to 2)
        println("map: $map, getOrDefault: ${map.getOrDefault("c", -1)}")

        // Transformations
        val people = listOf(
            Person("Ann", 30), Person("Bob", 25), Person("Cara", 30)
        )
        val names = people.map { it.name }
        val adults = people.filter { it.age >= 30 }
        val byAge = people.groupBy { it.age }
        val byName = people.associateBy { it.name }
        val sumAges = people.sumOf { it.age }
        println("names=$names, adults=$adults, sumAges=$sumAges")
        println("groupBy age=$byAge")
        println("associateBy name keys=${byName.keys}")

        // Sequences (lazy)
        val seq = generateSequence(1) { it + 1 }
            .map { it * it }
            .filter { it % 2 == 0 }
            .take(5)
            .toList()
        println("first 5 even squares via sequence: $seq")

        // Java interop
        val jList = java.util.ArrayList(listOf("a", "b"))
        println("java.util.List size=${jList.size}, type=${jList.javaClass.simpleName}")
    }
}
