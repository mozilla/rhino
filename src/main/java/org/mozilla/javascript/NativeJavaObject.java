/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Date;

/**
 * This class reflects non-Array Java objects into the JavaScript environment.  It
 * reflect fields directly, and uses NativeJavaMethod objects to reflect (possibly
 * overloaded) methods.<p>
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaPackage
 * @see NativeJavaClass
 */

public class NativeJavaObject implements Scriptable, Wrapper, Serializable
{
    static final long serialVersionUID = -6948590651130498591L;

    public NativeJavaObject() { }

    public NativeJavaObject(Scriptable scope, Object javaObject,
                            Class<?> staticType)
    {
        this(scope, javaObject, staticType, false);
    }

    public NativeJavaObject(Scriptable scope, Object javaObject,
                            Class<?> staticType, boolean isAdapter)
    {
        this.parent = scope;
        this.javaObject = javaObject;
        this.staticType = staticType;
        this.isAdapter = isAdapter;
        initMembers();
    }

    protected void initMembers() {
        Class<?> dynamicType;
        if (javaObject != null) {
            dynamicType = javaObject.getClass();
        } else {
            dynamicType = staticType;
        }
        members = JavaMembers.lookupClass(parent, dynamicType, staticType,
                                          isAdapter);
        fieldAndMethods
            = members.getFieldAndMethodsObjects(this, javaObject, false);
    }

    public boolean has(String name, Scriptable start) {
        return members.has(name, false);
    }

    public boolean has(int index, Scriptable start) {
        return false;
    }

    public Object get(String name, Scriptable start) {
        if (fieldAndMethods != null) {
            Object result = fieldAndMethods.get(name);
            if (result != null) {
                return result;
            }
        }
        // TODO: passing 'this' as the scope is bogus since it has
        //  no parent scope
        return members.get(this, name, javaObject, false);
    }

    public Object get(int index, Scriptable start) {
        throw members.reportMemberNotFound(Integer.toString(index));
    }

    public void put(String name, Scriptable start, Object value) {
        // We could be asked to modify the value of a property in the
        // prototype. Since we can't add a property to a Java object,
        // we modify it in the prototype rather than copy it down.
        if (prototype == null || members.has(name, false))
            members.put(this, name, javaObject, value, false);
        else
            prototype.put(name, prototype, value);
    }

    public void put(int index, Scriptable start, Object value) {
        throw members.reportMemberNotFound(Integer.toString(index));
    }

    public boolean hasInstance(Scriptable value) {
        // This is an instance of a Java class, so always return false
        return false;
    }

    public void delete(String name) {
    }

    public void delete(int index) {
    }

    public Scriptable getPrototype() {
        if (prototype == null && javaObject instanceof String) {
            return TopLevel.getBuiltinPrototype(
                    ScriptableObject.getTopLevelScope(parent),
                    TopLevel.Builtins.String);
        }
        return prototype;
    }

    /**
     * Sets the prototype of the object.
     */
    public void setPrototype(Scriptable m) {
        prototype = m;
    }

    /**
     * Returns the parent (enclosing) scope of the object.
     */
    public Scriptable getParentScope() {
        return parent;
    }

    /**
     * Sets the parent (enclosing) scope of the object.
     */
    public void setParentScope(Scriptable m) {
        parent = m;
    }

    public Object[] getIds() {
        return members.getIds(false);
    }

    /**
     * @deprecated Use {@link Context#getWrapFactory()} together with calling {@link
     * WrapFactory#wrap(Context, Scriptable, Object, Class)}
     */
    @Deprecated
    public static Object wrap(Scriptable scope, Object obj, Class<?> staticType) {

        Context cx = Context.getContext();
        return cx.getWrapFactory().wrap(cx, scope, obj, staticType);
    }

    public Object unwrap() {
        return javaObject;
    }

    public String getClassName() {
        return "JavaObject";
    }

    public Object getDefaultValue(Class<?> hint)
    {
        Object value;
        if (hint == null) {
            if (javaObject instanceof Boolean) {
                hint = ScriptRuntime.BooleanClass;
            }
        }
        if (hint == null || hint == ScriptRuntime.StringClass) {
            value = javaObject.toString();
        } else {
            String converterName;
            if (hint == ScriptRuntime.BooleanClass) {
                converterName = "booleanValue";
            } else if (hint == ScriptRuntime.NumberClass) {
                converterName = "doubleValue";
            } else {
                throw Context.reportRuntimeError0("msg.default.value");
            }
            Object converterObject = get(converterName, this);
            if (converterObject instanceof Function) {
                Function f = (Function)converterObject;
                value = f.call(Context.getContext(), f.getParentScope(),
                               this, ScriptRuntime.emptyArgs);
            } else {
                if (hint == ScriptRuntime.NumberClass
                    && javaObject instanceof Boolean)
                {
                    boolean b = ((Boolean)javaObject).booleanValue();
                    value = ScriptRuntime.wrapNumber(b ? 1.0 : 0.0);
                } else {
                    value = javaObject.toString();
                }
            }
        }
        return value;
    }

