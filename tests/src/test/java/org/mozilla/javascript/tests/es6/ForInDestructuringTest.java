package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** In ES6 a for-in destructuring pattern destructures each key, not a [key, value] pair. */
class ForInDestructuringTest {

    @Test
    void arrayPatternDestructuresKey() {
        Utils.assertWithAllModes_ES6(
                "a,b|a,b",
                """
                var r = [];
                for (let [x, y] in {ab: 1}) r.push(x + ',' + y);
                for (var [p, q] in {ab: 1}) r.push(p + ',' + q);
                r.join('|');
                """);
    }

    @Test
    void anyPatternLengthIsAllowed() {
        Utils.assertWithAllModes_ES6(
                "a|c",
                """
                var r = [];
                for (let [x] in {ab: 1}) r.push(x);
                for (const [, , z] in {abc: 1}) r.push(z);
                r.join('|');
                """);
    }

    @Test
    void objectPatternDestructuresKey() {
        Utils.assertWithAllModes_ES6(3, "var r; for (let {length} in {abc: 1}) r = length; r;");
    }

    @Test
    void defaultIsAppliedToMissingElement() {
        Utils.assertWithAllModes_ES6(
                "function,x",
                """
                let x = 'outside';
                var probeDecl;
                for (let [x, _ = probeDecl = function() { return x; }] in {x: 0}) ;
                typeof probeDecl + ',' + probeDecl();
                """);
    }

    @Test
    void legacyKeyValuePairBeforeES6() {
        Utils.assertWithAllModes_1_8(
                "k,1", "var r; for (var [a, b] in {k: 1}) r = a + ',' + b; r;");
    }
}
