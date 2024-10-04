package org.mozilla.javascript;

import static org.junit.Assert.assertNotNull;
import static org.mozilla.javascript.Token.FIRST_TOKEN;
import static org.mozilla.javascript.Token.LAST_TOKEN;

import org.junit.Test;

public class TokenTest {
    @Test
    public void allTokensHaveAName() {
        for (int token = FIRST_TOKEN; token < LAST_TOKEN; ++token) {
            assertNotNull(Token.typeToName(token));
        }
    }
}
