function assertSame(expected, found, name_opt) {
  if (found === expected) {
    if (expected !== 0 || (1 / expected) == (1 / found)) return;
  } else if ((expected !== expected) && (found !== found)) {
    return;
  }
  throw new Error('expected ' + expected + ' != ' + found);
}

function assertEquals(expected, found, name_opt) {
  assertSame(expected, found, name_opt);
}

function assertArrayEquals(expected, found, name_opt) {
  var start = "";
  if (name_opt) {
    start = name_opt + " - ";
  }
  assertSame(expected.length, found.length, start + "array length");
  if (expected.length == found.length) {
    for (var i = 0; i < expected.length; ++i) {
      assertSame(expected[i], found[i],
          start + "array element at index " + i);
    }
  }
}

function assertInstanceof(obj, type) {
  if (!(obj instanceof type)) {
    var actualTypeName = null;
    var actualConstructor = Object.getPrototypeOf(obj).constructor;
    if (typeof actualConstructor == "function") {
      actualTypeName = actualConstructor.name || String(actualConstructor);
    }
    throw new Error("Object <" + obj + "> is not an instance of <" +
      (type.name || type) + ">" +
      (actualTypeName ? " but of < " + actualTypeName + ">" : ""));
  }
}

function assertThrows(code, type_opt, cause_opt) {
  var threwException = true;
  try {
    if (typeof code == 'function') {
      code();
    } else {
      eval(code);
    }
    threwException = false;
  } catch (e) {
    if (typeof type_opt == 'function') {
      assertInstanceof(e, type_opt);
    }
    if (arguments.length >= 3) {
      assertEquals(e.type, cause_opt);
    }
    // Success.
    return;
  }
  throw new Error("Did not throw exception");
}