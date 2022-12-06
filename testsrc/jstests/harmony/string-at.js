load("testsrc/assert.js");
const s = "foobar";
assertEquals("r", s.at(-1));
assertEquals("r", s.at(5));
assertEquals("f", s.at(undefined));
assertEquals(undefined, s.at(Infinity));
assertEquals("f", s.at("f"))
assertEquals("f", s.at({}))
"success"