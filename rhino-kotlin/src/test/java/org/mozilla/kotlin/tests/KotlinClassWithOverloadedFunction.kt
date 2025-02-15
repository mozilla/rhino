package org.mozilla.kotlin.tests

class KotlinClassWithOverloadedFunction {
    fun function(nullableParam: Int?, nonNullParam: Int, anotherNullableParam: KotlinClass?) {
        // Do nothing
    }

    fun function(nullableParam: Int?, nonNullParam: Int, anotherNonNullParam: String) {
        // Do nothing
    }
}
