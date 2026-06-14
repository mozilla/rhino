'use strict';

load('testsrc/assert.js');

// Test the FinalizationRegistry. This is designed to test
// in the default mode where finalizations are never actually called.

const m = new Map();
const reg = new FinalizationRegistry((n) => {
    console.error(`Finalization called incorrectly for ${n} because finalization is disabled`); 
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
    m.delete('thing1');
    m.delete('thing2');
    m.delete('thing3');
    gc();
}).then(() => {
    gc();
});

Promise.all([p, p1]).then(() => {
    console.log('All tests completed');
});

'success';