    /**
     * Determine whether we can/should convert between the given type and the
     * desired one.  This should be superceded by a conversion-cost calculation
     * function, but for now I'll hide behind precedent.
     */
    public static boolean canConvert(Object fromObj, Class<?> to) {
        int weight = getConversionWeight(fromObj, to);

        return (weight < CONVERSION_NONE);
    }

    private static final int JSTYPE_UNDEFINED   = 0; // undefined type
    private static final int JSTYPE_NULL        = 1; // null
    private static final int JSTYPE_BOOLEAN     = 2; // boolean
    private static final int JSTYPE_NUMBER      = 3; // number
    private static final int JSTYPE_STRING      = 4; // string
    private static final int JSTYPE_JAVA_CLASS  = 5; // JavaClass
    private static final int JSTYPE_JAVA_OBJECT = 6; // JavaObject
    private static final int JSTYPE_JAVA_ARRAY  = 7; // JavaArray
    private static final int JSTYPE_OBJECT      = 8; // Scriptable

    static final byte CONVERSION_TRIVIAL      = 1;
    static final byte CONVERSION_NONTRIVIAL   = 0;
    static final byte CONVERSION_NONE         = 99;

    /**
     * Derive a ranking based on how "natural" the conversion is.
     * The special value CONVERSION_NONE means no conversion is possible,
     * and CONVERSION_NONTRIVIAL signals that more type conformance testing
     * is required.
     * Based on
     * <a href="http://www.mozilla.org/js/liveconnect/lc3_method_overloading.html">
     * "preferred method conversions" from Live Connect 3</a>
     */
    static int getConversionWeight(Object fromObj, Class<?> to) {
        int fromCode = getJSTypeCode(fromObj);

        switch (fromCode) {

        case JSTYPE_UNDEFINED:
            if (to == ScriptRuntime.StringClass ||
                to == ScriptRuntime.ObjectClass) {
                return 1;
            }
            break;

        case JSTYPE_NULL:
            if (!to.isPrimitive()) {
                return 1;
            }
            break;

        case JSTYPE_BOOLEAN:
            // "boolean" is #1
            if (to == Boolean.TYPE) {
                return 1;
            }
            else if (to == ScriptRuntime.BooleanClass) {
                return 2;
            }
            else if (to == ScriptRuntime.ObjectClass) {
                return 3;
            }
            else if (to == ScriptRuntime.StringClass) {
                return 4;
            }
            break;

        case JSTYPE_NUMBER:
            if (to.isPrimitive()) {
                if (to == Double.TYPE) {
                    return 1;
                }
                else if (to != Boolean.TYPE) {
                    return 1 + getSizeRank(to);
                }
            }
            else {
                if (to == ScriptRuntime.StringClass) {
                    // native numbers are #1-8
                    return 9;
                }
                else if (to == ScriptRuntime.ObjectClass) {
                    return 10;
                }
                else if (ScriptRuntime.NumberClass.isAssignableFrom(to)) {
                    // "double" is #1
                    return 2;
                }
            }
            break;

        case JSTYPE_STRING:
            if (to == ScriptRuntime.StringClass) {
                return 1;
            }
            else if (to.isInstance(fromObj)) {
                return 2;
            }
            else if (to.isPrimitive()) {
                if (to == Character.TYPE) {
                    return 3;
                } else if (to != Boolean.TYPE) {
                    return 4;
                }
            }
            break;

        case JSTYPE_JAVA_CLASS:
            if (to == ScriptRuntime.ClassClass) {
                return 1;
            }
            else if (to == ScriptRuntime.ObjectClass) {
                return 3;
            }
            else if (to == ScriptRuntime.StringClass) {
                return 4;
            }
            break;

        case JSTYPE_JAVA_OBJECT:
        case JSTYPE_JAVA_ARRAY:
            Object javaObj = fromObj;
            if (javaObj instanceof Wrapper) {
                javaObj = ((Wrapper)javaObj).unwrap();
            }
            if (to.isInstance(javaObj)) {
                return CONVERSION_NONTRIVIAL;
            }
            if (to == ScriptRuntime.StringClass) {
                return 2;
            }
            else if (to.isPrimitive() && to != Boolean.TYPE) {
                return (fromCode == JSTYPE_JAVA_ARRAY)
                       ? CONVERSION_NONE : 2 + getSizeRank(to);
            }
            break;

        case JSTYPE_OBJECT:
            // Other objects takes #1-#3 spots
            if (to != ScriptRuntime.ObjectClass && to.isInstance(fromObj)) {
                // No conversion required, but don't apply for java.lang.Object
                return 1;
            }
            if (to.isArray()) {
                if (fromObj instanceof NativeArray) {
                    // This is a native array conversion to a java array
                    // Array conversions are all equal, and preferable to object
                    // and string conversion, per LC3.
                    return 2;
                }
            }
            else if (to == ScriptRuntime.ObjectClass) {
                return 3;
            }
            else if (to == ScriptRuntime.StringClass) {
                return 4;
            }
            else if (to == ScriptRuntime.DateClass) {
                if (fromObj instanceof NativeDate) {
                    // This is a native date to java date conversion
                    return 1;
                }
            }
            else if (to.isInterface()) {
                if (fromObj instanceof NativeObject || fromObj instanceof NativeFunction) {
                    // See comments in createInterfaceAdapter
                    return 1;
                }
                return 12;
            }
            else if (to.isPrimitive() && to != Boolean.TYPE) {
                return 4 + getSizeRank(to);
            }
            break;
        }

        return CONVERSION_NONE;
    }

