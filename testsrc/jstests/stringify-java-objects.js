load('testsrc/assert.js');

function replacer(key, value) {
    var javatype = value instanceof java.lang.Object ? value.getClass().name : null;
    return javatype ? 'replaced: ' + javatype : value;
}

// java.lang.String
var javaString = new java.lang.String('test');

var obj = {test: javaString};
var expected = JSON.stringify({test: 'replaced: java.lang.String'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = {test: javaString};
var expected = JSON.stringify({test: 'test'});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// java.lang.Double
var javaDouble = java.lang.Double.valueOf(12.34);

var obj = {test: javaDouble};
var expected = JSON.stringify({test: 'replaced: java.lang.Double'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = {test: javaDouble};
var expected = JSON.stringify({test: 12.34});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// java.lang.Boolean
var javaBoolean = java.lang.Boolean.valueOf(false);

var obj = {test: javaBoolean};
var expected = JSON.stringify({test: 'replaced: java.lang.Boolean'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = {test: javaBoolean};
var expected = JSON.stringify({test: false});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// java.util.Collection
var javaCollection = new java.util.LinkedHashSet();
    javaCollection.add('test');
    javaCollection.add({nested: 'jsObj'});

var obj = {test: javaCollection};
var expected = JSON.stringify({test: 'replaced: java.util.LinkedHashSet'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = {test: javaCollection};
var expected = JSON.stringify({test: ['test', {nested: 'jsObj'}]});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// java Array
var javaArray = new java.lang.String('a,b,c').split(',');

var obj = {test: javaArray};
var expected = JSON.stringify({test: 'replaced: [Ljava.lang.String;'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = {test: javaArray};
var expected = JSON.stringify({test: ['a','b','c']});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// java Map
var javaMap = new java.util.HashMap();
    javaMap.put(new java.lang.Object(), 'property skipped if key is not string-like');
    javaMap.put('te' + 'st', 55);

var obj = {test: javaMap};
var expected = JSON.stringify({test: 'replaced: java.util.HashMap'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = javaMap;
var expected = JSON.stringify({test: 55});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// complex object
var obj = {
    array: javaArray,
    boxed: [javaDouble, javaBoolean],
    objects: {
        plainJS: {test: 1},
        emptyMap: java.util.Collections.EMPTY_MAP,
        otherMap: javaMap
    }
};
var expected = JSON.stringify({
    array: ['a','b','c'],
    boxed: [12.34, false],
    objects: {
        plainJS: {test: 1},
        emptyMap: {},
        otherMap: {test: 55}
    }
});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// other Java object
var javaObject = new java.net.URI('test://other/java/object');

var obj = {test: javaObject};
var expected = JSON.stringify({test: 'replaced: java.net.URI'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = {test: javaObject};
assertThrows(()=>JSON.stringify(obj), TypeError);

// JavaAdapter with toJSON
var javaObject = new JavaAdapter(java.lang.Object, {
    toJSON: _ => ({javaAdapter: true})
});

var obj = javaObject;
var expected = JSON.stringify({javaAdapter: true});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

// JavaAdapter without toJSON
var javaObject = new JavaAdapter(java.lang.Object, {
    toString: () => 'just an object'
});

var obj = {test: javaObject};
var expected = /^replaced: adapter\d+$/;
var actual = JSON.parse(JSON.stringify(obj, replacer)).test;
assertEquals("string", typeof actual);
assertTrue(expected.test(actual));

var obj = {test: javaObject};
assertThrows(()=>JSON.stringify(obj), TypeError);

// nested Maps and Lists
var map1 = new java.util.HashMap({a:1});
var map2 = new java.util.HashMap({b:2, map1: map1});

var list1 = new java.util.ArrayList([1]);
var list2 = new java.util.ArrayList([2, list1]);

var expected = JSON.stringify({
    b: 2,
    map1: {a: 1}
});
var actual = JSON.stringify(map2);
assertEquals(expected, actual);

var expected = JSON.stringify([2, [1]]);
var actual = JSON.stringify(list2);
assertEquals(expected, actual);

list2.add(map1);
var expected = JSON.stringify([2, [1], {a:1}]);
var actual = JSON.stringify(list2);
assertEquals(expected, actual);

"success"
