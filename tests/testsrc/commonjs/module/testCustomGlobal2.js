var assert = require("assert");

assert.strictEqual(test(4), 5);

var testWrap = function(arg) { return test(arg); };

assert.strictEqual(testWrap(5), 6);

var testWrap2 = function() { return function(arg) { return test(arg); }; };

assert.strictEqual(testWrap2()(6), 7);

var throws = false;
try {
    test.call("hmm", "oops");
} catch (e) {
    throws = true;
    assert.strictEqual("Method \"test\" called on incompatible object.", e.message);
}
if (!throws) {
    assert.fail("test() did not throw error when called on string");
}
