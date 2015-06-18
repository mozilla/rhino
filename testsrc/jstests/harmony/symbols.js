load("testsrc/assert.js");

assertTrue(Symbol.iterator !== undefined);
let desc = Object.getOwnPropertyDescriptor(Symbol, "iterator");
assertFalse(desc.writable);
assertFalse(desc.configurable);
assertFalse(desc.enumerable);

assertTrue(Symbol.toStringTag !== undefined);
desc = Object.getOwnPropertyDescriptor(Symbol, "toStringTag");
assertFalse(desc.writable);
assertFalse(desc.configurable);
assertFalse(desc.enumerable);

assertTrue(Symbol.species !== undefined);
desc = Object.getOwnPropertyDescriptor(Symbol, "species");
assertFalse(desc.writable);
assertFalse(desc.configurable);
assertFalse(desc.enumerable);

"success";
