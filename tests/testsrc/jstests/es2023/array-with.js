load("testsrc/assert.js");

(function withNoArgument() {
    var arr = [1, 2, 3];
    var arrWith = arr.with();
    assertTrue(Array.isArray(arrWith));
    assertEquals(3, arrWith.length);
    assertEquals(",2,3", arrWith.toString());
})();

(function withIndex() {
    var arr = [1, 2, 3];
    var arrWith = arr.with(1);
    assertEquals("1,,3", arrWith.toString());
})();

(function withIndexValue() {
    var arr = [1, 2, 3];
    var arrWith = arr.with(1, 4);
    assertEquals("1,4,3", arrWith.toString());
})();

(function withInvalidIndex() {
    var arr = [1, 2, 3];
    assertThrows(() => arr.with(42), RangeError);
})();

"success";
