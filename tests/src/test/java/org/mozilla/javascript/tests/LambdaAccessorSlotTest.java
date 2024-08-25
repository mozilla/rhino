package org.mozilla.javascript.tests;

import static org.junit.Assert.*;
import static org.mozilla.javascript.ScriptableObject.*;
import static org.mozilla.javascript.tests.LambdaAccessorSlotTest.StatusHolder.self;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class LambdaAccessorSlotTest {
    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() throws Exception {
        cx = Context.enter();
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() throws Exception {
        Context.exit();
    }

    @Test
    public void testGetterProperty() {
        StatusHolder.init(scope)
                .definePrototypeProperty(
                        cx,
                        "status",
                        (thisObj) -> self(thisObj).getStatus(),
                        (thisObj, value) -> self(thisObj).setStatus(value),
                        DONTENUM);

        Object getterResult =
                cx.evaluateString(
                        scope, "s = new StatusHolder('InProgress'); s.status", "source", 1, null);
        assertEquals("InProgress", getterResult);
    }

    @Test
    public void testThrowIfNeitherGetterOrSetterAreDefined() {
        var error =
                assertThrows(
                        EcmaError.class,
                        () ->
                                StatusHolder.init(scope)
                                        .definePrototypeProperty(
                                                cx, "status", null, null, DONTENUM));
        assertTrue(error.toString().contains("at least one of {getter, setter} is required"));
    }

    @Test
    public void testCanUpdateValueUsingSetter() {
        StatusHolder.init(scope)
                .definePrototypeProperty(
                        cx,
                        "status",
                        (thisObj) -> self(thisObj).getStatus(),
                        (thisObj, value) -> self(thisObj).setStatus(value),
                        DONTENUM);

        Object getterResult =
                cx.evaluateString(
                        scope, "s = new StatusHolder('InProgress'); s.status", "source", 1, null);
        assertEquals("InProgress", getterResult);

        Object setResult = cx.evaluateString(scope, "s.status = 'DONE';", "source", 1, null);

        Object newStatus = cx.evaluateString(scope, "s.status", "source", 1, null);
        assertEquals("NewStatus: DONE", newStatus);
    }

    @Test
    public void testOnlyGetterCanBeAccessed() {
        StatusHolder.init(scope)
                .definePrototypeProperty(
                        cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

        Object getterResult =
                cx.evaluateString(scope, "new StatusHolder('OK').status", "source", 1, null);
        assertEquals("OK", getterResult);

        Object hiddenFieldResult =
                cx.evaluateString(scope, "new StatusHolder('OK').hiddenStatus", "source", 1, null);
        assertEquals(
                "fields not explicitly defined as properties should return undefined",
                Undefined.instance,
                hiddenFieldResult);
    }

    @Test
    public void testWhenNoSetterDefined_InStrictMode_WillThrowException() {
        StatusHolder.init(scope)
                .definePrototypeProperty(
                        cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);
        Object getterResult =
                cx.evaluateString(
                        scope, "s = new StatusHolder('Constant'); s.status", "source", 1, null);
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
    }

    @Test
    public void testWhenNoSetterDefined_InNormalMode_NoErrorButValueIsNotChanged() {
        StatusHolder.init(scope)
                .definePrototypeProperty(
                        cx, "status", (thisObj) -> self(thisObj).getStatus(), DONTENUM);

        Object getterResult =
                cx.evaluateString(
                        scope, "s = new StatusHolder('Constant'); s.status", "source", 1, null);
        assertEquals("Constant", getterResult);

        Object setResult =
                cx.evaluateString(scope, "s.status = 'DONE'; s.status", "source", 1, null);
        assertEquals("status won't be changed", "Constant", setResult);

        Object shObj = cx.evaluateString(scope, "s", "source", 1, null);
        var statusHolder = (StatusHolder) shObj;
        assertEquals("Constant", statusHolder.getStatus());
    }

    @Test
    public void testSetterOnly_WillModifyUnderlyingValue() {
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
    }

    // using getOwnPropertyDescriptor to access property

    @Test
    public void testGetterUsing_getOwnPropertyDescriptor() {
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
    }

    @Test
    public void testSetterOnlyUsing_getOwnPropertyDescriptor() {
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
    }

    @Test
    public void testSetValueUsing_getOwnPropertyDescriptor() {
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
    }

    @Test
    public void testSetterOnlyUsing_getOwnPropertyDescriptor_ErrorOnGet() {
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
        assertTrue(error.toString().contains("Cannot call method \"call\" of undefined"));
    }

    @Test
    public void testRedefineExistingProperty_ChangingConfigurableAttr_ShouldFailValidation() {
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
                                        (thisObj, value) -> self(thisObj).setStatus(value),
                                        DONTENUM));
        assertTrue(
                error.toString()
                        .contains(
                                ScriptRuntime.getMessageById(
                                        "msg.change.configurable.false.to.true", "status")));
    }

    @Test
    public void
            testRedefineExistingProperty_ModifyingNotConfigurableProperty_ShouldFailValidation() {
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
                                        (thisObj, value) -> self(thisObj).setStatus(value),
                                        // making new property configurable: false and enumerable:
                                        // false
                                        DONTENUM | PERMANENT));
        assertTrue(
                error.toString()
                        .contains(
                                ScriptRuntime.getMessageById(
                                        "msg.change.enumerable.with.configurable.false",
                                        "status")));
    }

    @Test
    public void testSetterOnlyUsing_getOwnPropertyDescriptor_InStrictMode_ErrorOnGet() {
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
        assertTrue(error.toString().contains("Cannot call method \"call\" of undefined"));
    }

    @Test
    public void testGetterOnlyUsing_getOwnPropertyDescriptor_ErrorOnSet() {
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
        assertTrue(error.toString().contains("Cannot call method \"call\" of undefined"));
    }

    @Test
    public void testGetterOnlyUsing_getOwnPropertyDescriptor_InStrictMode_ErrorOnSet() {
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
        assertTrue(error.toString().contains("Cannot call method \"call\" of undefined"));
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
