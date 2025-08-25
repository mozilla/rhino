load("testsrc/assert.js");

// Test ES2025 Set methods integration scenarios

(function TestSetIntersectionWithVariousTypes() {
    var set1 = new Set([1, 2, 3, 'a', 'b']);
    var set2 = new Set([2, 3, 4, 'b', 'c']);
    
    var result = set1.intersection(set2);
    
    assertEquals(3, result.size);
    assertTrue(result.has(2));
    assertTrue(result.has(3));
    assertTrue(result.has('b'));
    assertFalse(result.has(1));
    assertFalse(result.has('a'));
    assertFalse(result.has(4));
})();

(function TestSetUnionDeduplication() {
    var set1 = new Set([1, 2, 3]);
    var set2 = new Set([3, 4, 5]);
    
    var result = set1.union(set2);
    
    assertEquals(5, result.size);
    assertTrue(result.has(1));
    assertTrue(result.has(2));
    assertTrue(result.has(3)); // Should appear only once
    assertTrue(result.has(4));
    assertTrue(result.has(5));
})();

(function TestSetDifferenceWithEmptySets() {
    var set1 = new Set([1, 2, 3]);
    var empty = new Set();
    
    var result1 = set1.difference(empty);
    var result2 = empty.difference(set1);
    
    assertEquals(3, result1.size);
    assertEquals(0, result2.size);
    
    assertTrue(result1.has(1));
    assertTrue(result1.has(2));
    assertTrue(result1.has(3));
})();

(function TestSetSymmetricDifferenceCommutative() {
    var set1 = new Set([1, 2, 3]);
    var set2 = new Set([3, 4, 5]);
    
    var result1 = set1.symmetricDifference(set2);
    var result2 = set2.symmetricDifference(set1);
    
    // Should be commutative
    assertEquals(result1.size, result2.size);
    assertEquals(4, result1.size);
    
    // Both should have elements 1, 2, 4, 5 but not 3
    for (var item of [1, 2, 4, 5]) {
        assertTrue(result1.has(item));
        assertTrue(result2.has(item));
    }
    assertFalse(result1.has(3));
    assertFalse(result2.has(3));
})();

(function TestSetSubsetSupersetRelationships() {
    var smallSet = new Set([1, 2]);
    var largeSet = new Set([1, 2, 3, 4]);
    var disjointSet = new Set([5, 6]);
    
    // Subset relationships
    assertTrue(smallSet.isSubsetOf(largeSet));
    assertTrue(smallSet.isSubsetOf(smallSet)); // Every set is subset of itself
    assertFalse(largeSet.isSubsetOf(smallSet));
    assertFalse(smallSet.isSubsetOf(disjointSet));
    
    // Superset relationships
    assertTrue(largeSet.isSupersetOf(smallSet));
    assertTrue(largeSet.isSupersetOf(largeSet)); // Every set is superset of itself
    assertFalse(smallSet.isSupersetOf(largeSet));
    assertFalse(smallSet.isSupersetOf(disjointSet));
})();

(function TestSetDisjointRelationships() {
    var set1 = new Set([1, 2, 3]);
    var set2 = new Set([4, 5, 6]);
    var set3 = new Set([3, 4, 5]);
    
    // Disjoint sets
    assertTrue(set1.isDisjointFrom(set2));
    assertTrue(set2.isDisjointFrom(set1)); // Should be symmetric
    
    // Non-disjoint sets
    assertFalse(set1.isDisjointFrom(set3));
    assertFalse(set3.isDisjointFrom(set1));
    
    // Set with itself is never disjoint (unless empty)
    assertFalse(set1.isDisjointFrom(set1));
})();

(function TestSetMethodsWithSetLikeObjects() {
    var realSet = new Set([1, 2, 3]);
    
    // Create set-like object
    var setLike = {
        size: 2,
        has: function(value) { return value === 2 || value === 4; },
        keys: function() { return [2, 4][Symbol.iterator](); }
    };
    
    var intersection = realSet.intersection(setLike);
    var union = realSet.union(setLike);
    
    // Intersection should contain only 2
    assertEquals(1, intersection.size);
    assertTrue(intersection.has(2));
    
    // Union should contain 1, 2, 3, 4
    assertEquals(4, union.size);
    assertTrue(union.has(1));
    assertTrue(union.has(2));
    assertTrue(union.has(3));
    assertTrue(union.has(4));
})();

(function TestSetMethodsWithArrays() {
    var set = new Set([1, 2, 3]);
    
    // Arrays are set-like if they have size, has, and keys
    var arrayLike = {
        size: 2,
        has: function(value) { return this.data.indexOf(value) !== -1; },
        keys: function() { return this.data[Symbol.iterator](); },
        data: ['a', 'b']
    };
    
    var union = set.union(arrayLike);
    
    assertEquals(5, union.size);
    assertTrue(union.has(1));
    assertTrue(union.has(2));
    assertTrue(union.has(3));
    assertTrue(union.has('a'));
    assertTrue(union.has('b'));
})();

(function TestSetMethodsWithSpecialValues() {
    var set1 = new Set([0, -0, NaN, undefined, null]);
    var set2 = new Set([-0, NaN, null, false]);
    
    var intersection = set1.intersection(set2);
    
    // Should handle special equality rules
    assertTrue(intersection.has(0)); // 0 and -0 are treated as same
    assertTrue(intersection.has(NaN)); // NaN should equal itself in Set
    assertTrue(intersection.has(null));
    assertFalse(intersection.has(undefined));
    assertFalse(intersection.has(false));
})();

