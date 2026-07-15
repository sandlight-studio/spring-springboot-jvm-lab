package studio.sandlight.jvm

fun stringInternDemo() {
    val a = "hello"
    val b = "he" + "llo" // compile-time constant: folded and interned, same pooled object as `a`
    val c = buildString { append("he"); append("llo") } // built at runtime: equal content, distinct object
    val d = String("hello".toCharArray()) // explicitly allocated new String instance

    println("a === b (same object)?   ${a === b}") // true  — both are the pooled literal
    println("a == c  (equal content)? ${a == c}")  // true
    println("a === c (same object)?   ${a === c}") // false — runtime-built, not from the pool
    println("a === d (same object)?   ${a === d}") // false

    // intern() returns the pooled instance for equal content.
    println("c.intern() === a?        ${c.intern() === a}") // true
    println("d.intern() === a?        ${d.intern() === a}") // true
}
