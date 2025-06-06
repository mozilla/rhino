load("testsrc/assert.js");

(function () {
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

function f(undefined) {
    return undefined;
}

assertEquals(12, f(12));

var o = { undefined: 42 };
with (o) {
    assertEquals(42, undefined);
}

try {
    throw "a";
} catch (undefined) {
    assertEquals("a", undefined);
}

"success";
