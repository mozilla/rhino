package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class EvalGlobalDeclarationsTest {

    private static final String HELPERS =
            """
            function errorName(f) { try { f(); return 'none'; } catch (e) { return e.name; } }
            function attrs(n) {
              var d = Object.getOwnPropertyDescriptor(globalThis, n);
              return d ? [d.writable, d.enumerable, d.configurable].join('/') : 'none';
            }
            """;

    @Test
    void nonDefinableGlobalFunctionIsTypeError() {
        Utils.assertWithAllModes_ES6(
                "TypeError,TypeError,TypeError,none",
                HELPERS
                        + """
                var direct;
                try { eval('function NaN() {}'); direct = 'none'; } catch (e) { direct = e.name; }
                [direct,
                 errorName(() => (0, eval)('function NaN() {}')),
                 errorName(() => (0, eval)('function* NaN() {}')),
                 errorName(() => (0, eval)('var NaN;'))].join();
                """);
    }

    @Test
    void failedGlobalEvalDeclaresNothing() {
        Utils.assertWithAllModes_ES6(
                "none,none",
                HELPERS
                        + """
                try { (0, eval)('function notDefined1() {} function NaN() {}'); } catch (e) {}
                try { (0, eval)('var notDefined2; function NaN() {}'); } catch (e) {}
                [attrs('notDefined1'), attrs('notDefined2')].join();
                """);
    }

    @Test
    void globalEvalBindingsAreConfigurable() {
        Utils.assertWithAllModes_ES6(
                "true/true/true,true/true/true",
                HELPERS
                        + """
                (0, eval)('function newFn() {} var newVar;');
                [attrs('newFn'), attrs('newVar')].join();
                """);
    }

    @Test
    void globalEvalFunctionRedefinesConfigurableProperty() {
        Utils.assertWithAllModes_ES6(
                "true/true/true,345,true/true/false,2",
                HELPERS
                        + """
                Object.defineProperty(globalThis, 'f', {enumerable: false, writable: false, configurable: true});
                Object.defineProperty(globalThis, 'g', {value: 1, enumerable: true, writable: true, configurable: false});
                var initial;
                (0, eval)('initial = f; function f() { return 345; } function g() { return 2; }');
                [attrs('f'), initial(), attrs('g'), g()].join();
                """);
    }

    @Test
    void nonExtensibleGlobalRejectsNewDeclarations() {
        Utils.assertWithAllModes_ES6(
                "TypeError,TypeError,none,none",
                HELPERS
                        + """
                var existing;
                var direct;
                Object.preventExtensions(globalThis);
                try { eval('var unlikely1;'); direct = 'none'; } catch (e) { direct = e.name; }
                [direct,
                 errorName(() => (0, eval)('function unlikely2() {}')),
                 errorName(() => (0, eval)('var existing;')),
                 errorName(function() { eval('function local() {}'); })].join();
                """);
    }
}
