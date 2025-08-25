load("testsrc/assert.js");

(function testRedefineUndefinedInAFunction() {
    var undefined = 10;
    assertEquals(10, undefined);

    (function () {
        assertEquals(10, undefined);

        (function () {
            assertEquals(10, undefined);
        })();
    })();

    assertEquals(false, delete undefined);

    var x = undefined;
    assertEquals(10, x);
})();

(function undefinedAsArgumentName() {
    function f(undefined) {
        return undefined;
    }

    assertEquals(12, f(12));
})();

(function undefinedInWith() {
    var o = {undefined: 42};
    with (o) {
        assertEquals(42, undefined);
    }
})();

(function undefinedAsCatchExpression() {
    try {
        throw "a";
    } catch (undefined) {
        assertEquals("a", undefined);
    }
})();

(function undefinedAsFunctionName() {
    function undefined() {
        return 10;
    }

    assertEquals(10, undefined());
})();

(function undefinedViaDefinePropertyIsIgnored() {
    Object.defineProperty(this, "undefined", { value: 10 });
    assertEquals(undefined, undefined);
})();

(function everythingStillWorks() {
    assertEquals(typeof undefined, "undefined");
    assertEquals(undefined, void 0);
})();

"success";
