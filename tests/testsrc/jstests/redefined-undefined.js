load("testsrc/assert.js");

(function () {
    var undefined = 10;
    assertEquals(10, undefined);

    (function () {
        assertEquals(10, undefined);

        (function () {
            assertEquals(10, undefined);
        })();

        (() => {
            assertEquals(10, undefined);
        })();
    })();

    (() => {
        assertEquals(10, undefined);

        (function () {
            assertEquals(10, undefined);
        })();

        (() => {
            assertEquals(10, undefined);
        })();
    })();

    assertEquals(false, delete undefined);

    var x = undefined;
    assertEquals(10, x);
})();

(() => {
    var undefined = 10;
    assertEquals(10, undefined);

    (function () {
        assertEquals(10, undefined);

        (function () {
            assertEquals(10, undefined);
        })();

        (() => {
            assertEquals(10, undefined);
        })();
    })();

    (() => {
        assertEquals(10, undefined);

        (function () {
            assertEquals(10, undefined);
        })();

        (() => {
            assertEquals(10, undefined);
        })();
    })();

    assertEquals(false, delete undefined);

    var x = undefined;
    assertEquals(10, x);
})();

"success";
