// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

// Test that reserved words can be used as both XML attribute names
// and as part of a name space qualified name.

load('testsrc/assert.js');

x =
<alpha>
    <bravo for="value1" ns:for="value3" xmlns:ns="http://someuri">
        <charlie class="value2" ns:class="value4"/>
    </bravo>
</alpha>

assertEquals("value1", x.bravo.@for.toXMLString());
assertEquals("value2", x.bravo.charlie.@class.toXMLString());
n = new Namespace("http://someuri");
assertEquals("value3", x.bravo.@n::for.toXMLString());
assertEquals("value4", x.bravo.charlie.@n::class.toXMLString());

"success"
