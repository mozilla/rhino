/**
 * @version $Id: testSandboxed.js,v 1.1 2010/02/15 19:31:15 szegedia%freemail.hu Exp $
 */
var assert = require("assert");

assert.strictEqual(require.paths, undefined);
assert.strictEqual(module.uri, undefined);