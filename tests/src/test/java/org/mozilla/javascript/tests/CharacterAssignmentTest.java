package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

/**
 * @author Jakob Silbereisen, Foconis Analytics GmbH
 */
public class CharacterAssignmentTest {

    public static class X {
        public Character bigCharacter;
        public char primitiveCharacter;

        public X() {}

        public X(char c) {
            bigCharacter = c;
            primitiveCharacter = c;
        }
    }

    @Test
    public void assignCharacterTypesFromNumbers() {
        Utils.runWithAllModes(
                cx -> {
                    final Global scope = new Global();
                    scope.init(cx);
                    X x1 = new X(), x2 = new X();
                    scope.put("x1", scope, x1);
                    scope.put("x2", scope, x2);
                    scope.put("src", scope, new X('X'));
                    Object ret =
                            cx.evaluateString(
                                    scope,
                                    "x1.bigCharacter = 65;"
                                            + "x1.primitiveCharacter = 65;"
                                            + "x2.bigCharacter = src.primitiveCharacter;",
                                    "myScript.js",
                                    1,
                                    null);
                    assertEquals('A', x1.bigCharacter);
                    assertEquals('A', x1.primitiveCharacter);
                    assertEquals('X', x2.bigCharacter);
                    return null;
                });
    }
}
