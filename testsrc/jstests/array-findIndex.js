function assertSame(expected, found, name_opt) {
    if (found === expected) {
        if (expected !== 0 || (1 / expected) == (1 / found)) return;
    } else if ((expected !== expected) && (found !== found)) {
        return;
    }
    throw new Error('expected ' + expected + ' != ' + found);
}

function assertEquals(expected, found, name_opt) {
    assertSame(expected, found, name_opt);
}

function assertArrayEquals(expected, found, name_opt) {
    var start = "";
    if (name_opt) {
        start = name_opt + " - ";
    }
    assertSame(expected.length, found.length, start + "array length");
    if (expected.length == found.length) {
        for (var i = 0; i < expected.length; ++i) {
            assertSame(expected[i], found[i],
                    start + "array element at index " + i);
        }
    }
}

function assertInstanceof(obj, type) {
    if (!(obj instanceof type)) {
        var actualTypeName = null;
        var actualConstructor = Object.getPrototypeOf(obj).constructor;
        if (typeof actualConstructor == "function") {
            actualTypeName = actualConstructor.name || String(actualConstructor);
        }
        throw new Error("Object <" + obj + "> is not an instance of <" +
            (type.name || type) + ">" +
            (actualTypeName ? " but of < " + actualTypeName + ">" : ""));
    }
}

function assertThrows(code, type_opt, cause_opt) {
    var threwException = true;
    try {
        if (typeof code == 'function') {
            code();
        } else {
            eval(code);
        }
        threwException = false;
    } catch (e) {
        if (typeof type_opt == 'function') {
            assertInstanceof(e, type_opt);
        }
        if (arguments.length >= 3) {
            assertEquals(e.type, cause_opt);
        }
        // Success.
        return;
    }
    throw new Error("Did not throw exception");
}

assertEquals(1, Array.prototype.findIndex.length);

var a = [21, 22, 23, 24];
assertEquals(-1, a.findIndex(function() { return false; }));
assertEquals(-1, a.findIndex(function(val) { return 121 === val; }));
assertEquals(0, a.findIndex(function() { return true; }));
assertEquals(1, a.findIndex(function(val) { return 22 === val; }), undefined);
assertEquals(2, a.findIndex(function(val) { return 23 === val; }), null);
assertEquals(3, a.findIndex(function(val) { return 24 === val; }));


//
// Test predicate is not called when array is empty
//
(function() {
    var a = [];
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;

    a.findIndex(function(val, key, obj) {
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
// Test predicate is called with correct argumetns
//
(function() {
    var a = ["b"];
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;

    var index = a.findIndex(function(val, key, obj) {
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
    assertEquals(-1, index);
})();


//
// Test predicate is called array.length times
//
(function() {
    var a = [1, 2, 3, 4, 5];
    var l = 0;

    a.findIndex(function() {
        l++;
        return false;
    });

    assertEquals(a.length, l);
})();


//
// Test Array.prototype.findIndex works with String
//
(function() {
    var a = "abcd";
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;

    var index = Array.prototype.findIndex.call(a, function(val, key, obj) {
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
    assertEquals(-1, index);

    index = Array.prototype.findIndex.apply(a, [function(val, key, obj) {
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
    assertEquals(0, index);
})();


//
// Test Array.prototype.findIndex works with exotic object
//
(function() {
    var l = -1;
    var o = -1;
    var v = -1;
    var k = -1;
    var a = {
        prop1: "val1",
        prop2: "val2",
        isValid: function() {
            return this.prop1 === "val1" && this.prop2 === "val2";
        }
    };

    Array.prototype.push.apply(a, [30, 31, 32]);

    var index = Array.prototype.findIndex.call(a, function(val, key, obj) {
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
    assertEquals(-1, index);
})();


//
// Test array modifications
//
(function() {
    var a = [1, 2, 3];
    a.findIndex(function(val) { a.push(val); return false; });
    assertArrayEquals([1, 2, 3, 1, 2, 3], a);
    assertEquals(6, a.length);

    a = [1, 2, 3];
    a.findIndex(function(val, key) { a[key] = ++val; return false; });
    assertArrayEquals([2, 3, 4], a);
    assertEquals(3, a.length);
})();


//
// Test predicate is only called for existing elements
//
(function() {
    var a = new Array(30);
    a[11] = 21;
    a[7] = 10;
    a[29] = 31;

    var count = 0;
    a.findIndex(function() { count++; return false; });
    assertEquals(3, count);
})();


//
// Test thisArg
//
(function() {
//    Test String as a thisArg
    var index = [1, 2, 3].findIndex(function(val, key) {
        return this.charAt(Number(key)) === String(val);
    }, "321");
    assertEquals(1, index);

//    Test object as a thisArg
    var thisArg = {
        elementAt: function(key) {
            return this[key];
        }
    };
    Array.prototype.push.apply(thisArg, ["c", "b", "a"]);

    index = ["a", "b", "c"].findIndex(function(val, key) {
        return this.elementAt(key) === val;
    }, thisArg);
    assertEquals(1, index);
})();

// Test exceptions
//assertThrows('Array.prototype.findIndex.call(null, function() { })', TypeError);
//assertThrows('Array.prototype.findIndex.call(undefined, function() { })', TypeError);
//assertThrows('Array.prototype.findIndex.apply(null, function() { }, [])', TypeError);
//assertThrows('Array.prototype.findIndex.apply(undefined, function() { }, [])', TypeError);

assertThrows('[].findIndex(null)', TypeError);
assertThrows('[].findIndex(undefined)', TypeError);
assertThrows('[].findIndex(0)', TypeError);
assertThrows('[].findIndex(true)', TypeError);
assertThrows('[].findIndex(false)', TypeError);
assertThrows('[].findIndex("")', TypeError);
assertThrows('[].findIndex({})', TypeError);
assertThrows('[].findIndex([])', TypeError);
//assertThrows('[].findIndex(/\d+/)', TypeError);

assertThrows('Array.prototype.findIndex.call({}, null)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, undefined)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, 0)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, true)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, false)', TypeError);
assertThrows('Array.prototype.findIndex.call({}, "")', TypeError);
assertThrows('Array.prototype.findIndex.call({}, {})', TypeError);
assertThrows('Array.prototype.findIndex.call({}, [])', TypeError);
//assertThrows('Array.prototype.findIndex.call({}, /\d+/)', TypeError);

assertThrows('Array.prototype.findIndex.apply({}, null, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, undefined, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, 0, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, true, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, false, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, "", [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, {}, [])', TypeError);
assertThrows('Array.prototype.findIndex.apply({}, [], [])', TypeError);
//assertThrows('Array.prototype.findIndex.apply({}, /\d+/, [])', TypeError);

"success";