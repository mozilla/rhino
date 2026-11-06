/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for the global lexical declarations ({@code let} and {@code const}) of scripts sharing the
 * same global scope and for the lexical declarations of eval code.
 */
class GlobalLexicalDeclarationTest {

    @Test
    void letIsNotPropertyOfGlobalObject() {
        Utils.assertWithAllModes_ES6(
                "1 false", "let a = 1; a + ' ' + globalThis.hasOwnProperty('a')");
    }

    @Test
    void constIsNotPropertyOfGlobalObject() {
        Utils.assertWithAllModes_ES6(
                "1 false", "const a = 1; a + ' ' + globalThis.hasOwnProperty('a')");
    }

    @Test
    void varIsPropertyOfGlobalObject() {
        Utils.assertWithAllModes_ES6(
                "1 true", "var a = 1; a + ' ' + globalThis.hasOwnProperty('a')");
    }

    @Test
    void letIsSharedBetweenScripts() {
        Utils.assertWithAllModesScripts_ES6(
                "2", "let a = 1;", "function f() { return a; }", "a = 2;", "String(f())");
    }

    @Test
    void propertyOfGlobalObjectDoesNotChangeLet() {
        Utils.assertWithAllModes_ES6(
                "lex obj", "let a = 'lex'; globalThis.a = 'obj'; a + ' ' + globalThis.a");
    }

    @Test
    void letShadowsConfigurablePropertyOfGlobalObject() {
        Utils.assertWithAllModesScripts_ES6(
                "lex obj", "globalThis.a = 'obj';", "let a = 'lex'; a + ' ' + globalThis.a");
    }

    @Test
    void constShadowsConfigurablePropertyOfGlobalObject() {
        Utils.assertWithAllModesScripts_ES6(
                "1 obj", "globalThis.a = 'obj';", "const a = 1; a + ' ' + globalThis.a");
    }

    @Test
    void letRedeclarationInOtherScript() {
        Utils.assertEcmaErrorScriptsES6("SyntaxError: ", "let a = 1;", "let a = 2;");
    }

    @Test
    void constRedeclarationInOtherScript() {
        Utils.assertEcmaErrorScriptsES6("SyntaxError: ", "const a = 1;", "const a = 2;");
    }

    @Test
    void letAfterVarInOtherScript() {
        Utils.assertEcmaErrorScriptsES6("SyntaxError: ", "var a = 1;", "let a = 2;");
    }

    @Test
    void varAfterLetInOtherScript() {
        Utils.assertEcmaErrorScriptsES6("SyntaxError: ", "let a = 1;", "var a = 2;");
    }

    @Test
    void letAfterFunctionInOtherScript() {
        Utils.assertEcmaErrorScriptsES6("SyntaxError: ", "function a() {}", "let a = 2;");
    }

    @Test
    void letBeforeInitialization() {
        Utils.assertEcmaErrorES6(
                "ReferenceError: Cannot access \"a\" before initialization.", "a; let a = 1;");
    }

    @Test
    void typeofLetBeforeInitialization() {
        Utils.assertEcmaErrorES6(
                "ReferenceError: Cannot access \"a\" before initialization.", "typeof a; let a;");
    }

    @Test
    void constBeforeInitialization() {
        Utils.assertEcmaErrorES6(
                "ReferenceError: Cannot access \"a\" before initialization.",
                "typeof a; const a = 1;");
    }

    @Test
    void closureReadsLetBeforeInitialization() {
        Utils.assertEcmaErrorES6(
                "ReferenceError: Cannot access \"a\" before initialization.",
                "function f() { return a; } f(); let a = 1;");
    }

    @Test
    void letWithoutInitializerIsUndefined() {
        Utils.assertWithAllModes_ES6("undefined", "let a; String(a)");
    }

    @Test
    void letDestructuringWithDefault() {
        Utils.assertWithAllModes_ES6("1 2", "let [a = 1, b = 3] = [undefined, 2]; a + ' ' + b");
    }

    @Test
    void failedScriptDeclaresNothing() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel scope = cx.initStandardObjects();
                    cx.evaluateString(scope, "var b;", "test1.js", 1, null);
                    assertThrows(
                            EcmaError.class,
                            () ->
                                    cx.evaluateString(
                                            scope, "let c = 1; let b = 2;", "test2.js", 1, null));
                    assertEquals(
                            "undefined", cx.evaluateString(scope, "typeof c", "test3.js", 1, null));
                    return null;
                });
    }

    @Test
    void evalLetIsLocalToEvalCode() {
        Utils.assertWithAllModes_ES6("1 undefined", "eval('let a = 1; a') + ' ' + typeof a");
    }

    @Test
    void evalConstTwice() {
        Utils.assertWithAllModes_ES6(
                "2 undefined", "eval('const a = 1;'); eval('const a = 2; a') + ' ' + typeof a");
    }

    @Test
    void indirectEvalConstTwice() {
        Utils.assertWithAllModes_ES6(
                "2 undefined",
                "(0, eval)('const a = 1;'); (0, eval)('const a = 2; a') + ' ' + typeof a");
    }

    @Test
    void indirectEvalConstWithGlobalConst() {
        Utils.assertWithAllModes_ES6("2 1", "const a = 1; (0, eval)('const a = 2; a') + ' ' + a");
    }

    @Test
    void evalLetBeforeInitialization() {
        Utils.assertWithAllModes_ES6(
                "ReferenceError ReferenceError",
                "var res = [];\n"
                        + "try { eval('typeof a; let a;'); } catch (e) { res.push(e.name); }\n"
                        + "try { (0, eval)('typeof a; const a = 1;'); } catch (e) { res.push(e.name); }\n"
                        + "res.join(' ')");
    }

    @Test
    void evalVarIsGlobal() {
        Utils.assertWithAllModes_ES6(
                "undefined 2", "eval('let a = 1; var b = a + 1;'); typeof a + ' ' + b");
    }

    @Test
    void evalVarKeepsValueOfExistingVar() {
        Utils.assertWithAllModes_ES6("23", "var x = 23; eval('var x;'); String(x)");
    }

    @Test
    void evalFunctionDeclarationIsGlobal() {
        Utils.assertWithAllModes_ES6(
                "1 undefined",
                "eval('let a = 1; function f() { return a; }'); f() + ' ' + typeof a");
    }

    @Test
    void evalInFunction() {
        Utils.assertWithAllModes_ES6(
                "undefined 2",
                "(function() { eval('let a = 1; var b = 2;'); return typeof a + ' ' + b; })()");
    }
}
