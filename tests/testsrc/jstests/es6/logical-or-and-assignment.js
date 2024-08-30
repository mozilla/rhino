/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load('testsrc/assert.js');

(function basicTests() {
	var a = false;
	var b = true;
	a ||= b;
	assertTrue(a);
	
	var c = false;
	a &&= c;
	assertFalse(a);
})();

(function shortCircuits() {
	var counter = 0;
	function incCounter() { ++ counter; return true; }
	
	var a = true;
	counter = 0;
	a ||= incCounter();
	assertEquals(0, counter);

	a = false;
	counter = 0;
	a ||= incCounter();
	assertEquals(1, counter);

	a = true;
	counter = 0;
	a &&= incCounter();
	assertEquals(1, counter);

	a = false;
	counter = 0;
	a &&= incCounter();
	assertEquals(0, counter);
})();

"success";
