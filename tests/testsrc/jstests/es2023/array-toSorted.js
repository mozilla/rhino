load("testsrc/assert.js");

(function basicSorting() {
    var arr = [3, 1, 2, 4, 5, 6, 0, 7];
    var sorted = arr.toSorted();
    assertTrue(Array.isArray(sorted));
    assertEquals(8, sorted.length);
    assertEquals("0,1,2,3,4,5,6,7", sorted.toString());
})();

(function passingComparatorFunction() {
    var arr = [-1, -3, -2, -4, -1, -3, 0, 3, 1];
    var sorted = arr.toSorted((a, b) => b.toString().length - a.toString().length);
    assertEquals("-1,-3,-2,-4,-1,-3,0,3,1", sorted.toString());
})();

(function sortIsStable() {
    var arr = [3, 1, 2, 4, 5, 6, 0, 7];
    var sorted = arr.toSorted((a, b) => a % 2 - b % 2);
    assertEquals("2,4,6,0,3,1,5,7", sorted.toString());
})();

"success";
