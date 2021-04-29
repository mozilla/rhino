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
var objectKey = new JavaAdapter(java.lang.Object, {toString:()=>"object key"});
var javaMap = new java.util.LinkedHashMap();
    javaMap.put(Symbol(), 'property skipped for Symbol keys');
    javaMap.put(objectKey, 'object value');
    javaMap.put('te' + 'st', 55);

var obj = {test: javaMap};
var expected = JSON.stringify({test: 'replaced: java.util.LinkedHashMap'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var obj = javaMap;
var expected = JSON.stringify({"object key": "object value", test: 55});
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
        otherMap: {"object key": "object value", test: 55}
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
var expected = JSON.stringify({test: 'test://other/java/object'});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// JavaAdapter with toJSON
var javaObject = new JavaAdapter(java.lang.Object, {
    toJSON: _ => ({javaAdapter: true}),
    toString: () => 'just an object'
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
var expected = JSON.stringify({test: "just an object"});
var actual = JSON.stringify(obj)
assertEquals(expected, actual);

// nested Maps and Lists
var map1 = new java.util.LinkedHashMap({a:1});
var map2 = new java.util.LinkedHashMap({b:2, map1: map1});

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

// make circular reference
list1.add(map2);
map1.put('list2', list2);
assertThrows(()=>JSON.stringify(map1), TypeError);

// primitive java arrays
var obj = new java.util.HashMap({bytes: new java.lang.String('abc').getBytes('UTF-8')});
var expected = JSON.stringify({bytes: [97, 98, 99]});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// alternative converters
var cx = org.mozilla.javascript.Context.getCurrentContext();
var obj = {
    uri: new java.net.URI('test://converter.test'),
    enum: java.time.DayOfWeek.FRIDAY,
    string: "plain string"
}
const {STRING, EMPTY_OBJECT, UNDEFINED, THROW_TYPE_ERROR, BEAN} =
    org.mozilla.javascript.JavaToJSONConverters;
cx.setJavaToJSONConverter(EMPTY_OBJECT);
var expected = JSON.stringify({uri: {}, enum: {}, string: "plain string"});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

cx.setJavaToJSONConverter(UNDEFINED);
var expected = JSON.stringify({string: "plain string"});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

cx.setJavaToJSONConverter(THROW_TYPE_ERROR);
assertThrows(()=>JSON.stringify(obj), TypeError);

// custom converter defined in javascript
obj.calendar = new java.util.GregorianCalendar(2021,3,25);
var converter = o => 
    o instanceof java.lang.Enum ? {enum: {type: o.getClass().getName(), name: o.name()}} :
    o instanceof java.util.Calendar ? o.toZonedDateTime().toLocalDate() :
    o.toString();

cx.setJavaToJSONConverter(converter);
var expected = JSON.stringify({
    uri: 'test://converter.test',
    enum: {enum: {type: "java.time.DayOfWeek", name: "FRIDAY"}},
    string: "plain string",
    calendar: "2021-04-25"
});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// JavaBean tester
cx.setJavaToJSONConverter(BEAN);
var obj = {a: new java.net.URI('test://bean/converter'), b: {}, c: 'test'};
var expected = {
    "a": {
        "beanClass": "java.net.URI",
        "properties": {
            "rawFragment": null,
            "userInfo": null,
            "opaque": false,
            "scheme": "test",
            "query": null,
            "schemeSpecificPart": "//bean/converter",
            "rawUserInfo": null,
            "path": "/converter",
            "fragment": null,
            "rawPath": "/converter",
            "port": -1,
            "rawSchemeSpecificPart": "//bean/converter",
            "absolute": true,
            "rawAuthority": "bean",
            "authority": "bean",
            "host": "bean",
            "rawQuery": null
        }
    },
    "b": {},
    "c": "test"
};
var actual = JSON.parse(JSON.stringify(obj));
assertEquals(expected, actual);

"success"
