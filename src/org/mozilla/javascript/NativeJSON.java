/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Matthew Crumley
 *   Raphael Speyer
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

import org.mozilla.javascript.json.JsonParser;

import java.util.Stack;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

/**
 * This class implements the JSON native object.
 * See ECMA 15.12.
 * @author Matthew Crumley, Raphael Speyer
 */
public final class NativeJSON extends IdScriptableObject
{
    static final long serialVersionUID = -4567599697595654984L;

    private static final Object JSON_TAG = "JSON";

    private static final int MAX_STRINGIFY_GAP_LENGTH = 10;

    static void init(Scriptable scope, boolean sealed)
    {
        NativeJSON obj = new NativeJSON();
        obj.activatePrototypeMap(MAX_ID);
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) { obj.sealObject(); }
        ScriptableObject.defineProperty(scope, "JSON", obj,
                                        ScriptableObject.DONTENUM);
    }

    private NativeJSON()
    {
    }

    @Override
    public String getClassName() { return "JSON"; }

    @Override
    protected void initPrototypeId(int id)
    {
        if (id <= LAST_METHOD_ID) {
            String name;
            int arity;
            switch (id) {
              case Id_toSource:  arity = 0; name = "toSource";  break;
              case Id_parse:     arity = 2; name = "parse";     break;
              case Id_stringify: arity = 3; name = "stringify"; break;
              default: throw new IllegalStateException(String.valueOf(id));
            }
            initPrototypeMethod(JSON_TAG, id, name, arity);
        } else {
            throw new IllegalStateException(String.valueOf(id));
        }
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(JSON_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int methodId = f.methodId();
        switch (methodId) {
            case Id_toSource:
                return "JSON";

            case Id_parse: {
                String jtext = ScriptRuntime.toString(args, 0);
                Object reviver = null;
                if (args.length > 1) {
                    reviver = args[1];
                }
                if (reviver instanceof Callable) {
                  return parse(cx, scope, jtext, (Callable) reviver);
                } else {
                  return parse(cx, scope, jtext);
                }
            }

            case Id_stringify: {
                Object value = null, replacer = null, space = null;
                switch (args.length) {
                    default:
                    case 3: space = args[2];
                    case 2: replacer = args[1];
                    case 1: value = args[0];
                    case 0:
                }
                return stringify(cx, scope, value, replacer, space);
            }

            default: throw new IllegalStateException(String.valueOf(methodId));
        }
    }

    private static Object parse(Context cx, Scriptable scope, String jtext) {
      try {
        return new JsonParser(cx, scope).parseValue(jtext);
      } catch (JsonParser.ParseException ex) {
        throw ScriptRuntime.constructError("SyntaxError", ex.getMessage());
      }
    }

    public static Object parse(Context cx, Scriptable scope, String jtext,
                               Callable reviver)
    {
      Object unfiltered = parse(cx, scope, jtext);
      Scriptable root = cx.newObject(scope);
      root.put("", root, unfiltered);
      return walk(cx, scope, reviver, root, "");
    }

    private static Object walk(Context cx, Scriptable scope, Callable reviver,
                               Scriptable holder, Object name)
    {
        final Object property;
        if (name instanceof Number) {
            property = holder.get( ((Number) name).intValue(), holder);
        } else {
            property = holder.get( ((String) name), holder);
        }

        if (property instanceof Scriptable) {
            Scriptable val = ((Scriptable) property);
            if (val instanceof NativeArray) {
                int len = (int) ((NativeArray) val).getLength();
                for (int i = 0; i < len; i++) {
                    Object newElement = walk(cx, scope, reviver, val, i);
                    if (newElement == Undefined.instance) {
                      val.delete(i);
                    } else {
                      val.put(i, val, newElement);
                    }
                }
            } else {
                Object[] keys = val.getIds();
                for (Object p : keys) {
                    Object newElement = walk(cx, scope, reviver, val, p);
                    if (newElement == Undefined.instance) {
                        if (p instanceof Number)
                          val.delete(((Number) p).intValue());
                        else
                          val.delete((String) p);
                    } else {
                        if (p instanceof Number)
                          val.put(((Number) p).intValue(), val, newElement);
                        else
                          val.put((String) p, val, newElement);
                    }
                }
            }
        }

        return reviver.call(cx, scope, holder, new Object[] { name, property });
    }

    private static String repeat(char c, int count) {
      char chars[] = new char[count];
      Arrays.fill(chars, c);
      return new String(chars);
    }

    private static class StringifyState {
        StringifyState(Context cx, Scriptable scope, String indent, String gap,
                       Callable replacer, List<Object> propertyList,
                       Object space)
        {
            this.cx = cx;
            this.scope = scope;

            this.indent = indent;
            this.gap = gap;
            this.replacer = replacer;
            this.propertyList = propertyList;
            this.space = space;
        }

        Stack<Scriptable> stack = new Stack<Scriptable>();
        Stack<Object> jstack = new Stack<Object>();
        // consider making completely static for better caching?
        HashMap<Class<?>,HashMap<String,Method>> beanMaps = new HashMap<Class<?>,HashMap<String,Method>>();
        HashMap<Class<?>,HashMap<String,Field>> fieldMaps = new HashMap<Class<?>,HashMap<String,Field>>();
        String indent;
        String gap;
        Callable replacer;
        List<Object> propertyList;
        Object space;

        Context cx;
        Scriptable scope;
    }

    public static Object stringify(Context cx, Scriptable scope, Object value,
                                   Object replacer, Object space)
    {
        String indent = "";
        String gap = "";

        List<Object> propertyList = null;
        Callable replacerFunction = null;

        if (replacer instanceof Callable) {
          replacerFunction = (Callable) replacer;
        } else if (replacer instanceof NativeArray) {
          propertyList = new LinkedList<Object>();
          NativeArray replacerArray = (NativeArray) replacer;
          for (int i : replacerArray.getIndexIds()) {
            Object v = replacerArray.get(i, replacerArray);
            if (v instanceof String || v instanceof Number) {
              propertyList.add(v);
            } else if (v instanceof NativeString || v instanceof NativeNumber) {
              propertyList.add(ScriptRuntime.toString(v));
            }
          }
        }

        if (space instanceof NativeNumber) {
            space = ScriptRuntime.toNumber(space);
        } else if (space instanceof NativeString) {
            space = ScriptRuntime.toString(space);
        }

        if (space instanceof Number) {
            int gapLength = (int) ScriptRuntime.toInteger(space);
            gapLength = Math.min(MAX_STRINGIFY_GAP_LENGTH, gapLength);
            gap = (gapLength > 0) ? repeat(' ', gapLength) : "";
            space = gapLength;
        } else if (space instanceof String) {
            gap = (String) space;
            if (gap.length() > MAX_STRINGIFY_GAP_LENGTH) {
              gap = gap.substring(0, MAX_STRINGIFY_GAP_LENGTH);
            }
        }

        StringifyState state = new StringifyState(cx, scope,
            indent,
            gap,
            replacerFunction,
            propertyList,
            space);

        ScriptableObject wrapper = new NativeObject();
        wrapper.setParentScope(scope);
        wrapper.setPrototype(ScriptableObject.getObjectPrototype(scope));
        wrapper.defineProperty("", value, 0);
        return str("", wrapper, state);
    }

    private static Object str(Object key, Scriptable holder,
                              StringifyState state)
    {
        Object value = null;
        if (key instanceof String) {
            value = getProperty(holder, (String) key);
        } else {
            value = getProperty(holder, ((Number) key).intValue());
        }

        if (value instanceof NativeJavaArray)
            return jja(((NativeJavaArray) value).unwrap(), state);
        if (value instanceof NativeJavaObject)
            return jo((Scriptable) value, state);
        if (value instanceof Scriptable) {
            Object toJSON = getProperty((Scriptable) value, "toJSON");
            if (toJSON instanceof Callable) {
                value = callMethod(state.cx, (Scriptable) value, "toJSON",
                                   new Object[] { key });
            }
        }

        if (state.replacer != null) {
            value = state.replacer.call(state.cx, state.scope, holder,
                                        new Object[] { key, value });
        }


        if (value instanceof NativeNumber) {
            value = ScriptRuntime.toNumber(value);
        } else if (value instanceof NativeString) {
            value = ScriptRuntime.toString(value);
        } else if (value instanceof NativeBoolean) {
            value = ((NativeBoolean) value).getDefaultValue(ScriptRuntime.BooleanClass);
        }

        if (value == null) return "null";
        if (value.equals(Boolean.TRUE)) return "true";
        if (value.equals(Boolean.FALSE)) return "false";

        if (value instanceof String) {
            return quote((String) value);
        }

        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == d && d != Double.POSITIVE_INFINITY &&
                d != Double.NEGATIVE_INFINITY)
            {
                return ScriptRuntime.toString(value);
            } else {
                return "null";
            }
        }

        if (value instanceof Scriptable && !(value instanceof Callable)) {
            if (value instanceof NativeArray) {
                return ja((NativeArray) value, state);
            }
            return jo((Scriptable) value, state);
        }

        return Undefined.instance;
    }

    private static String join(Collection<Object> objs, String delimiter) {
        if (objs == null || objs.isEmpty()) {
            return "";
        }
        Iterator<Object> iter = objs.iterator();
        if (!iter.hasNext()) return "";
        StringBuilder builder = new StringBuilder(iter.next().toString());
        while (iter.hasNext()) {
            builder.append(delimiter).append(iter.next().toString());
        }
        return builder.toString();
    }

    private static String toBeanName(String name) {
        if (name == null || name.length() == 0)
            return name;

        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
                && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
    private static HashMap<String,Method> beanMap(Object o, StringifyState s) {
        Class<?> clazz = o.getClass();
        if (s.beanMaps.containsKey(clazz))
            return s.beanMaps.get(clazz);
        HashMap<String,Method> clazzBeanMap;

        if (clazz != Class.class)
            clazzBeanMap = beanMap(Class.class, s);
        else
            clazzBeanMap = new HashMap<String,Method>();

        Method[] methods = clazz.getMethods();
        HashMap<String,Method> beanMap = new HashMap<String,Method>();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers()))
                continue;
            String name = m.getName();
            if (name.startsWith("is")
                    && Character.isUpperCase(name.charAt(2))) {
                Class<?> rtype = m.getReturnType();
                if ((rtype == Boolean.class || rtype == boolean.class)
                        && m.getParameterTypes().length == 0) {
                    String beanProp = toBeanName(name.substring(2));
                    if (!clazzBeanMap.containsKey(beanProp))
                        beanMap.put(beanProp, m);
                }
            }
            if (name.startsWith("get")
                    && Character.isUpperCase(name.charAt(3))
                    && m.getParameterTypes().length == 0) {
                String beanProp = toBeanName(name.substring(3));
                if (!clazzBeanMap.containsKey(beanProp))
                    beanMap.put(beanProp, m);
            }
        }
        s.beanMaps.put(clazz, beanMap);
        return beanMap;
    }
    private static HashMap<String,Field> fieldMap(Object o, StringifyState s) {
        Class<?> clazz = o.getClass();
        if (s.fieldMaps.containsKey(clazz))
            return s.fieldMaps.get(clazz);
        Field[] fields = clazz.getFields();
        HashMap<String,Field> fieldMap = new HashMap<String,Field>();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()))
                continue;
            String name = f.getName();
            fieldMap.put(name, f);
        }
        s.fieldMaps.put(clazz, fieldMap);
        return fieldMap;
    }
    private static String mjo(Map value, StringifyState state) {
        if (value == null)
            return "null";

        state.jstack.push(value);

        String stepback = state.indent;
        state.indent = state.indent + state.gap;
        List<Object> partial = new LinkedList<Object>();

        for (Object p : value.keySet()) {
            Object strP = jjo(value.get(p), state);
            if (strP != null) {
                String member = quote(p.toString()) + ":";
                if (state.gap.length() > 0) {
                    member = member + " ";
                }
                member = member + strP;
                partial.add(member);
            }
        }

        final String finalValue;

        if (partial.isEmpty()) {
            finalValue = "{}";
        } else {
            if (state.gap.length() == 0) {
                finalValue = '{' + join(partial, ",") + '}';
            } else {
                String separator = ",\n" + state.indent;
                String properties = join(partial, separator);
                finalValue = "{\n" + state.indent + properties + '\n' +
                    stepback + '}';
            }
        }

        state.jstack.pop();
        state.indent = stepback;
        return finalValue;
    }
    private static String jjo(Object value, StringifyState state) {

        if (value == null)
            return "null";

        // serialize a Map as if it were an object with properties
        if (value instanceof Map)
            return mjo((Map) value, state);

        // serialize it as if it were an array
        if (value instanceof Collection)
            return cja((Collection) value, state);

        // boolean values and numbers may be unquoted
        if (value instanceof Boolean || value instanceof Number)
            return value.toString();

        Class<?> clazz = value.getClass();
        // primitives get away with no quoting
        if (clazz.isPrimitive() && clazz != char.class)
            return value.toString();

        // an array
        if (clazz.isArray())
            return jja(value, state);

        // assume everything in java.lang.** to be a quoted string
        if (clazz.getName().startsWith("java.lang."))
            return quote(value.toString());

        state.jstack.push(value);

        String stepback = state.indent;
        state.indent = state.indent + state.gap;

        Map<String,Method> beanMap = beanMap(value, state);
        Map<String,Field> fieldMap = fieldMap(value, state);
        Map<Method,String> nameMap = new IdentityHashMap<Method,String>();
        for (String p : beanMap.keySet())
            nameMap.put(beanMap.get(p), p);

        List<Object> partial = new LinkedList<Object>();
        LinkedList<Member> members = new LinkedList<Member>();

        members.addAll(beanMap.values());
        members.addAll(fieldMap.values());

        for (Member m : members) {
            Object beanVal = null;
            String name = m.getName();
            try {
                if (m instanceof Method) {
                    beanVal = ((Method) m).invoke(value);
                    name = nameMap.get((Method) m);
                } else if (m instanceof Field) {
                    beanVal = ((Field) m).get(value);
                } else
                    throw new IllegalArgumentException(m.getClass().getName());
                // remove cyclic references
                if (state.jstack.search(beanVal) != -1)
                    continue;
            }
            catch (IllegalAccessException e) { continue; }
            catch (InvocationTargetException e) { continue; }
            Object strP = jjo(beanVal, state);
            if (strP != null) {
                String member = quote(name) + ":";
                if (state.gap.length() > 0) {
                    member = member + " ";
                }
                member = member + strP;
                partial.add(member);
            }
        }

        final String finalValue;

        if (partial.isEmpty()) {
            finalValue = "{}";
        } else {
            if (state.gap.length() == 0) {
                finalValue = '{' + join(partial, ",") + '}';
            } else {
                String separator = ",\n" + state.indent;
                String properties = join(partial, separator);
                finalValue = "{\n" + state.indent + properties + '\n' +
                    stepback + '}';
            }
        }

        state.jstack.pop();
        state.indent = stepback;
        return finalValue;
    }

    private static String jo(Scriptable value, StringifyState state) {
        if (value instanceof NativeJavaObject) {
            Object v = ((NativeJavaObject)value).unwrap();
            return jjo(v, state);
        }

        if (state.stack.search(value) != -1) {
            throw ScriptRuntime.typeError0("msg.cyclic.value");
        }
        state.stack.push(value);

        String stepback = state.indent;
        state.indent = state.indent + state.gap;
        Object[] k = null;
        if (state.propertyList != null) {
            k = state.propertyList.toArray();
        } else {
            k = value.getIds();
        }

        List<Object> partial = new LinkedList<Object>();

        for (Object p : k) {
            Object strP = str(p, value, state);
            if (strP != Undefined.instance) {
                String member = quote(p.toString()) + ":";
                if (state.gap.length() > 0) {
                    member = member + " ";
                }
                member = member + strP;
                partial.add(member);
            }
        }

        final String finalValue;

        if (partial.isEmpty()) {
            finalValue = "{}";
        } else {
            if (state.gap.length() == 0) {
                finalValue = '{' + join(partial, ",") + '}';
            } else {
                String separator = ",\n" + state.indent;
                String properties = join(partial, separator);
                finalValue = "{\n" + state.indent + properties + '\n' +
                    stepback + '}';
            }
        }

        state.stack.pop();
        state.indent = stepback;
        return finalValue;
    }

    private static String ja(NativeArray value, StringifyState state) {
        if (state.stack.search(value) != -1) {
            throw ScriptRuntime.typeError0("msg.cyclic.value");
        }
        state.stack.push(value);

        String stepback = state.indent;
        state.indent = state.indent + state.gap;
        List<Object> partial = new LinkedList<Object>();

        int len = (int) value.getLength();
        for (int index = 0; index < len; index++) {
            Object strP = str(index, value, state);
            if (strP == Undefined.instance) {
                partial.add("null");
            } else {
                partial.add(strP);
            }
        }

        final String finalValue;

        if (partial.isEmpty()) {
            finalValue = "[]";
        } else {
            if (state.gap.length() == 0) {
                finalValue = '[' + join(partial, ",") + ']';
            } else {
                String separator = ",\n" + state.indent;
                String properties = join(partial, separator);
                finalValue = "[\n" + state.indent + properties + '\n' + stepback + ']';
            }
        }

        state.stack.pop();
        state.indent = stepback;
        return finalValue;
    }

    private static String jja(Object value, StringifyState state) {
        if (state.jstack.search(value) != -1) {
            throw ScriptRuntime.typeError0("msg.cyclic.value");
        }
        state.jstack.push(value);

        String stepback = state.indent;
        state.indent = state.indent + state.gap;
        List<Object> partial = new LinkedList<Object>();

        int len = Array.getLength(value);
        for (int index = 0; index < len; index++) {
            Object strP = jjo(Array.get(value, index), state);
            if (strP == null) {
                partial.add("null");
            } else {
                partial.add(strP);
            }
        }

        final String finalValue;

        if (partial.isEmpty()) {
            finalValue = "[]";
        } else {
            if (state.gap.length() == 0) {
                finalValue = '[' + join(partial, ",") + ']';
            } else {
                String separator = ",\n" + state.indent;
                String properties = join(partial, separator);
                finalValue = "[\n" + state.indent + properties + '\n' + stepback + ']';
            }
        }

        state.jstack.pop();
        state.indent = stepback;
        return finalValue;
    }
    private static String cja(Collection value, StringifyState state) {
        if (state.jstack.search(value) != -1) {
            throw ScriptRuntime.typeError0("msg.cyclic.value");
        }
        state.jstack.push(value);

        String stepback = state.indent;
        state.indent = state.indent + state.gap;
        List<Object> partial = new LinkedList<Object>();

        for (Object o : value) {
            Object strP = jjo(o, state);
            if (strP == null) {
                partial.add("null");
            } else {
                partial.add(strP);
            }
        }

        final String finalValue;

        if (partial.isEmpty()) {
            finalValue = "[]";
        } else {
            if (state.gap.length() == 0) {
                finalValue = '[' + join(partial, ",") + ']';
            } else {
                String separator = ",\n" + state.indent;
                String properties = join(partial, separator);
                finalValue = "[\n" + state.indent + properties + '\n' + stepback + ']';
            }
        }

        state.jstack.pop();
        state.indent = stepback;
        return finalValue;
    }

    private static String quote(String string) {
        StringBuffer product = new StringBuffer(string.length()+2); // two extra chars for " on either side
        product.append('"');
        int length = string.length();
        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"':
                    product.append("\\\"");
                    break;
                case '\\':
                    product.append("\\\\");
                    break;
                case '\b':
                    product.append("\\b");
                    break;
                case '\f':
                    product.append("\\f");
                    break;
                case '\n':
                    product.append("\\n");
                    break;
                case '\r':
                    product.append("\\r");
                    break;
                case '\t':
                    product.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        product.append("\\u");
                        String hex = String.format("%04x", (int) c);
                        product.append(hex);
                    }
                    else {
                        product.append(c);
                    }
                    break;
            }
        }
        product.append('"');
        return product.toString();
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2009-05-25 16:01:00 EDT
        {   id = 0; String X = null;
            L: switch (s.length()) {
            case 5: X="parse";id=Id_parse; break L;
            case 8: X="toSource";id=Id_toSource; break L;
            case 9: X="stringify";id=Id_stringify; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_toSource     = 1,
        Id_parse        = 2,
        Id_stringify    = 3,
        LAST_METHOD_ID  = 3,
        MAX_ID          = 3;

// #/string_id_map#
}