(function TestSetMethodsPreserveOriginalSets() {
    var set1 = new Set([1, 2, 3]);
    var set2 = new Set([3, 4, 5]);
    
    var originalSet1Size = set1.size;
    var originalSet2Size = set2.size;
    
    // Perform operations
    var intersection = set1.intersection(set2);
    var union = set1.union(set2);
    var difference = set1.difference(set2);
    var symDiff = set1.symmetricDifference(set2);
    
    // Original sets should be unchanged
    assertEquals(originalSet1Size, set1.size);
    assertEquals(originalSet2Size, set2.size);
    assertTrue(set1.has(1));
    assertTrue(set1.has(2));
    assertTrue(set1.has(3));
    assertTrue(set2.has(3));
    assertTrue(set2.has(4));
    assertTrue(set2.has(5));
})();

(function TestSetMethodsErrorConditions() {
    var set = new Set([1, 2, 3]);
    
    // Test with objects that don't have required methods
    assertThrows(function() {
        set.intersection({});
    }, TypeError);
    
    assertThrows(function() {
        set.union({ size: 1 }); // Missing has and keys
    }, TypeError);
    
    assertThrows(function() {
        set.intersection({ 
            size: 1, 
            has: function() { return true; }
            // Missing keys method
        });
    }, TypeError);
})();

(function TestSetMethodsChaining() {
    var set1 = new Set([1, 2, 3, 4, 5]);
    var set2 = new Set([4, 5, 6, 7]);
    var set3 = new Set([6, 7, 8, 9]);
    
    // Chain operations
    var result = set1.intersection(set2).union(set3);
    
    // set1 ∩ set2 = {4, 5}
    // {4, 5} ∪ set3 = {4, 5, 6, 7, 8, 9}
    assertEquals(6, result.size);
    assertTrue(result.has(4));
    assertTrue(result.has(5));
    assertTrue(result.has(6));
    assertTrue(result.has(7));
    assertTrue(result.has(8));
    assertTrue(result.has(9));
})();

(function TestSetMethodsWithLargeSets() {
    var largeSet1 = new Set();
    var largeSet2 = new Set();
    
    // Create large sets
    for (var i = 0; i < 1000; i++) {
        largeSet1.add(i);
    }
    for (var i = 500; i < 1500; i++) {
        largeSet2.add(i);
    }
    
    var intersection = largeSet1.intersection(largeSet2);
    var union = largeSet1.union(largeSet2);
    
    // Intersection should be 500-999 (500 elements)
    assertEquals(500, intersection.size);
    assertTrue(intersection.has(500));
    assertTrue(intersection.has(999));
    assertFalse(intersection.has(499));
    assertFalse(intersection.has(1000));
    
    // Union should be 0-1499 (1500 elements)
    assertEquals(1500, union.size);
    assertTrue(union.has(0));
    assertTrue(union.has(1499));
})();

(function TestSetMethodsReturnNewSets() {
    var original = new Set([1, 2, 3]);
    var other = new Set([2, 3, 4]);
    
    var intersection = original.intersection(other);
    var union = original.union(other);
    var difference = original.difference(other);
    var symDiff = original.symmetricDifference(other);
    
    // All results should be new Set instances
    assertTrue(intersection instanceof Set);
    assertTrue(union instanceof Set);
    assertTrue(difference instanceof Set);
    assertTrue(symDiff instanceof Set);
    
    // None should be the same object as original
    assertFalse(intersection === original);
    assertFalse(union === original);
    assertFalse(difference === original);
    assertFalse(symDiff === original);
    assertFalse(intersection === other);
    assertFalse(union === other);
    assertFalse(difference === other);
    assertFalse(symDiff === other);
})();

(function TestSetMethodsBooleanReturnValues() {
    var set1 = new Set([1, 2]);
    var set2 = new Set([1, 2, 3]);
    var set3 = new Set([4, 5]);
    
    // Boolean methods should return actual booleans
    assertEquals('boolean', typeof set1.isSubsetOf(set2));
    assertEquals('boolean', typeof set2.isSupersetOf(set1));
    assertEquals('boolean', typeof set1.isDisjointFrom(set3));
    
    // Check specific values
    assertTrue(set1.isSubsetOf(set2) === true);
    assertTrue(set1.isSubsetOf(set3) === false);
    assertTrue(set2.isSupersetOf(set1) === true);
    assertTrue(set1.isSupersetOf(set2) === false);
    assertTrue(set1.isDisjointFrom(set3) === true);
    assertTrue(set1.isDisjointFrom(set2) === false);
})();

(function TestSetMethodsWithMixedTypes() {
    var mixedSet = new Set([1, '1', true, false, null, undefined, {}, []]);
    var numberSet = new Set([1, 2, 3]);
    var stringSet = new Set(['1', '2', '3']);
    
    var numIntersection = mixedSet.intersection(numberSet);
    var strIntersection = mixedSet.intersection(stringSet);
    
    // Should only match exact type equality
    assertEquals(1, numIntersection.size);
    assertTrue(numIntersection.has(1));
    assertFalse(numIntersection.has('1')); // String '1' != number 1
    
    assertEquals(1, strIntersection.size);
    assertTrue(strIntersection.has('1'));
    assertFalse(strIntersection.has(1)); // Number 1 != string '1'
})();

print("Set methods integration tests completed successfully");