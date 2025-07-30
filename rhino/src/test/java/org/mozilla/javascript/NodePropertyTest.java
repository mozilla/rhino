package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NodePropertyTest {
    @Test
    void canPrintAllPropertyNames() {
        for (int propId = Node.FIRST_PROP; propId <= Node.LAST_PROP; ++propId) {
            assertNotNull(Node.propName(propId), "property with id " + propId);
        }
    }
}
