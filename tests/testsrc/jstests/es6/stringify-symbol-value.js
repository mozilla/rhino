load('testsrc/assert.js');

// check for symbol after the replacer was called

var count = 0;
var sym = Symbol('testSymbol');

function replacerSym(key, value) {
    count++;
    if ('test' == key) return sym;
    return value;
}

function replacerStr(key, value) {
    count++;
    if ('test' == key) return 'newStr';
    return value;
}

var obj = {test: 'value'};
var actual = JSON.stringify(obj, replacerSym);
assertEquals("{}", actual);
assertEquals(2, count);

count = 0;
var obj = {test: sym};
var actual = JSON.stringify(obj, replacerStr);
assertEquals('{"test":"newStr"}', actual);
assertEquals(2, count);


"success"