    static int getSizeRank(Class<?> aType) {
        if (aType == Double.TYPE) {
            return 1;
        }
        else if (aType == Float.TYPE) {
            return 2;
        }
        else if (aType == Long.TYPE) {
            return 3;
        }
        else if (aType == Integer.TYPE) {
            return 4;
        }
        else if (aType == Short.TYPE) {
            return 5;
        }
        else if (aType == Character.TYPE) {
            return 6;
        }
        else if (aType == Byte.TYPE) {
            return 7;
        }
        else if (aType == Boolean.TYPE) {
            return CONVERSION_NONE;
        }
        else {
            return 8;
        }
    }

    private static int getJSTypeCode(Object value) {
        if (value == null) {
            return JSTYPE_NULL;
        }
        else if (value == Undefined.instance) {
            return JSTYPE_UNDEFINED;
        }
        else if (value instanceof CharSequence) {
            return JSTYPE_STRING;
        }
        else if (value instanceof Number) {
            return JSTYPE_NUMBER;
        }
        else if (value instanceof Boolean) {
            return JSTYPE_BOOLEAN;
        }
        else if (value instanceof Scriptable) {
            if (value instanceof NativeJavaClass) {
                return JSTYPE_JAVA_CLASS;
            }
            else if (value instanceof NativeJavaArray) {
                return JSTYPE_JAVA_ARRAY;
            }
            else if (value instanceof Wrapper) {
                return JSTYPE_JAVA_OBJECT;
            }
            else {
                return JSTYPE_OBJECT;
            }
        }
        else if (value instanceof Class) {
            return JSTYPE_JAVA_CLASS;
        }
        else {
            Class<?> valueClass = value.getClass();
            if (valueClass.isArray()) {
                return JSTYPE_JAVA_ARRAY;
            }
            else {
                return JSTYPE_JAVA_OBJECT;
            }
        }
    }

    /**
     * Not intended for public use. Callers should use the
     * public API Context.toType.
     * @deprecated as of 1.5 Release 4
     * @see org.mozilla.javascript.Context#jsToJava(Object, Class)
     */
    @Deprecated
    public static Object coerceType(Class<?> type, Object value)
    {
        return coerceTypeImpl(type, value);
    }

