/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * host is instance of Issue176Test
 */

/*
 * Step 1: Basic built-in errors from java
 */
try {
  host.throwError("Aboo!"); // line 13
  throw 'Unreachable 1!';
} catch (e) { // EcmaError
  var act = e.toString();
  if (act !== 'Error: Aboo!') throw 'Test 1: Wrong toString(): ' + act;
  if (e.name !== 'Error') throw 'Test 2: Wrong name: ' + e.name;
  // FIXME: sourceName is undefined - why?
  // if (e.sourceName !== 'Issue176.js') throw 'Test 3: Wrong source: ' + e.sourceName;
  if (e.lineNumber !== 13) throw 'Test 4: Wrong source line: ' + e.lineNumber;
}


/*
 * Step 2: User-defined errors from java
 */
function MyBang(msg, file, line) {
  this.foo = 'Bar';
  this.msg = msg;
  this.file = file;
  this.line = line;
}

try {
  host.throwCustomError("MyBang", "Aboo!"); // line 36
  throw 'Unreachable 2!';
} catch (e) { // MyBang
  if (e.foo !== 'Bar') throw 'Test 5: Wrong foo: ' + e.foo + " (" + e + ")";
  if (e.msg !== 'Aboo!') throw 'Test 6: Wrong msg: ' + e.msg+ " (" + e + ")";
  if (e.file !== 'Issue176.js') throw 'Test 7: Wrong file: ' + e.file+ " (" + e + ")";
  if (e.line !== 36) throw 'Test 8: Wrong line: ' + e.line+ " (" + e + ")";
 }


/*
 * Step 3: Change a built-in error, but make sure it is not overwritten.
 * Thanks to anba for explaining the issue.
 */
TypeError = function() { this.msg = 'Wrong!' };

try { 
  Object.create(void 0);
  throw 'Unreachable 3!';
} catch (e) {
  // FIXME: TypeError is actually overwritten
  // if (e.msg === 'Wrong!') throw 'Test 9: TypeError can be overwritten ?! (' + e + ')'; 
} 


"success";
