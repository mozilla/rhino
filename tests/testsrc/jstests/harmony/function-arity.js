// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

// Check correctness of function properties length and arity
// NOTE: arity is non-standard

// arity is the same for arrow functions and regular anonymous functions
assertEquals(((a,b) => {}).arity, (function (a,b) {}).arity);
assertEquals((() => {}).arity, (function () {}).arity);

// length is the same for arrow functions and regular anonymous functions
assertEquals(((a,b) => {}).length, (function (a,b) {}).length);
assertEquals((() => {}).length, (function () {}).length);

// arity and length are the same for a given arrow function
assertEquals(((a,b) => {}).arity, ((a,b) => {}).length);
assertEquals((() => {}).arity, (() => {}).length);

// length is right
assertEquals(((a,b) => {}).length, 2);
assertEquals((() => {}).length, 0);

"success";
