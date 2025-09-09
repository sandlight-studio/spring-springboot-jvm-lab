package studio.sandlight.jvm

fun stringInternDemo() {
    val a = "hello"
    val b = "he" + "llo" // compile-time constant; same interned literal
    val c = ("he" + System.currentTimeMillis().toString().substring(100.coerceAtMost(0))) + "llo" // ensures not const
    val d = String("hello".toCharArray()) // new String instance

    println("a === b (same object)? ${a === b}")
    println("a === c (same object)? ${a === c}")
    println("a === d (same object)? ${a === d}")
    println("a == d  (equal content)? ${a == d}")

    val e = d.intern()
    println("intern(d) === a? ${e === a}")
}

