load('testsrc/assert.js');

function replacer(key, value) {
    var isXMLList = value instanceof XMLList;
    var isXML = value instanceof XML;
    return isXMLList ? 'XMLList with ' + value.length() + ' nodes' :
        isXML ? 'XML with ' + value.children().length() + ' children' :
        value;
}

XML.prettyPrinting = false;

// empty object
var xml = new XML();
var obj = {test: xml};

var expected = JSON.stringify({test: 'XML with 0 children'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var expected = JSON.stringify({test: ''});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);


// empty list
var xml = new XMLList();
var obj = {test: xml};

var expected = JSON.stringify({test: 'XMLList with 0 nodes'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var expected = JSON.stringify({test: ''});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);


// no children
var xml = <xml value="1"/>;
var obj = {test: xml};

var expected = JSON.stringify({test: 'XML with 0 children'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var expected = JSON.stringify({test: ''});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);


// simple content
var xml = <xml>simple</xml>;
var obj = {test: xml};

var expected = JSON.stringify({test: 'XML with 1 children'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var expected = JSON.stringify({test: 'simple'});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);


// with children
var xml = <xml>
    <a>1</a>
    <a>2</a>
</xml>;
var obj = {test: xml};

var expected = JSON.stringify({test: 'XML with 2 children'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var expected = JSON.stringify({test: '<xml><a>1</a><a>2</a></xml>'});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);

// list
var xml = xml.a;
var obj = {test: xml};

var expected = JSON.stringify({test: 'XMLList with 2 nodes'});
var actual = JSON.stringify(obj, replacer);
assertEquals(expected, actual);

var expected = JSON.stringify({test: '<a>1</a><a>2</a>'});
var actual = JSON.stringify(obj);
assertEquals(expected, actual);


"success"
