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

(function withIndexAndValue() {
    var arr = [1, 2, 3];
    var arrWith = arr.with(1, 4);
    assertEquals("1,4,3", arrWith.toString());
})();

(function withNegativeIndexAndValue() {
    var arr = [1, 2, 3];
    var arrWith = arr.with(-2, 5);
    assertEquals("1,5,3", arrWith.toString());
})();

(function withIndexTooLarge() {
    var arr = [1, 2, 3];
    assertThrows(() => arr.with(3), RangeError);
})();

(function withIndexTooLarge() {
    var arr = [1, 2, 3];
    assertThrows(() => arr.with(3), RangeError);
})();

(function withMaxLengthExceedingArrayLengthLimit() {
    var arr = {length: 2**32};
    assertThrows(
        () => Array.prototype.with.call(arr, 0, 0),
        RangeError
    );
})();

"success";
