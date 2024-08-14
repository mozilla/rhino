load("testsrc/assert.js");

(function reversingArray() {
    var arr = [1, 2, 3];
    var reversed = arr.toReversed();
    assertTrue(Array.isArray(reversed));
    assertEquals(3, reversed.length);
    assertEquals("3,2,1", reversed.toString());
})();

"success";
