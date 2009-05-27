function expectTypeError(code) {
  try {
    code();
    throw (code.toSource() + ' should have thrown a TypeError');
  } catch (e if e instanceof TypeError) {
    // all good
  }
}
