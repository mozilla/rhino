var assert = require("assert");

assert.strictEqual(test(1), 2);

var testWrap = function(arg) { return test(arg); };

assert.strictEqual(testWrap(2), 3);

var testWrap2 = function() { return function(arg) { return test(arg); }; };

assert.strictEqual(testWrap2()(3), 4);

require('testCustomGlobal2');

var throws = false;
try {
    test.call({}, "oops");
} catch (e) {
    throws = true;
    assert.strictEqual("Method \"test\" called on incompatible object.", e.message);
}
if (!throws) {
    assert.fail("test() did not throw error when called on invalid object");
}
