/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function generatorAsShortHandMethod() {
	const o = {
		h() {},
		*g() {
			yield 1;
			yield 2;
		}
	};

	const iter = o.g();

	let next = iter.next();
	assertEquals(1, next.value);
	assertEquals(false, next.done);

	next = iter.next();
	assertEquals(2, next.value);
	assertEquals(false, next.done);

	next = iter.next();
	assertEquals(undefined, next.value);
	assertEquals(true, next.done);
})();

"success";
