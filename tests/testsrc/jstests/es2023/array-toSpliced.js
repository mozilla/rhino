load("testsrc/assert.js");

(function toSplicedNoArgument() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced();
    assertTrue(Array.isArray(spliced));
    assertEquals(3, spliced.length);
    assertEquals("1,2,3", spliced.toString());
})();

(function toSplicedOneElement() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(2);
    assertEquals("1,2", spliced.toString());
})();

(function toSplicedAfterLength() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(3);
    assertEquals("1,2,3", spliced.toString());
})();

(function toSplicedStartAndCount() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(0, 1);
    assertEquals("2,3", spliced.toString());
})();

(function toSplicedItemsToInsert() {
    var arr = [1, 2, 3];
    var spliced = arr.toSpliced(0, 1, 4, 5);
    assertEquals("4,5,2,3", spliced.toString());
})();

"success";
