// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

//
// Test The length of 'Array.prototype.findLast' is 1
// (22.1.3.8)
(function () {
    assertEquals(1, Array.prototype.findLast.length);
})();

//
// Quick check for base cases
//
(function () {
    var a = [21, 22, 23, 24];

    // well, it works
    assertEquals(a[3], a.findLast(function () { return true; }));

    // predicate is called with current value, index and object on which `findLast()` was called
    assertEquals(a[3], a.findLast(function (val, i, array) { return array === a && i === 3; }));

    // 'this' can be augmented by second optional parameter
    var thisArg = {};
    assertEquals(a[3], a.findLast(function () { return this === thisArg; }, thisArg));

    // when nothing found, `undefined` is returned
    assertEquals(undefined, a.findLast(function () { return false; }));

    // it is not required to return Boolean, it will be automatically casted
    assertEquals(a[2], a.findLast(function (val) { return (val === a[2] ? "true" : null); }));
})();

//
// Test predicate is anything that has [[Call]] internal method
//
(function () {
    var a = [21, 22, 23, 24];

    // `InterpretedFunction` or `? extends NativeFunction`
    assertEquals(a[3], a.findLast(function () { return true; }));
    // `IdScriptableObject`
    assertEquals(a[3], a.findLast(Object.prototype.toString));
    assertEquals(a[3], a.findLast(String));
    // `BoundFunction`
    assertEquals(a[3], a.findLast((function () { return true; }).bind({})))
})();

