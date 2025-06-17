var assert = require("assert");

// This should be an empty object
assert.strictEqual(typeof this, 'object');
assert.strictEqual(Object.keys(this).length, 0);

// This should not be the globalThis
assert.notEqual(this, globalThis);

// This should not be the scope
assert.strictEqual(this.exports, undefined);
assert.notEqual(exports, undefined);
