var assert = require("assert");

// This should be an empty object
assert.strictEqual(typeof this, 'object');
assert.strictEqual(Object.keys(this).length, 0);

// This should not be the globalThis
assert.notEqual(this, globalThis);

// This should not be the scope
assert.strictEqual(this.module, undefined);
assert.notEqual(module, undefined);

// This should be exports
assert.strictEqual(this, exports);
assert.strictEqual(this, module.exports);
assert.strictEqual(exports, module.exports);

// Modifying exports
exports.foo = 2;
assert.strictEqual(this, exports);
assert.strictEqual(this, module.exports);
assert.strictEqual(exports, module.exports);

// Reassigning exports
module.exports = {foo: 'bar'};
assert.strictEqual(this, exports); // Still true
assert.notEqual(this, module.exports); // No longer true
assert.notEqual(exports, module.exports); // No longer true
