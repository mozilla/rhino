// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

//
// Test The length of 'Array.prototype.findIndex' is 1
// (22.1.3.9)
(function () {
    assertEquals(1, Array.prototype.findIndex.length);
})();

//
// Quick check for base cases
//
(function () {
    var a = [21, 22, 23, 24];

    // well, it works
    assertEquals(0, a.findIndex(function () { return true; }));

    // predicate is called with current value, index and object on which `findIndex()` was called
    assertEquals(3, a.findIndex(function (val, i, array) { return array === a && i === 3; }));

    // 'this' can be augmented by second optional parameter
    var thisArg = {};
    assertEquals(0, a.findIndex(function () { return this === thisArg; }, thisArg));

    // when nothing found, `-1` is returned
    assertEquals(-1, a.findIndex(function () { return false; }));

    // it is not required to return Boolean, it will be automatically casted
    assertEquals(2, a.findIndex(function (val) { return (val === a[2] ? "true" : null); }));
})();

//
// Test predicate is anything that has [[Call]] internal method
//
(function () {
    var a = [21, 22, 23, 24];

    // `InterpretedFunction` or `? extends NativeFunction`
    assertEquals(0, a.findIndex(function () { return true; }));
    // `IdScriptableObject`
    assertEquals(0, a.findIndex(Object.prototype.toString));
    assertEquals(0, a.findIndex(String));
    // `BoundFunction`
    assertEquals(0, a.findIndex((function () { return true; }).bind({})))
})();

//
// Test predicate is not called when array is empty
//
(function () {
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;

    [].findIndex(function (val, key, obj) {
        o = obj;
        l = obj.length;
        v = val;
        k = key;

        return false;
    });

    assertEquals(-1, l);
    assertEquals(-1, o);
    assertEquals(-1, v);
    assertEquals(-1, k);
})();

//
// Test predicate is called with correct arguments
//
(function () {
    var a = ["b"];
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;

    var found = a.findIndex(function (val, key, obj) {
        o = obj;
        l = obj.length;
        v = val;
        k = key;

        return false;
    });

    assertArrayEquals(a, o);
    assertEquals(a.length, l);
    assertEquals("b", v);
    assertEquals(0, k);
    assertEquals(-1, found);
})();

//
// Test predicate is called array.length times
//
(function () {
    var a = [1, 2, 3, 4, 5];
    var l = 0;
    var sawUndefined = false;
    var predicate = function p(v) {
        l++;
        sawUndefined = sawUndefined || (v === undefined);
    };

    a.findIndex(predicate);
    assertEquals(a.length, l);
    assertFalse(sawUndefined);

    // even for sparse arrays
    a = new Array(10);
    l = 0;
    a.findIndex(predicate);
    assertEquals(a.length, l);
    assertTrue(sawUndefined);

    a = [];
    a[10] = 1;
    l = 0;
    sawUndefined = false;
    a.findIndex(predicate);
    assertEquals(a.length, l);
    assertTrue(sawUndefined);
})();


//
// Test Array.prototype.findIndex is generic and works with String
//
(function () {
    var a = "abcd";
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;
    var found = Array.prototype.findIndex.call(a, function (val, key, obj) {
        o = obj.toString();
        l = obj.length;
        v = val;
        k = key;

        return false;
    });

    assertEquals(a, o);
    assertEquals(a.length, l);
    assertEquals("d", v);
    assertEquals(3, k);
    assertEquals(-1, found);

    found = Array.prototype.findIndex.apply(a, [function (val, key, obj) {
        o = obj.toString();
        l = obj.length;
        v = val;
        k = key;

        return true;
    }]);

    assertEquals(a, o);
    assertEquals(a.length, l);
    assertEquals("a", v);
    assertEquals(0, k);
    assertEquals(0, found);
})();

//
// Test Array.prototype.findIndex works with simple arraylike objects
//
(function () {
    var o = {0: 0, 1: 1, 2: 2, length: 3};
    assertEquals(2, Array.prototype.findIndex.call(o, function (v) { return v == o[2]; }));
    assertEquals(1, Array.prototype.findIndex.apply(o, [function (v) { return v == o[1]; }]));

    // object without `length` property defined,
    // behaves as if it has `length` set 0
    var empty = {0: 0, 1: 1, 2: 2};
    var called = false;
    Array.prototype.findIndex.call(empty, function () { called = true; });
    assertEquals(false, called);
})();

