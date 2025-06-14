package org.mozilla.javascript;

/** Abstract operations for string manipulation as defined by EcmaScript */
public class AbstractEcmaStringOperations {
    /**
     * GetSubstitution(matched, str, position, captures, namedCaptures, replacementTemplate)
     *
     * <p><a
     * href="https://tc39.es/ecma262/multipage/text-processing.html#sec-getsubstitution">22.1.3.19.1
     * GetSubstitution (matched, str, position, captures, namedCaptures, replacementTemplate)</a>
     */
    public static String getSubstitution(
            Context cx,
            Scriptable scope,
            String matched,
            String str,
            int position,
            NativeArray capturesArray,
            Object namedCaptures,
            String replacementTemplate) {
        // See ECMAScript spec 22.1.3.19.1
        int stringLength = str.length();
        if (position > stringLength) Kit.codeBug();
        StringBuilder result = new StringBuilder();
        String templateRemainder = replacementTemplate;
        while (!templateRemainder.isEmpty()) {
            String ref = templateRemainder.substring(0, 1);
            String refReplacement = ref;

            if (templateRemainder.charAt(0) == '$') {
                if (templateRemainder.length() > 1) {
                    char c = templateRemainder.charAt(1);
                    switch (c) {
                        case '$':
                            ref = "$$";
                            refReplacement = "$";
                            break;

                        case '`':
                            ref = "$`";
                            refReplacement = str.substring(0, position);
                            break;

                        case '&':
                            ref = "$&";
                            refReplacement = matched;
                            break;

                        case '\'':
                            {
                                ref = "$'";
                                int matchLength = matched.length();
                                int tailPos = position + matchLength;
                                refReplacement = str.substring(Math.min(tailPos, stringLength));
                                break;
                            }

                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            {
                                int digitCount = 1;
                                if (templateRemainder.length() > 2) {
                                    char c2 = templateRemainder.charAt(2);
                                    if (isAsciiDigit(c2)) {
                                        digitCount = 2;
                                    }
                                }
                                String digits = templateRemainder.substring(1, 1 + digitCount);

                                // No need for ScriptRuntime version; we know the string is one or
                                // two characters and
                                // contains only [0-9]
                                int index = Integer.parseInt(digits);
                                long captureLen = capturesArray.getLength();
                                if (index > captureLen && digitCount == 2) {
                                    digitCount = 1;
                                    digits = digits.substring(0, 1);
                                    index = Integer.parseInt(digits);
                                }
                                ref = templateRemainder.substring(0, 1 + digitCount);
                                if (1 <= index && index <= captureLen) {
                                    Object capture = capturesArray.get(index - 1);
                                    if (capture
                                            == null) { // Undefined or missing are returned as null
                                        refReplacement = "";
                                    } else {
                                        refReplacement = ScriptRuntime.toString(capture);
                                    }
                                } else {
                                    refReplacement = ref;
                                }
                                break;
                            }

                        case '<':
                            {
                                int gtPos = templateRemainder.indexOf('>');
                                if (gtPos == -1 || Undefined.isUndefined(namedCaptures)) {
                                    ref = "$<";
                                    refReplacement = ref;
                                } else {
                                    ref = templateRemainder.substring(0, gtPos + 1);
                                    String groupName = templateRemainder.substring(2, gtPos);
                                    Object capture =
                                            ScriptRuntime.getObjectProp(
                                                    namedCaptures, groupName, cx, scope);
                                    if (Undefined.isUndefined(capture)) {
                                        refReplacement = "";
                                    } else {
                                        refReplacement = ScriptRuntime.toString(capture);
                                    }
                                }
                            }
                            break;
                    }
                }
            }

            int refLength = ref.length();
            templateRemainder = templateRemainder.substring(refLength);
            result.append(refReplacement);
        }
        return result.toString();
    }

    private static boolean isAsciiDigit(char c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return true;

            default:
                return false;
        }
    }
}
