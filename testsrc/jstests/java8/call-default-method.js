load("testsrc/assert.js");

function startsWith(str, prefix) {
  return str.lastIndexOf(prefix, prefix.length) === 0;
}

if (startsWith(''+java.util.stream.Stream, '[JavaClass')) {
  var p = new java.util.function.Predicate({
    test: function(t) {
      return true;
    }
  });
  assertInstanceof(p.negate(), java.util.function.Predicate);
  assertFalse(p.negate().test(0));
  assertInstanceof(p.isEqual(null), java.util.function.Predicate);
  assertTrue(p.isEqual(null).test(null));
}

"success";
