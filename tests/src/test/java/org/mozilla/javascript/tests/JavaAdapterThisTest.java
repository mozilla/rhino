package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Inside a method of a JavaScript object that implements a Java interface or extends an abstract
 * class, {@code this} must see the other properties of that object.
 */
public class JavaAdapterThisTest {

    @Test
    void interfaceMethodSeesOwnProperties() {
        Utils.assertWithAllModes(
                "Test",
                "var s = new java.util.function.Supplier({\n"
                        + "  value: 'Test',\n"
                        + "  get: function() { return this.value; }\n"
                        + "});\n"
                        + "String(s.get());");
    }

    @Test
    void javaAdapterConstructor() {
        Utils.assertWithAllModes(
                "Test",
                "var s = new JavaAdapter(java.util.function.Supplier, {\n"
                        + "  value: 'Test',\n"
                        + "  get: function() { return this.value; }\n"
                        + "});\n"
                        + "String(s.get());");
    }

    @Test
    void javaAdapterCalledAsFunction() {
        Utils.assertWithAllModes(
                "Test",
                "var s = JavaAdapter(java.util.function.Supplier, {\n"
                        + "  value: 'Test',\n"
                        + "  get: function() { return this.value; }\n"
                        + "});\n"
                        + "String(s.get());");
    }

    @Test
    void abstractClassMethodSeesOwnProperties() {
        Utils.assertWithAllModes(
                "3:b:false",
                "var list = new java.util.AbstractList({\n"
                        + "  items: ['a', 'b', 'c'],\n"
                        + "  get: function(i) { return this.items[i]; },\n"
                        + "  size: function() { return this.items.length; }\n"
                        + "});\n"
                        + "list.size() + ':' + list.get(1) + ':' + list.isEmpty();");
    }

    @Test
    void writesThroughThisReachTheObject() {
        Utils.assertWithAllModes(
                "3",
                "var impl = {\n"
                        + "  n: 0,\n"
                        + "  get: function() { this.n++; return null; }\n"
                        + "};\n"
                        + "var s = new java.util.function.Supplier(impl);\n"
                        + "s.get(); s.get(); s.get();\n"
                        + "String(impl.n);");
    }
}
