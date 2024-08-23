load("testsrc/assert.js");

(function toReversedBasic() {
    var arr = [1, 2, 3];
    var reversed = arr.toReversed();
    
    assertTrue(Array.isArray(reversed));
    assertEquals(3, reversed.length);
    assertEquals("3,2,1", reversed.toString());

    assertEquals(3, arr.length);
    assertEquals("1,2,3", arr.toString());
})();

(function toReversedArrayPreservesHoles() {
    var arr = [0, /* hole */, 2, /* hole */, 4];
    Array.prototype[3] = 3;
    var reversed = arr.toReversed();
    assertEquals("4,3,2,,0", reversed.toString());
    assertTrue(reversed.hasOwnProperty(3));
})();

(function toReversedMaxLengthExceedingArrayLengthLimit() {
    var arr = {length: 2**32};
    assertThrows(
        () => Array.prototype.toReversed.call(arr, 0, 0),
        RangeError
    );
})();

"success";
