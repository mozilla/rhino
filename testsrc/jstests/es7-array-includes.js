load("testsrc/assert.js");

assertEquals(2, [1, 2, 3, 4].filter(t => [1, 2].includes(t)).length);

"success";