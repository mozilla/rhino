var assert = require("assert");

assert.strictEqual(test(4), 5);

var testWrap = function(arg) { return test(arg); };

assert.strictEqual(testWrap(5), 6);

var testWrap2 = function() { return function(arg) { return test(arg); }; };

assert.strictEqual(testWrap2()(6), 7);