    /**
     * Type-munging for field setting and method invocation.
     * Conforms to LC3 specification
     */
    static Object coerceTypeImpl(Class<?> type, Object value)
    {
        if (value != null && value.getClass() == type) {
            return value;
        }

        switch (getJSTypeCode(value)) {

        case JSTYPE_NULL:
            // raise error if type.isPrimitive()
            if (type.isPrimitive()) {
                reportConversionError(value, type);
            }
            return null;

        case JSTYPE_UNDEFINED:
            if (type == ScriptRuntime.StringClass ||
                type == ScriptRuntime.ObjectClass) {
                return "undefined";
            }
            else {
                reportConversionError("undefined", type);
            }
            break;

        case JSTYPE_BOOLEAN:
            // Under LC3, only JS Booleans can be coerced into a Boolean value
            if (type == Boolean.TYPE ||
                type == ScriptRuntime.BooleanClass ||
                type == ScriptRuntime.ObjectClass) {
                return value;
            }
            else if (type == ScriptRuntime.StringClass) {
                return value.toString();
            }
            else {
                reportConversionError(value, type);
            }
            break;

        case JSTYPE_NUMBER:
            if (type == ScriptRuntime.StringClass) {
                return ScriptRuntime.toString(value);
            }
            else if (type == ScriptRuntime.ObjectClass) {
                return coerceToNumber(Double.TYPE, value);
            }
            else if ((type.isPrimitive() && type != Boolean.TYPE) ||
                     ScriptRuntime.NumberClass.isAssignableFrom(type)) {
                return coerceToNumber(type, value);
            }
            else {
                reportConversionError(value, type);
            }
            break;

        case JSTYPE_STRING:
            if (type == ScriptRuntime.StringClass || type.isInstance(value)) {
                return value.toString();
            }
            else if (type == Character.TYPE
                     || type == ScriptRuntime.CharacterClass)
            {
                // Special case for converting a single char string to a
                // character
                // Placed here because it applies *only* to JS strings,
                // not other JS objects converted to strings
                if (((CharSequence)value).length() == 1) {
                    return Character.valueOf(((CharSequence)value).charAt(0));
                }
                else {
                    return coerceToNumber(type, value);
                }
            }
            else if ((type.isPrimitive() && type != Boolean.TYPE)
                     || ScriptRuntime.NumberClass.isAssignableFrom(type))
            {
                return coerceToNumber(type, value);
            }
            else {
                reportConversionError(value, type);
            }
            break;

        case JSTYPE_JAVA_CLASS:
            if (value instanceof Wrapper) {
                value = ((Wrapper)value).unwrap();
            }

            if (type == ScriptRuntime.ClassClass ||
                type == ScriptRuntime.ObjectClass) {
                return value;
            }
            else if (type == ScriptRuntime.StringClass) {
                return value.toString();
            }
            else {
                reportConversionError(value, type);
            }
            break;

        case JSTYPE_JAVA_OBJECT:
        case JSTYPE_JAVA_ARRAY:
            if (value instanceof Wrapper) {
              value = ((Wrapper)value).unwrap();
            }
            if (type.isPrimitive()) {
                if (type == Boolean.TYPE) {
                    reportConversionError(value, type);
                }
                return coerceToNumber(type, value);
            }
            else {
              if (type == ScriptRuntime.StringClass) {
                    return value.toString();
                }
                else {
                    if (type.isInstance(value)) {
                        return value;
                    }
                    else {
                        reportConversionError(value, type);
                    }
                }
            }
            break;

        case JSTYPE_OBJECT:
            if (type == ScriptRuntime.StringClass) {
                return ScriptRuntime.toString(value);
            }
            else if (type.isPrimitive()) {
                if (type == Boolean.TYPE) {
                    reportConversionError(value, type);
                }
                return coerceToNumber(type, value);
            }
            else if (type.isInstance(value)) {
                return value;
            }
            else if (type == ScriptRuntime.DateClass
                     && value instanceof NativeDate)
            {
                double time = ((NativeDate)value).getJSTimeValue();
                // XXX: This will replace NaN by 0
                return new Date((long)time);
            }
            else if (type.isArray() && value instanceof NativeArray) {
                // Make a new java array, and coerce the JS array components
                // to the target (component) type.
                NativeArray array = (NativeArray) value;
                long length = array.getLength();
                Class<?> arrayType = type.getComponentType();
                Object Result = Array.newInstance(arrayType, (int)length);
                for (int i = 0 ; i < length ; ++i) {
                    try  {
                        Array.set(Result, i, coerceTypeImpl(
                                arrayType, array.get(i, array)));
                    }
                    catch (EvaluatorException ee) {
                        reportConversionError(value, type);
                    }
                }

                return Result;
            }
            else if (value instanceof Wrapper) {
                value = ((Wrapper)value).unwrap();
                if (type.isInstance(value))
                    return value;
                reportConversionError(value, type);
            }
            else if (type.isInterface() && (value instanceof NativeObject
                    || value instanceof NativeFunction)) {
                // Try to use function/object as implementation of Java interface.
                return createInterfaceAdapter(type, (ScriptableObject) value);
            } else {
                reportConversionError(value, type);
            }
            break;
        }

        return value;
    }

