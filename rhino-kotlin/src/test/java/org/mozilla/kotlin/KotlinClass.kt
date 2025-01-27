package org.mozilla.kotlin

class KotlinClass(
    val nonNullProperty: String,
    val nullableProperty: String?
) {
    fun function(nullableParam: Int?, nonNullParam: Int, anotherNullableParam: KotlinClass?) {
        // Do nothing
    }

    fun function(nonNullParam: Int, anotherNonNullParam: Int) {
        // Do nothing
    }
}
