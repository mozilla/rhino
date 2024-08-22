load("testsrc/assert.js");

assertThrows("function() { 'use strict'; 01; }", SyntaxError);
assertThrows("function() { 'use strict'; ({ 01: 1 }); }", SyntaxError);
assertEquals(8, (function() { return 010; })());
assertEquals(1, (function() { return ({ 010: 1 })[8]; })());

"success";