//
// Test predicate is not called when array is empty
//
(function () {
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;

    [].findLast(function (val, key, obj) {
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

    var found = a.findLast(function (val, key, obj) {
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
    assertEquals(undefined, found);
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

    a.findLast(predicate);
    assertEquals(a.length, l);
    assertFalse(sawUndefined);

    // even for sparse arrays
    a = new Array(10);
    l = 0;
    a.findLast(predicate);
    assertEquals(a.length, l);
    assertTrue(sawUndefined);

    a = [];
    a[10] = 1;
    l = 0;
    sawUndefined = false;
    a.findLast(predicate);
    assertEquals(a.length, l);
    assertTrue(sawUndefined);
})();


//
// Test Array.prototype.findLast is generic and works with String
//
(function () {
    var a = "abcd";
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;
    var found = Array.prototype.findLast.call(a, function (val, key, obj) {
        o = obj.toString();
        l = obj.length;
        v = val;
        k = key;

        return false;
    });

    assertEquals(a, o);
    assertEquals(a.length, l);
    assertEquals("a", v);
    assertEquals(0, k);
    assertEquals(undefined, found);

    found = Array.prototype.findLast.apply(a, [function (val, key, obj) {
        o = obj.toString();
        l = obj.length;
        v = val;
        k = key;

        return true;
    }]);

    assertEquals(a, o);
    assertEquals(a.length, l);
    assertEquals("d", v);
    assertEquals(3, k);
    assertEquals("d", found);
})();

//
// Test Array.prototype.findLast works with simple arraylike objects
//
(function () {
    var o = {0: 0, 1: 1, 2: 2, length: 3};
    assertEquals(o[2], Array.prototype.findLast.call(o, function (v) { return v == o[2]; }));
    assertEquals(o[1], Array.prototype.findLast.apply(o, [function (v) { return v == o[1]; }]));

    // object without `length` property defined,
    // behaves as if it has `length` set 0
    var empty = {0: 0, 1: 1, 2: 2};
    var called = false;
    Array.prototype.findLast.call(empty, function () { called = true; });
    assertEquals(false, called);
})();

//
// Test Array.prototype.findLast works with mixed arraylike objects
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
    var found = Array.prototype.findLast.call(a, function (val, key, obj) {
        o = obj;
        l = obj.length;
        v = val;
        k = key;

        return !obj.isValid();
    });

    assertArrayEquals(a, o);
    assertEquals(3, l);
    assertEquals(30, v);
    assertEquals(0, k);
    assertEquals(undefined, found);
})();

//
// Test Array.prototype.findLast works with arraylike object with getters
//
(function () {
    var count = 0;
    var a = {get 0() { return count++; }, length: 1};
    // FIXME: right now, Rhino will get raw getter function
    //        during iteration and use it as a value passed to predicate
    //Array.prototype.findLast.call(a, (function () { return true; }));
    //assertEquals(1, count);
})();

//
// Test Array.prototype.findLast iteration includes inherited properties
//
(function () {
    var o1 = {0: 0, 1: 1};
    var o2 = {2: 2, length: 3};
    // FIXME: use Object.setPrototypeOf instead
    o2.__proto__ = o1;
    var a = [];
    Array.prototype.findLast.call(o2, function (v) { a.push(v); });
    assertEquals([2, 1, 0], a);
    assertEquals(2, Array.prototype.findLast.call(o2, function () { return true; }));
})();

//
// Test array modifications
//
(function () {
    var a = [1, 2, 3];
    var found = a.findLast(function (val) {
        a.push(val);
        return false;
    });
    assertArrayEquals([1, 2, 3, 3, 2, 1], a);
    assertEquals(6, a.length);
    assertEquals(undefined, found);

    a = [1, 2, 3];
    found = a.findLast(function (val, key) {
        a[key] = ++val;
        return false;
    });
    assertArrayEquals([2, 3, 4], a);
    assertEquals(3, a.length);
    assertEquals(undefined, found);
})();

//
// Test thisArg
//
(function () {
    // If thisArg is not provided, predicate is invoked with this set to `undefined`
    // FIXME:
    // var o = -1;
    //[1,2].findLast(function () { o = this; });
    //assertEquals(undefined, o);

    // Test String as a thisArg
    var found = [1, 2, 3].findLast(function (val, key) {
        return this.charAt(Number(key)) === String(val);
    }, "321");
    assertEquals(2, found);

    // Test object as a thisArg
    var thisArg = {
        elementAt: function (key) {
            return this[key];
        }
    };
    Array.prototype.push.apply(thisArg, ["c", "b", "a"]);

    found = ["a", "b", "c"].findLast(function (val, key) {
        return this.elementAt(key) === val;
    }, thisArg);
    assertEquals("b", found);

    // Test array itself as thisArg
    var o;
    var a = [1, 2];
    a.findLast(function () { o = this; }, a);
    assertEquals(a, o);
})();

// Test exceptions
assertThrows('Array.prototype.findLast.call(null, function() { })', TypeError);
assertThrows('Array.prototype.findLast.call(undefined, function() { })', TypeError);
assertThrows('Array.prototype.findLast.apply(null, function() { }, [])', TypeError);
assertThrows('Array.prototype.findLast.apply(undefined, function() { }, [])', TypeError);

assertThrows('[].findLast(null)', TypeError);
assertThrows('[].findLast(undefined)', TypeError);
assertThrows('[].findLast(0)', TypeError);
assertThrows('[].findLast(true)', TypeError);
assertThrows('[].findLast(false)', TypeError);
assertThrows('[].findLast("")', TypeError);
assertThrows('[].findLast({})', TypeError);
assertThrows('[].findLast([])', TypeError);
assertThrows('[].findLast(/\d+/)', TypeError);

assertThrows('Array.prototype.findLast.call({}, null)', TypeError);
assertThrows('Array.prototype.findLast.call({}, undefined)', TypeError);
assertThrows('Array.prototype.findLast.call({}, 0)', TypeError);
assertThrows('Array.prototype.findLast.call({}, true)', TypeError);
assertThrows('Array.prototype.findLast.call({}, false)', TypeError);
assertThrows('Array.prototype.findLast.call({}, "")', TypeError);
assertThrows('Array.prototype.findLast.call({}, {})', TypeError);
assertThrows('Array.prototype.findLast.call({}, [])', TypeError);
assertThrows('Array.prototype.findLast.call({}, /\d+/)', TypeError);

assertThrows('Array.prototype.findLast.apply({}, null, [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, undefined, [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, 0, [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, true, [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, false, [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, "", [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, {}, [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, [], [])', TypeError);
assertThrows('Array.prototype.findLast.apply({}, /\d+/, [])', TypeError);

"success";
