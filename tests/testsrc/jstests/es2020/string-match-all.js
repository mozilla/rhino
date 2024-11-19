// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

function assertIteratorIsDone(iterator) {
	const next = iterator.next();
	assertTrue(next.done);
	assertEquals(undefined, next.value);
}

(function happyPath() {
	const s = "aabbc";
	const re = /([a-z])\1/g;
	const matches = s.matchAll(re);

	var match = matches.next();
	assertFalse(match.done);
	assertEquals("aa", match.value[0]);
	assertEquals(0, match.value.index);
	assertEquals(s, match.value.input);

	match = matches.next();
	assertFalse(match.done);
	assertEquals("bb", match.value[0]);
	assertEquals(2, match.value.index);
	assertEquals(s, match.value.input);

	assertIteratorIsDone(matches);
})();

(function matchingEmptyStringDoesNotResultInInfiniteLoop() {
	const s = "abc";
	const re = /(?=[a-b])/g;
	const matches = s.matchAll(re);

	var match = matches.next();
	assertFalse(match.done);
	assertEquals("", match.value[0]);
	assertEquals(0, match.value.index);
	assertEquals(s, match.value.input);

	match = matches.next();
	assertFalse(match.done);
	assertEquals("", match.value[0]);
	assertEquals(1, match.value.index);
	assertEquals(s, match.value.input);

	assertIteratorIsDone(matches);
})();

(function stringMatchAllCalledWithNonGlobalRegExp() {
	const s = "aabbc";
	const re = /([a-z])\1/;
	try {
		s.matchAll(re);
		throw new Error('Expected a TypeError because regex is not global');
	} catch (err) {
		assertEquals('TypeError', err.name);
		assertEquals('String.prototype.matchAll called with a non-global RegExp argument', err.message);
	}
})();

(function regexpSymbolMatchAllGlobal() {
	const s = "aabbc";
	const re = /([a-z])\1/g;
	const matches = re[Symbol.matchAll](s);

	var match = matches.next();
	assertFalse(match.done);
	assertEquals("aa", match.value[0]);
	assertEquals(0, match.value.index);
	assertEquals(s, match.value.input);

	match = matches.next();
	assertFalse(match.done);
	assertEquals("bb", match.value[0]);
	assertEquals(2, match.value.index);
	assertEquals(s, match.value.input);

	assertIteratorIsDone(matches);
})();

(function regexpSymbolMatchAllNonGlobal() {
	const s = "aabbc";
	const re = /([a-z])\1/;
	const matches = re[Symbol.matchAll](s);

	var match = matches.next();
	assertFalse(match.done);
	assertEquals("aa", match.value[0]);
	assertEquals(0, match.value.index);
	assertEquals(s, match.value.input);

	// No second match, already done	
	assertIteratorIsDone(matches);
})();

(function symbolMatchAllIsNotCallable() {
	const s = "aabbc";
	const re = {
		[Symbol.matchAll]: 42
	};
	try {
		s.matchAll(re);
		throw new Error('Expected a TypeError because the value of Symbol.matchAll is not a function');
	} catch (err) {
		assertEquals('TypeError', err.name);
		assertEquals('Cannot call property Symbol.matchAll in object [object Object]. It is not a function, it is "number".', err.message);
	}
})();

(function customSymbolMatchAllImplementation() {
	const s = "2016-01-02|2019-03-07";

	const numbers = {
		[Symbol.matchAll]: function*(str) {
			for (var n of str.matchAll(/[0-9]+/g)) {
				yield n[0];
			}
		},
	};

	const results = Array.from(s.matchAll(numbers));
	assertEquals(6, results.length);
	assertEquals("2016", results[0]);
	assertEquals("01", results[1]);
	assertEquals("02", results[2]);
	assertEquals("2019", results[3]);
	assertEquals("03", results[4]);
	assertEquals("07", results[5]);
})();

(function nullRegExpShouldMatchLiteralNullString() {
	const s = "to null or not to null";
	const results = Array.from(s.matchAll(null));
	assertEquals(2, results.length);
	assertEquals("null", results[0][0]);
	assertEquals(3, results[0].index);
	assertEquals("null", results[1][0]);
	assertEquals(18, results[1].index);
})();

'success';
