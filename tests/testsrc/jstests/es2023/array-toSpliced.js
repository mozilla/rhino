load("testsrc/assert.js");

(function toSplicedNoArgument() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced();
    assertTrue(Array.isArray(spliced));
    assertEquals(3, spliced.length);
    assertEquals("1,2,3", spliced.toString());
})();

(function toSplicedStart() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(2);
    assertEquals("1,2", spliced.toString());
})();

(function toSplicedNegativeStart() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(-1);
    assertEquals("1,2", spliced.toString());
})();

(function toSplicedStartUndefined() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(undefined);
    assertEquals("", spliced.toString());
})();

(function toSplicedStartAfterMaxLength() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(3);
    assertEquals("1,2,3", spliced.toString());
})();

(function toSplicedStartAndCount() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(0, 1);
    assertEquals("2,3", spliced.toString());
})();

(function toSplicedStartAndUndefinedCount() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(2, undefined);
    assertEquals("1,2,3", spliced.toString());
})();

(function toSplicedItemsToInsert() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(0, 1, 4, 5);
    assertEquals("4,5,2,3", spliced.toString());
})();

(function toSplicedMaxLengthExceedingArrayLengthLimit() {
    var arr = {length: 2**32};
    assertThrows(
        () => Array.prototype.toSpliced.call(arr, 0, 0),
        RangeError
    );
})();

"success";