    protected static Object createInterfaceAdapter(Class<?>type, ScriptableObject so) {
        // XXX: Currently only instances of ScriptableObject are
        // supported since the resulting interface proxies should
        // be reused next time conversion is made and generic
        // Callable has no storage for it. Weak references can
        // address it but for now use this restriction.

        Object key = Kit.makeHashKeyFromPair(COERCED_INTERFACE_KEY, type);
        Object old = so.getAssociatedValue(key);
        if (old != null) {
            // Function was already wrapped
            return old;
        }
        Context cx = Context.getContext();
        Object glue = InterfaceAdapter.create(cx, type, so);
        // Store for later retrieval
        glue = so.associateValue(key, glue);
        return glue;
    }

    private static Object coerceToNumber(Class<?> type, Object value)
    {
        Class<?> valueClass = value.getClass();

        // Character
        if (type == Character.TYPE || type == ScriptRuntime.CharacterClass) {
            if (valueClass == ScriptRuntime.CharacterClass) {
                return value;
            }
            return Character.valueOf((char)toInteger(value,
                                                 ScriptRuntime.CharacterClass,
                                                 Character.MIN_VALUE,
                                                 Character.MAX_VALUE));
        }

        // Double, Float
        if (type == ScriptRuntime.ObjectClass ||
            type == ScriptRuntime.DoubleClass || type == Double.TYPE) {
            return valueClass == ScriptRuntime.DoubleClass
                ? value
                : new Double(toDouble(value));
        }

        if (type == ScriptRuntime.FloatClass || type == Float.TYPE) {
            if (valueClass == ScriptRuntime.FloatClass) {
                return value;
            }
            else {
                double number = toDouble(value);
                if (Double.isInfinite(number) || Double.isNaN(number)
                    || number == 0.0) {
                    return new Float((float)number);
                }
                else {
                    double absNumber = Math.abs(number);
                    if (absNumber < Float.MIN_VALUE) {
                        return new Float((number > 0.0) ? +0.0 : -0.0);
                    }
                    else if (absNumber > Float.MAX_VALUE) {
                        return new Float((number > 0.0) ?
                                         Float.POSITIVE_INFINITY :
                                         Float.NEGATIVE_INFINITY);
                    }
                    else {
                        return new Float((float)number);
                    }
                }
            }
        }

        // Integer, Long, Short, Byte
        if (type == ScriptRuntime.IntegerClass || type == Integer.TYPE) {
            if (valueClass == ScriptRuntime.IntegerClass) {
                return value;
            }
            else {
                return Integer.valueOf((int)toInteger(value,
                                                  ScriptRuntime.IntegerClass,
                                                  Integer.MIN_VALUE,
                                                  Integer.MAX_VALUE));
            }
        }

        if (type == ScriptRuntime.LongClass || type == Long.TYPE) {
            if (valueClass == ScriptRuntime.LongClass) {
                return value;
            } else {
                /* Long values cannot be expressed exactly in doubles.
                 * We thus use the largest and smallest double value that
                 * has a value expressible as a long value. We build these
                 * numerical values from their hexidecimal representations
                 * to avoid any problems caused by attempting to parse a
                 * decimal representation.
                 */
                final double max = Double.longBitsToDouble(0x43dfffffffffffffL);
                final double min = Double.longBitsToDouble(0xc3e0000000000000L);
                return Long.valueOf(toInteger(value,
                                          ScriptRuntime.LongClass,
                                          min,
                                          max));
            }
        }

        if (type == ScriptRuntime.ShortClass || type == Short.TYPE) {
            if (valueClass == ScriptRuntime.ShortClass) {
                return value;
            }
            else {
                return Short.valueOf((short)toInteger(value,
                                                  ScriptRuntime.ShortClass,
                                                  Short.MIN_VALUE,
                                                  Short.MAX_VALUE));
            }
        }

        if (type == ScriptRuntime.ByteClass || type == Byte.TYPE) {
            if (valueClass == ScriptRuntime.ByteClass) {
                return value;
            }
            else {
                return Byte.valueOf((byte)toInteger(value,
                                                ScriptRuntime.ByteClass,
                                                Byte.MIN_VALUE,
                                                Byte.MAX_VALUE));
            }
        }

        return new Double(toDouble(value));
    }


