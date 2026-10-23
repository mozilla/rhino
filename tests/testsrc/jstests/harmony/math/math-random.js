load("testsrc/assert.js");

for (var i = 0; i < 1000; i++) {
  var r = Math.random();
  assertEquals(typeof r, "number");
  assertTrue(r >= 0 && r < 1);
}

"success";
