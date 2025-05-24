load('testsrc/assert.js');

/* Some tests from section 11.2.3-3 of the test262 suite. */

var fooCalled = false;
function foo(){ fooCalled = true; }
var o;

/* Case 1 */
fooCalled = false;
o = { };
assertThrows(function() {
  o.bar( foo() );
  throw "o.bar does not exist!";
}, TypeError);
// Called because bar is merely undefined
assertTrue(fooCalled);

/* Case 3 */
fooCalled = false;
o = { };
assertThrows(function() {
        o.bar.gar( foo() );
        throw "o.bar does not exist!";
}, TypeError);
// Not called because bar is not an object
assertFalse(fooCalled);

/* Extend case to a function object */
fooCalled = false;
assertThrows(function() {
  functionDoesNotExist(foo());
  throw "function does not exist";
}, ReferenceError);
// Not called because of the ReferenceError
assertFalse(fooCalled);

/* Extend case to a non-function object */
fooCalled = false;
notAFunction = 'Hello, World!';
assertThrows(function() {
  notAFunction(foo());
  throw "That wasn't a function";
}, TypeError);
// Called because it's merely not a function
assertTrue(fooCalled);

'success';
