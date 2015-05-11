try {
  org.mozilla.javascript.tests.Issue176Test.throwError("Aboo!");
} catch (e) { // EcmaError
  assertEquals(e.toString(), "Aboo!");
  // FIXME: Message, file and line
}

function MyBang(msg, file, line) {
  this.foo = 'Bar';
  this.msg = msg;
  this.file = file;
  this.line = line;
}

try {
  org.mozilla.javascript.tests.Issue176Test.throwCustomError("MyBang", "Aboo!");
} catch (e) { // MyBang
  assertEquals(e.foo, 'Bar');
  // FIXME: Message, file and line
}

// TBI: anba's test of not-overriding internal error types
