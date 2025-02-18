package org.mozilla.javascript.tests;

import static org.junit.Assert.*;
import static org.mozilla.javascript.ScriptableObject.*;
import static org.mozilla.javascript.tests.LambdaAccessorSlotTest.StatusHolder.self;

import org.junit.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

public class LambdaAccessorSlotTest {
    @Test
    public void testGetterProperty() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    (thisObj) -> self(thisObj).getStatus(),
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);

                    Object getterResult =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('InProgress'); s.status",
                                    "source",
                                    1,
                                    null);
                    assertEquals("InProgress", getterResult);
                    return null;
                });
    }

    @Test
    public void testThrowIfNeitherGetterOrSetterAreDefined() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            StatusHolder.init(scope)
                                                    .definePrototypeProperty(
                                                            cx, "status", null, null, DONTENUM));
                    assertTrue(
                            error.toString()
                                    .contains("at least one of {getter, setter} is required"));
                    return null;
                });
    }

    @Test
    public void testCanUpdateValueUsingSetter() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    (thisObj) -> self(thisObj).getStatus(),
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);

                    Object getterResult =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('InProgress'); s.status",
                                    "source",
                                    1,
                                    null);
                    assertEquals("InProgress", getterResult);

                    Object setResult =
                            cx.evaluateString(scope, "s.status = 'DONE';", "source", 1, null);

                    Object newStatus = cx.evaluateString(scope, "s.status", "source", 1, null);
                    assertEquals("NewStatus: DONE", newStatus);
                    return null;
                });
    }

    @Test
    public void testOnlyGetterCanBeAccessed() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

                    Object getterResult =
                            cx.evaluateString(
                                    scope, "new StatusHolder('OK').status", "source", 1, null);
                    assertEquals("OK", getterResult);

                    Object hiddenFieldResult =
                            cx.evaluateString(
                                    scope,
                                    "new StatusHolder('OK').hiddenStatus",
                                    "source",
                                    1,
                                    null);
                    assertEquals(
                            "fields not explicitly defined as properties should return undefined",
                            Undefined.instance,
                            hiddenFieldResult);
                    return null;
                });
    }

    @Test
    public void testRedefineExistingProperty() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    var sh = new StatusHolder("PENDING");

                    sh.defineProperty("value", "oldValueOfValue", DONTENUM);

                    sh.defineProperty(cx, "value", (thisObj) -> "valueOfValue", null, DONTENUM);

                    sh.defineProperty(cx, "status", (thisObj) -> 42, null, DONTENUM);

                    sh.defineProperty(
                            cx,
                            "status",
                            (thisObj) -> self(thisObj).getStatus(),
                            (thisObj, value) -> self(thisObj).setStatus(value),
                            DONTENUM);

                    var status = sh.get("status", sh);
                    assertEquals("PENDING", status);

                    var value = sh.get("value", sh);
                    assertEquals("valueOfValue", value);
                    return null;
                });
    }

    @Test
    public void testWhenNoSetterDefined_InStrictMode_WillThrowException() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);
                    Object getterResult =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('Constant'); s.status",
                                    "source",
                                    1,
                                    null);
                    assertEquals("Constant", getterResult);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "\"use strict\"; s.status = 'DONE'; s.status",
                                                    "source",
                                                    1,
                                                    null));
                    String expectedError =
                            ScriptRuntime.getMessageById(
                                    "msg.set.prop.no.setter", "[StatusHolder].status", "DONE");
                    assertTrue(error.toString().contains(expectedError));
                    return null;
                });
    }

    @Test
    public void testWhenNoSetterDefined_InNormalMode_NoErrorButValueIsNotChanged() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

                    Object getterResult =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('Constant'); s.status",
                                    "source",
                                    1,
                                    null);
                    assertEquals("Constant", getterResult);

                    Object setResult =
                            cx.evaluateString(
                                    scope, "s.status = 'DONE'; s.status", "source", 1, null);
                    assertEquals("status won't be changed", "Constant", setResult);

                    Object shObj = cx.evaluateString(scope, "s", "source", 1, null);
                    var statusHolder = (StatusHolder) shObj;
                    assertEquals("Constant", statusHolder.getStatus());
                    return null;
                });
    }

    @Test
    public void testSetterOnly_WillModifyUnderlyingValue() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    null,
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);
                    cx.evaluateString(scope, "s = new StatusHolder('Constant')", "source", 1, null);

                    cx.evaluateString(scope, "s.status = 'DONE'; s.status", "source", 1, null);

                    Object newStatus = cx.evaluateString(scope, "s.status", "source", 1, null);
                    assertEquals(null, newStatus);
                    Object shObj = cx.evaluateString(scope, "s", "source", 1, null);
                    var statusHolder = (StatusHolder) shObj;
                    assertEquals("NewStatus: DONE", statusHolder.getStatus());
                    return null;
                });
    }

    // using getOwnPropertyDescriptor to access property

    @Test
    public void testGetterUsing_getOwnPropertyDescriptor() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('InProgress');"
                                            + "f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                            + "f.get.call(s)",
                                    "source",
                                    1,
                                    null);
                    assertEquals("InProgress", result);
                    return null;
                });
    }

    @Test
    public void testSetterOnlyUsing_getOwnPropertyDescriptor() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    null,
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);

                    Object shObj =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('InProgress');"
                                            + "f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                            + "f.set.call(s, 'DONE');"
                                            + "s",
                                    "source",
                                    1,
                                    null);
                    var statusHolder = (StatusHolder) shObj;
                    assertEquals("NewStatus: DONE", statusHolder.getStatus());
                    return null;
                });
    }

    @Test
    public void testSetValueUsing_getOwnPropertyDescriptor() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    (thisObj) -> self(thisObj).getStatus(),
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "s = new StatusHolder('InProgress');"
                                            + "f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                            + "f.set.call(s, 'DONE');"
                                            + "s.status",
                                    "source",
                                    1,
                                    null);
                    assertEquals("Status with prefix", "NewStatus: DONE", result);
                    return null;
                });
    }

    @Test
    public void testSetterOnlyUsing_getOwnPropertyDescriptor_ErrorOnGet() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    null,
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "var s = new StatusHolder('InProgress');"
                                                            + "var f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                                            + "f.get.call(s)",
                                                    "source",
                                                    1,
                                                    null));
                    assertTrue(
                            error.toString().contains("Cannot call method \"call\" of undefined"));
                    return null;
                });
    }

    @Test
    public void testRedefineExistingProperty_ChangingConfigurableAttr_ShouldFailValidation() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    var sh = new StatusHolder("PENDING");
                    ScriptableObject existingDesc = (ScriptableObject) cx.newObject(scope);

                    //
                    existingDesc.defineProperty("configurable", false, ScriptableObject.EMPTY);

                    sh.defineOwnProperty(cx, "status", existingDesc);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            sh.defineProperty(
                                                    cx,
                                                    "status",
                                                    (thisObj) -> self(thisObj).getStatus(),
                                                    (thisObj, value) ->
                                                            self(thisObj).setStatus(value),
                                                    DONTENUM));
                    assertTrue(
                            error.toString()
                                    .contains(
                                            ScriptRuntime.getMessageById(
                                                    "msg.change.configurable.false.to.true",
                                                    "status")));
                    return null;
                });
    }

    @Test
    public void
            testRedefineExistingProperty_ModifyingNotConfigurableProperty_ShouldFailValidation() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    var sh = new StatusHolder("PENDING");
                    ScriptableObject existingDesc = (ScriptableObject) cx.newObject(scope);

                    //
                    existingDesc.defineProperty("configurable", false, ScriptableObject.EMPTY);
                    existingDesc.defineProperty("enumerable", true, ScriptableObject.EMPTY);

                    sh.defineOwnProperty(cx, "status", existingDesc);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            sh.defineProperty(
                                                    cx,
                                                    "status",
                                                    (thisObj) -> self(thisObj).getStatus(),
                                                    (thisObj, value) ->
                                                            self(thisObj).setStatus(value),
                                                    // making new property configurable: false and
                                                    // enumerable:
                                                    // false
                                                    DONTENUM | PERMANENT));
                    assertTrue(
                            error.toString()
                                    .contains(
                                            ScriptRuntime.getMessageById(
                                                    "msg.change.enumerable.with.configurable.false",
                                                    "status")));
                    return null;
                });
    }

    @Test
    public void testSetterOnlyUsing_getOwnPropertyDescriptor_InStrictMode_ErrorOnGet() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx,
                                    "status",
                                    null,
                                    (thisObj, value) -> self(thisObj).setStatus(value),
                                    DONTENUM);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "\"use strict\";"
                                                            + "var s = new StatusHolder('InProgress');"
                                                            + "var f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                                            + "f.get.call(s)",
                                                    "source",
                                                    1,
                                                    null));
                    assertTrue(
                            error.toString().contains("Cannot call method \"call\" of undefined"));
                    return null;
                });
    }

    @Test
    public void testGetterOnlyUsing_getOwnPropertyDescriptor_ErrorOnSet() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "var s = new StatusHolder('InProgress');"
                                                            + "var f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                                            + "f.set.call(s, 'DONE');"
                                                            + "s.status",
                                                    "source",
                                                    1,
                                                    null));
                    assertTrue(
                            error.toString().contains("Cannot call method \"call\" of undefined"));
                    return null;
                });
    }

    @Test
    public void testGetterOnlyUsing_getOwnPropertyDescriptor_InStrictMode_ErrorOnSet() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    StatusHolder.init(scope)
                            .definePrototypeProperty(
                                    cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

                    var error =
                            assertThrows(
                                    EcmaError.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "\"use strict\";"
                                                            + "var s = new StatusHolder('InProgress');"
                                                            + "var f = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(s), 'status');"
                                                            + "f.set.call(s, 'DONE');"
                                                            + "s.status",
                                                    "source",
                                                    1,
                                                    null));
                    assertTrue(
                            error.toString().contains("Cannot call method \"call\" of undefined"));
                    return null;
                });
    }

    static class StatusHolder extends ScriptableObject {
        private String status;
        private final String hiddenStatus;

        static LambdaConstructor init(Scriptable scope) {
            LambdaConstructor constructor =
                    new LambdaConstructor(
                            scope,
                            "StatusHolder",
                            1,
                            LambdaConstructor.CONSTRUCTOR_NEW,
                            (cx, scope1, args) -> new StatusHolder((String) args[0]));

            ScriptableObject.defineProperty(scope, "StatusHolder", constructor, DONTENUM);
            return constructor;
        }

        static StatusHolder self(Scriptable thisObj) {
            return LambdaConstructor.convertThisObject(thisObj, StatusHolder.class);
        }

        StatusHolder(String status) {
            this.status = status;
            this.hiddenStatus = "NotQuiteReady";
        }

        public String getStatus() {
            return status;
        }

        @Override
        public String getClassName() {
            return "StatusHolder";
        }

        public void setStatus(Object value) {
            this.status = "NewStatus: " + (String) value;
        }
    }
}
