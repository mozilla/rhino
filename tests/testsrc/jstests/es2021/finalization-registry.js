'use strict';

load('testsrc/assert.js');

// Test the FinalizationRegistry. Since garbage collection is non-deterministic,
// these tests will not fail if the garbage collector never runs. However,
// we try to encourage it to run, and the callbacks are there so that we can
// exercise all the cleanup logic.

const pending = new Set();
const m = new Map();
const reg = new FinalizationRegistry((n) => { 
    // Can't assert here because errors are swallowed deliberately
    if (!pending.delete(n)) {
        console.error(`Did not expect finalization of ${n}`);
    }
    console.log(`Finalized ${n}, ${pending.size} left`);
    // Do something that will result in a microtask being enqueued
    // while we're in the microtask queue
    new Promise((resolve) => {
        resolve();
    }).then(() => {
        console.log(`${n} finalized`);
    });
});
const fooUnregister = {};
const barUnregister = {};

// Do some operations with promises so that microtasks run.
// This way the reference queue will actually be cleared.
const p = new Promise((resolve) => {
    // Create an object that we can GC later
    m.set('foo', {name: 'foo'});
    m.set('bar', {name: 'bar'});
    // Put it in the finalization registry
    reg.register(m.get('foo'), 'foo', fooUnregister);
    reg.register(m.get('bar'), 'bar', barUnregister);
    resolve();
}).then(() => {
    // On second thought forget about bar
    assert(reg.unregister(barUnregister));
    // Drop the reference, making it eligible for GC,
    // and indicate that we're expecting it to be finalized
    pending.add('foo');
    m.delete('foo');
    m.delete('bar');
    gc();
}).then(() => {
    // One more gc for good measure since this is nondeterministic
    gc();
});

const thingUnregister = {};

// Do it again but unregister a group of things
const p1 = new Promise((resolve) => {
    m.set('thing1', {id: 1});
    m.set('thing2', {id: 2});
    m.set('thing3', {id: 3});
    reg.register(m.get('thing1'), 1, thingUnregister);
    reg.register(m.get('thing2'), 2);
    reg.register(m.get('thing3'), 3, thingUnregister);
    resolve();
}).then(() => {
    assert(reg.unregister(thingUnregister));
    pending.add(2);
    m.delete('thing1');
    m.delete('thing2');
    m.delete('thing3');
    gc();
}).then(() => {
    gc();
});

// Set up an invalid cleanup callback.
const reg2 = new FinalizationRegistry((v) => {
    throw 'Deliberate error raised by FinalizationRegistry test';
});
const p2 = new Promise((resolve) => {
    m.set('error1', {error: true});
    reg2.register(m.get('error1'), 'error');
    resolve();
}).then(() => {
    m.delete('error1');
    gc();
}).then(() => {
    gc();
});

Promise.all([p, p1, p2]).then(() => {
    console.log('All tests completed');
});

'success';