    private static double toDouble(Object value)
    {
        if (value instanceof Number) {
            return ((Number)value).doubleValue();
        }
        else if (value instanceof String) {
            return ScriptRuntime.toNumber((String)value);
        }
        else if (value instanceof Scriptable) {
            if (value instanceof Wrapper) {
                // XXX: optimize tail-recursion?
                return toDouble(((Wrapper)value).unwrap());
            }
            else {
                return ScriptRuntime.toNumber(value);
            }
        }
        else {
            Method meth;
            try {
                meth = value.getClass().getMethod("doubleValue",
                		                          (Class [])null);
            }
            catch (NoSuchMethodException e) {
                meth = null;
            }
            catch (SecurityException e) {
                meth = null;
            }
            if (meth != null) {
                try {
                    return ((Number)meth.invoke(value,
                    		                    (Object [])null)).doubleValue();
                }
                catch (IllegalAccessException e) {
                    // XXX: ignore, or error message?
                    reportConversionError(value, Double.TYPE);
                }
                catch (InvocationTargetException e) {
                    // XXX: ignore, or error message?
                    reportConversionError(value, Double.TYPE);
                }
            }
            return ScriptRuntime.toNumber(value.toString());
        }
    }

    private static long toInteger(Object value, Class<?> type,
                                  double min, double max)
    {
        double d = toDouble(value);

        if (Double.isInfinite(d) || Double.isNaN(d)) {
            // Convert to string first, for more readable message
            reportConversionError(ScriptRuntime.toString(value), type);
        }

        if (d > 0.0) {
            d = Math.floor(d);
        }
        else {
            d = Math.ceil(d);
        }

        if (d < min || d > max) {
            // Convert to string first, for more readable message
            reportConversionError(ScriptRuntime.toString(value), type);
        }
        return (long)d;
    }

    static void reportConversionError(Object value, Class<?> type)
    {
        // It uses String.valueOf(value), not value.toString() since
        // value can be null, bug 282447.
        throw Context.reportRuntimeError2(
            "msg.conversion.not.allowed",
            String.valueOf(value),
            JavaMembers.javaSignature(type));
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        out.writeBoolean(isAdapter);
        if (isAdapter) {
            if (adapter_writeAdapterObject == null) {
                throw new IOException();
            }
            Object[] args = { javaObject, out };
            try {
                adapter_writeAdapterObject.invoke(null, args);
            } catch (Exception ex) {
                throw new IOException();
            }
        } else {
            out.writeObject(javaObject);
        }

        if (staticType != null) {
            out.writeObject(staticType.getClass().getName());
        } else {
            out.writeObject(null);
        }
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        isAdapter = in.readBoolean();
        if (isAdapter) {
            if (adapter_readAdapterObject == null)
                throw new ClassNotFoundException();
            Object[] args = { this, in };
            try {
                javaObject = adapter_readAdapterObject.invoke(null, args);
            } catch (Exception ex) {
                throw new IOException();
            }
        } else {
            javaObject = in.readObject();
        }

        String className = (String)in.readObject();
        if (className != null) {
            staticType = Class.forName(className);
        } else {
            staticType = null;
        }

        initMembers();
    }

    /**
     * The prototype of this object.
     */
    protected Scriptable prototype;

    /**
     * The parent scope of this object.
     */
    protected Scriptable parent;

    protected transient Object javaObject;

    protected transient Class<?> staticType;
    protected transient JavaMembers members;
    private transient Map<String,FieldAndMethods> fieldAndMethods;
    protected transient boolean isAdapter;

    private static final Object COERCED_INTERFACE_KEY = "Coerced Interface";
    private static Method adapter_writeAdapterObject;
    private static Method adapter_readAdapterObject;

    static {
        // Reflection in java is verbose
        Class<?>[] sig2 = new Class[2];
        Class<?> cl = Kit.classOrNull("org.mozilla.javascript.JavaAdapter");
        if (cl != null) {
            try {
                sig2[0] = ScriptRuntime.ObjectClass;
                sig2[1] = Kit.classOrNull("java.io.ObjectOutputStream");
                adapter_writeAdapterObject = cl.getMethod("writeAdapterObject",
                                                          sig2);

                sig2[0] = ScriptRuntime.ScriptableClass;
                sig2[1] = Kit.classOrNull("java.io.ObjectInputStream");
                adapter_readAdapterObject = cl.getMethod("readAdapterObject",
                                                         sig2);

            } catch (NoSuchMethodException e) {
                adapter_writeAdapterObject = null;
                adapter_readAdapterObject = null;
            }
        }
    }

}
