/**
 * @version $Id: testNonSandboxed.js,v 1.1 2010/02/15 19:31:15 szegedia%freemail.hu Exp $
 */
var assert = require("assert");
function isUndefined(x) {var u; return x === u;}
assert.ok(isUndefined(require.paths));
assert.ok(isUndefined(module.uri));