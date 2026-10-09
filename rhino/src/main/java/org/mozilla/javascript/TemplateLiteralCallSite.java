package org.mozilla.javascript;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import org.mozilla.classfile.DynamicConstant;

public class TemplateLiteralCallSite implements DynamicConstant {
    /*
     * The values and raw values are each encoded as a single string so that they can be carried
     * as bootstrap arguments. Components are separated by SEPARATOR. Within a string component
     * SEPARATOR is written as ESCAPE '0' and ESCAPE as ESCAPE '1', so a component consisting of a
     * lone ESCAPE can only mean Undefined.instance, and an empty component the empty string.
     */
    private static final char SEPARATOR = '\0';
    private static final char ESCAPE = '\1';

    private volatile Scriptable callSite;
    private final Object[] values;
    private final String[] rawValues;

    public TemplateLiteralCallSite(Object[] values, String[] rawValues) {
        assert (values.length == rawValues.length);
        this.values = values;
        this.rawValues = rawValues;
    }

    public String encodedValues() {
        return encode(values);
    }

    public String encodedRawValues() {
        return encode(rawValues);
    }

    /**
     * Bootstrap method for the dynamic constant described by {@link
     * org.mozilla.javascript.optimizer.TemplateLiteralCallSiteDescriber}. The hash only serves to
     * keep distinct call sites with the same contents in distinct constants.
     */
    public static TemplateLiteralCallSite templateLiteralCallSiteConstant(
            MethodHandles.Lookup lookup,
            String name,
            Class<?> type,
            String values,
            String rawValues,
            int dedupHash) {
        Object[] raw = decode(rawValues);
        return new TemplateLiteralCallSite(
                decode(values), Arrays.copyOf(raw, raw.length, String[].class));
    }

    private static String encode(Object[] components) {
        var sb = new StringBuilder();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            if (Undefined.isUndefined(components[i])) {
                sb.append(ESCAPE);
                continue;
            }
            String s = (String) components[i];
            for (int j = 0; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == SEPARATOR) {
                    sb.append(ESCAPE).append('0');
                } else if (c == ESCAPE) {
                    sb.append(ESCAPE).append('1');
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /** Decodes components; a template always has at least one, so "" is one empty string. */
    private static Object[] decode(String encoded) {
        var components = new ArrayList<Object>();
        int n = encoded.length();
        int i = 0;
        while (true) {
            if (i < n
                    && encoded.charAt(i) == ESCAPE
                    && (i + 1 == n || encoded.charAt(i + 1) == SEPARATOR)) {
                components.add(Undefined.instance);
                i++;
            } else {
                var sb = new StringBuilder();
                while (i < n && encoded.charAt(i) != SEPARATOR) {
                    char c = encoded.charAt(i++);
                    if (c == ESCAPE) {
                        c = encoded.charAt(i++) == '0' ? SEPARATOR : ESCAPE;
                    }
                    sb.append(c);
                }
                components.add(sb.toString());
            }
            if (i == n) {
                break;
            }
            i++;
        }
        return components.toArray();
    }

    public Scriptable getSiteObject(Context cx, VarScope scope) {
        if (callSite == null) {
            synchronized (this) {
                if (callSite == null) {
                    callSite = initSiteObject(cx, scope);
                }
            }
        }
        return callSite;
    }

    public Scriptable initSiteObject(Context cx, VarScope scope) {
        ScriptableObject siteObj = (ScriptableObject) cx.newArray(scope, values.length);
        ScriptableObject rawObj = (ScriptableObject) cx.newArray(scope, rawValues.length);

        siteObj.put("raw", siteObj, rawObj);
        siteObj.setAttributes("raw", ScriptableObject.DONTENUM);

        for (int i = 0; i < values.length; i++) {
            siteObj.put(i, siteObj, values[i]);

            rawObj.put(i, rawObj, rawValues[i]);
        }

        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, rawObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, siteObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);

        return siteObj;
    }
}