//
// Test Array.prototype.findIndex works with mixed arraylike objects
//
(function () {
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;
    var a = {
        prop1: "val1",
        prop2: "val2",
        isValid: function () {
            return this.prop1 === "val1" && this.prop2 === "val2";
        },
        length: 0
    };

    Array.prototype.push.apply(a, [30, 31, 32]);
    var found = Array.prototype.findIndex.call(a, function (val, key, obj) {
        o = obj;
        l = obj.length;
        v = val;
        k = key;

        return !obj.isValid();
    });

    assertArrayEquals(a, o);
    assertEquals(3, l);
    assertEquals(32, v);
    assertEquals(2, k);
    assertEquals(-1, found);
})();

//
// Test Array.prototype.findIndex works with arraylike object with getters
//
(function () {
    var count = 0;
    var a = {get 0() { return count++; }, length: 1};
    // FIXME: right now, Rhino will get raw getter function
    //        during iteration and use it as a value passed to predicate
    //Array.prototype.findIndex.call(a, (function () { return true; }));
    //assertEquals(1, count);
})();

//
// Test Array.prototype.findIndex iteration includes inherited properties
//
(function () {
    var o1 = {0: 0, 1: 1};
    var o2 = {2: 2, length: 3};
    // FIXME: use Object.setPrototypeOf instead
    o2.__proto__ = o1;
    var a = [];
    Array.prototype.findIndex.call(o2, function (v) { a.push(v); });
    assertEquals([0, 1, 2], a);
    assertEquals(0, Array.prototype.findIndex.call(o2, function () { return true; }));
})();

//
// Test array modifications
//
(function () {
    var a = [1, 2, 3];
    var found = a.findIndex(function (val) {
        a.push(val);
        return false;
    });
    assertArrayEquals([1, 2, 3, 1, 2, 3], a);
    assertEquals(6, a.length);
    assertEquals(-1, found);

    a = [1, 2, 3];
    found = a.findIndex(function (val, key) {
        a[key] = ++val;
        return false;
    });
    assertArrayEquals([2, 3, 4], a);
    assertEquals(3, a.length);
    assertEquals(-1, found);
})();

//
// Test thisArg
//
(function () {
    // If thisArg is not provided, predicate is invoked with this set to `undefined`
    // FIXME:
    // var o = -1;
    //[1,2].findIndex(function () { o = this; });
    //assertEquals(undefined, o);

    // Test String as a thisArg
    var found = [1, 2, 3].findIndex(function (val, key) {
        return this.charAt(Number(key)) === String(val);
    }, "321");
    assertEquals(1, found);

    // Test object as a thisArg
    var thisArg = {
        elementAt: function (key) {
            return this[key];
        }
    };
    Array.prototype.push.apply(thisArg, ["c", "b", "a"]);

    found = ["a", "b", "c"].findIndex(function (val, key) {
        return this.elementAt(key) === val;
    }, thisArg);
    assertEquals(1, found);

    // Test array itself as thisArg
    var o;
    var a = [1, 2];
    a.findIndex(function () { o = this; }, a);
    assertEquals(a, o);
})();

// Test exceptions
assertThrows('Array.prototype.findIndex.call(null, function() { })', TypeError);
assertThrows('Array.prototype.findIndex.call(undefined, function() { })', TypeError);
assertThrows('Array.prototype.findIndex.apply(null, function() { }, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply(undefined, function() { }, [])', TypeError);

assertThrows('[].findIndex(null)', TypeError);
assertThrows('[].findIndex(undefined)', TypeError);
assertThrows('[].findIndex(0)', TypeError);
assertThrows('[].findIndex(true)', TypeError);
assertThrows('[].findIndex(false)', TypeError);
assertThrows('[].findIndex("")', TypeError);
assertThrows('[].findIndex({})', TypeError);
assertThrows('[].findIndex([])', TypeError);
assertThrows('[].findIndex(/\d+/)', TypeError);

assertThrows('Array.prototype.findIndex.call({}, null)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, undefined)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, 0)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, true)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, false)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, "")', TypeError);
assertThrows('Array.prototype.findIndex.call({}, {})', TypeError);
assertThrows('Array.prototype.findIndex.call({}, [])', TypeError);
assertThrows('Array.prototype.findIndex.call({}, /\d+/)', TypeError);

assertThrows('Array.prototype.findIndex.apply({}, null, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, undefined, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, 0, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, true, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, false, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, "", [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, {}, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, [], [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, /\d+/, [])', TypeError);

"success";
