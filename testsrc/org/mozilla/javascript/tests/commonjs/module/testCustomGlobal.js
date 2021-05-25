var assert = require("assert");

assert.strictEqual(test(1), 2);

var testWrap = function(arg) { return test(arg); };

assert.strictEqual(testWrap(2), 3);

var testWrap2 = function() { return function(arg) { return test(arg); }; };

assert.strictEqual(testWrap2()(3), 4);

require('testCustomGlobal2');
