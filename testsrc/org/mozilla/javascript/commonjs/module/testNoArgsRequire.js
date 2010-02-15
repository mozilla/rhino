/**
 * @version $Id: testNoArgsRequire.js,v 1.1 2010/02/15 19:31:15 szegedia%freemail.hu Exp $
 */
var assert = require("assert");
try {
    require();
    assert.fail("require() succeeded with no arguments");
}
catch(e) {
    assert.equal(e.message, "require() needs one argument");
}

try {
    new require();
    assert.fail("require() succeeded as a constructor");
}
catch(e) {
    assert.equal(e.message, "require() can not be invoked as a constructor");
}