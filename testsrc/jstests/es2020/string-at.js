// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

const s = "foobar";

//basic numeric arguments 
assertEquals("f", s.at(0));
assertEquals("r", s.at(-1));
assertEquals("r", s.at(5));

//it treats missing arg like 0
assertEquals("f", s.at());

//it treats certain non numeric arguments like zero 
assertEquals("f", s.at(undefined));
assertEquals("f", s.at(null));
assertEquals("f", s.at("f"))
assertEquals("f", s.at(""))
assertEquals("f", s.at({}))
assertEquals("f",s.at(function() {}));
assertEquals("f",s.at(false));

//it treats other non numeric arguments like one
assertEquals("o", s.at("1"))
assertEquals("o", s.at(true))

//it returns undefined for out of range
assertEquals(undefined, s.at(Infinity));
assertEquals(undefined, s.at(-Infinity));
assertEquals(undefined, s.at(100));
assertEquals(undefined, s.at(-50));

//it throws with Symbol()
assertThrows(function() { s.at(Symbol());
}, TypeError);

//it throws when called on null or undefined
assertThrows(function() { String.prototype.at.call(null); }, TypeError);
assertThrows(function() { String.prototype.at.call(undefined); }, TypeError);

//it performs as in V8 when called on other non-strings 
assertEquals(',',String.prototype.at.call(['a','b'],1));
assertEquals('[',String.prototype.at.call({'foo':'bar'}, 0));
assertEquals('2', String.prototype.at.call(12,1));

"success"