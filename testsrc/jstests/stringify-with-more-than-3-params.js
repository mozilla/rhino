/*---
JSON.stringify() with more than three parameters should respect the first three parameters
---*/

load("testsrc/assert.js");

assertEquals("[\n-\"a\"\n]" , JSON.stringify(['a'], ['a'], '-', undefined));
assertEquals("[\n-\"a\"\n]" , JSON.stringify(['a'], ['a'], '-', {}));

"success"
