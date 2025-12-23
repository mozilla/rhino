package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

/** Abstract operations for string manipulation as defined by EcmaScript */
public class AbstractEcmaStringOperations {

    public static List<ReplacementOperation> buildReplacementList(String replacementTemplate) {
        List<ReplacementOperation> ops = new ArrayList<>();
        int position = 0;
        int start = 0;

        while (position < replacementTemplate.length()) {
            if (replacementTemplate.charAt(position) == '$') {
                if (position < (replacementTemplate.length() - 1)) {
                    if (start != position) {
                        ops.add(
                                new LiteralReplacement(
                                        replacementTemplate.substring(start, position)));
                    }
                    String ref = replacementTemplate.substring(position, position + 1);
                    char c = replacementTemplate.charAt(position + 1);
                    switch (c) {
                        case '$':
                            ref = "$$";
                            ops.add(new LiteralReplacement("$"));
                            break;

                        case '`':
                            ref = "$`";
                            ops.add(new FromStartToMatchReplacement());
                            break;

                        case '&':
                            ref = "$&";
                            ops.add(new MatchedReplacement());
                            break;

                        case '\'':
                            {
                                ref = "$'";
                                ops.add(new FromMatchToEndReplacement());
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
                                if (replacementTemplate.length() > position + 2) {
                                    char c2 = replacementTemplate.charAt(position + 2);
                                    if (isAsciiDigit(c2)) {
                                        digitCount = 2;
                                    }
                                }
                                String digits =
                                        replacementTemplate.substring(
                                                position + 1, position + 1 + digitCount);
                                ref =
                                        replacementTemplate.substring(
                                                position, position + 1 + digitCount);

                                // No need for ScriptRuntime version; we know the string is one or
                                // two characters and
                                // contains only [0-9]
                                int index = Integer.parseInt(digits);
                                if (digits.length() == 1) {
                                    ops.add(new OneDigitCaptureReplacement(index));
                                } else {
                                    ops.add(new TwoDigitCaptureReplacement(index));
                                }
                                break;
                            }

                        case '<':
                            {
                                int gtPos = replacementTemplate.indexOf('>', position + 2);
                                if (gtPos == -1) {
                                    ref = "$<";
                                    ops.add(new LiteralReplacement(ref));
                                } else {
                                    ref = replacementTemplate.substring(position, gtPos + 1);
                                    String groupName =
                                            replacementTemplate.substring(position + 2, gtPos);
                                    ops.add(new NamedCaptureReplacement(groupName));
                                }
                            }
                            break;
                        default:
                            ops.add(new LiteralReplacement(ref));
                            break;
                    }
                    position += ref.length();
                    start = position;
                } else {
                    position++;
                }
            } else {
                position++;
            }
        }
        if (start != position) {
            ops.add(new LiteralReplacement(replacementTemplate.substring(start, position)));
        }
        return ops;
    }

    public static <T> String getSubstitution(
            Context cx,
            Scriptable scope,
            String matched,
            String str,
            int position,
            List<T> capturesList,
            Object namedCaptures,
            List<ReplacementOperation> replacementTemplate) {
        if (position > str.length()) Kit.codeBug();
        StringBuilder result = new StringBuilder();
        for (var op : replacementTemplate) {
            result.append(
                    op.replacement(cx, scope, matched, str, position, capturesList, namedCaptures));
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

    public abstract static class ReplacementOperation {

        abstract <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures);
    }

    private static class LiteralReplacement extends ReplacementOperation {

        private final String replacement;

        LiteralReplacement(String replacement) {
            this.replacement = replacement;
        }

        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            return replacement;
        }
    }

    private static class OneDigitCaptureReplacement extends ReplacementOperation {
        private final int capture;

        OneDigitCaptureReplacement(int capture) {
            this.capture = capture;
        }

        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            if (capture >= 1 && capture < captures.size()) {
                var v = captures.get(capture);
                if (v == null || v == Undefined.instance) {
                    return "";
                } else {
                    return v.toString();
                }
            } else {
                return "$" + Integer.toString(capture);
            }
        }
    }

    private static class TwoDigitCaptureReplacement extends ReplacementOperation {
        private final int capture;

        TwoDigitCaptureReplacement(int capture) {
            this.capture = capture;
        }

        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            int i = capture;
            if (i > 9 && i >= captures.size() && i / 10 < captures.size()) {
                i = i / 10; // Just take the first digit.
                var v = captures.get(i);
                if (v == null || v == Undefined.instance) {
                    return "" + Integer.toString(capture % 10);
                } else {
                    return v.toString() + Integer.toString(capture % 10);
                }
            } else if (i >= 1 && i < captures.size()) {
                var v = captures.get(i);
                if (v == null || v == Undefined.instance) {
                    return "";
                } else {
                    return v.toString();
                }
            } else {
                return (capture >= 10 ? "$" : "$0") + Integer.toString(capture);
            }
        }
    }

    private static class FromStartToMatchReplacement extends ReplacementOperation {
        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            return str.substring(0, position);
        }
    }

    private static class MatchedReplacement extends ReplacementOperation {
        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            return matched;
        }
    }

    private static class FromMatchToEndReplacement extends ReplacementOperation {
        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            int matchLength = matched.length();
            int tailPos = position + matchLength;
            return str.substring(Math.min(str.length(), tailPos));
        }
    }

    private static class NamedCaptureReplacement extends ReplacementOperation {
        final String groupName;

        NamedCaptureReplacement(String groupName) {
            this.groupName = groupName;
        }

        @Override
        <T> String replacement(
                Context cx,
                Scriptable scope,
                String matched,
                String str,
                int position,
                List<T> captures,
                Object namedCaptures) {
            if (Undefined.isUndefined(namedCaptures)) {
                List<ReplacementOperation> ops = buildReplacementList(groupName);
                return "$<"
                        + getSubstitution(
                                cx, scope, matched, str, position, captures, namedCaptures, ops)
                        + ">";
            }

            Object capture = ScriptRuntime.getObjectProp(namedCaptures, groupName, cx, scope);
            if (Undefined.isUndefined(capture)) {
                return "";
            } else {
                return ScriptRuntime.toString(capture);
            }
        }
    }
}
