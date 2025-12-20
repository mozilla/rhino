/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Unicode properties handler for regular expressions.
 *
 * <p>Provides lookups for Unicode properties: binary properties (Alphabetic, Emoji, etc.), general
 * categories (Letter, Number, etc.), and scripts. Uses Java's Character class and ICU4J.
 *
 * <p>See ECMA 262 §22.2.1.
 */
public class UnicodeProperties {

    // ==================================================================================
    // THREAD-LOCAL REUSABLE OBJECTS
    // ==================================================================================
    //
    // NOTE: Previously used ThreadLocal<BitSet> for Script_Extensions lookups, but this
    // caused a memory leak. BitSet.clear() doesn't shrink the internal word array, so it
    // grew unbounded across 105k+ tests, eventually causing OutOfMemoryError.
    //
    // Solution: Allocate new BitSet per call. The performance cost is negligible compared
    // to the memory leak (BitSet allocation is ~100ns, test suite runs for minutes).

    // ==================================================================================
    // REGEX PATTERNS
    // ==================================================================================

    /**
     * Pattern for parsing property escapes in the form "propName" or "propName=propValue".
     * Validates that property names and values contain only alphanumeric characters and
     * underscores.
     */
    private static final java.util.regex.Pattern PROPERTY_PATTERN =
            java.util.regex.Pattern.compile(
                    "^(?<propName>[a-zA-Z_]+)(?:=(?<propValue>[a-zA-Z_0-9]+))?$");

    // ==================================================================================
    // PROPERTY CONSTANTS
    // ==================================================================================

    // --------------------------------------------------
    // Binary Properties (ECMA-262 compliant)
    // --------------------------------------------------
    public static final byte ALPHABETIC = 1;
    public static final byte ASCII = ALPHABETIC + 1;
    public static final byte CASE_IGNORABLE = ASCII + 1;
    public static final byte ASCII_HEX_DIGIT = CASE_IGNORABLE + 1;
    public static final byte HEX_DIGIT = ASCII_HEX_DIGIT + 1;
    public static final byte ID_CONTINUE = HEX_DIGIT + 1;
    public static final byte ID_START = ID_CONTINUE + 1;
    public static final byte LOWERCASE = ID_START + 1;
    public static final byte UPPERCASE = LOWERCASE + 1;
    public static final byte WHITE_SPACE = UPPERCASE + 1;

    // --------------------------------------------------
    // Extended Binary Properties (Java Character API)
    // --------------------------------------------------
    public static final byte ANY = WHITE_SPACE + 1;
    public static final byte ASSIGNED = ANY + 1;
    public static final byte XID_START = ASSIGNED + 1;
    public static final byte XID_CONTINUE = XID_START + 1;
    public static final byte CASED = XID_CONTINUE + 1;
    public static final byte IDEOGRAPHIC = CASED + 1;
    public static final byte MATH = IDEOGRAPHIC + 1;
    public static final byte DASH = MATH + 1;
    public static final byte NONCHARACTER_CODE_POINT = DASH + 1;
    public static final byte BIDI_MIRRORED = NONCHARACTER_CODE_POINT + 1;
    public static final byte PATTERN_WHITE_SPACE = BIDI_MIRRORED + 1;
    public static final byte GRAPHEME_BASE = PATTERN_WHITE_SPACE + 1;
    public static final byte GRAPHEME_EXTEND = GRAPHEME_BASE + 1;
    public static final byte EXTENDER = GRAPHEME_EXTEND + 1;
    public static final byte PATTERN_SYNTAX = EXTENDER + 1;
    public static final byte JOIN_CONTROL = PATTERN_SYNTAX + 1;
    public static final byte CHANGES_WHEN_LOWERCASED = JOIN_CONTROL + 1;
    public static final byte CHANGES_WHEN_UPPERCASED = CHANGES_WHEN_LOWERCASED + 1;
    public static final byte CHANGES_WHEN_TITLECASED = CHANGES_WHEN_UPPERCASED + 1;
    public static final byte CHANGES_WHEN_CASEFOLDED = CHANGES_WHEN_TITLECASED + 1;
    public static final byte CHANGES_WHEN_CASEMAPPED = CHANGES_WHEN_CASEFOLDED + 1;
    public static final byte CHANGES_WHEN_NFKC_CASEFOLDED = CHANGES_WHEN_CASEMAPPED + 1;

    // --------------------------------------------------
    // ICU4J-based Binary Properties (Unicode 15.0+)
    // --------------------------------------------------
    public static final byte DEFAULT_IGNORABLE_CODE_POINT = CHANGES_WHEN_NFKC_CASEFOLDED + 1;
    public static final byte EMOJI = DEFAULT_IGNORABLE_CODE_POINT + 1;
    public static final byte EMOJI_PRESENTATION = EMOJI + 1;
    public static final byte EMOJI_MODIFIER = EMOJI_PRESENTATION + 1;
    public static final byte EMOJI_MODIFIER_BASE = EMOJI_MODIFIER + 1;
    public static final byte EMOJI_COMPONENT = EMOJI_MODIFIER_BASE + 1;
    public static final byte EXTENDED_PICTOGRAPHIC = EMOJI_COMPONENT + 1;
    public static final byte REGIONAL_INDICATOR = EXTENDED_PICTOGRAPHIC + 1;
    public static final byte QUOTATION_MARK = REGIONAL_INDICATOR + 1;
    public static final byte SENTENCE_TERMINAL = QUOTATION_MARK + 1;
    public static final byte TERMINAL_PUNCTUATION = SENTENCE_TERMINAL + 1;
    public static final byte VARIATION_SELECTOR = TERMINAL_PUNCTUATION + 1;
    public static final byte RADICAL = VARIATION_SELECTOR + 1;
    public static final byte UNIFIED_IDEOGRAPH = RADICAL + 1;
    public static final byte DEPRECATED = UNIFIED_IDEOGRAPH + 1;
    public static final byte SOFT_DOTTED = DEPRECATED + 1;
    public static final byte LOGICAL_ORDER_EXCEPTION = SOFT_DOTTED + 1;
    public static final byte DIACRITIC = LOGICAL_ORDER_EXCEPTION + 1;
    public static final byte IDS_BINARY_OPERATOR = DIACRITIC + 1;
    public static final byte IDS_TRINARY_OPERATOR = IDS_BINARY_OPERATOR + 1;

    // --------------------------------------------------
    // Non-Binary Properties
    // --------------------------------------------------
    public static final byte GENERAL_CATEGORY = IDS_TRINARY_OPERATOR + 1;
    public static final byte SCRIPT = GENERAL_CATEGORY + 1;
    public static final byte SCRIPT_EXTENSIONS = SCRIPT + 1;

    // --------------------------------------------------
    // General Category Values (PropertyValueAliases.txt)
    // --------------------------------------------------
    public static final byte OTHER = 1;
    public static final byte CONTROL = OTHER + 1;
    public static final byte FORMAT = CONTROL + 1;
    public static final byte UNASSIGNED = FORMAT + 1;
    public static final byte PRIVATE_USE = UNASSIGNED + 1;
    public static final byte SURROGATE = PRIVATE_USE + 1;
    public static final byte LETTER = SURROGATE + 1;
    public static final byte LOWERCASE_LETTER = LETTER + 1;
    public static final byte MODIFIER_LETTER = LOWERCASE_LETTER + 1;
    public static final byte OTHER_LETTER = MODIFIER_LETTER + 1;
    public static final byte TITLECASE_LETTER = OTHER_LETTER + 1;
    public static final byte UPPERCASE_LETTER = TITLECASE_LETTER + 1;
    public static final byte MARK = UPPERCASE_LETTER + 1;
    public static final byte SPACING_MARK = MARK + 1;
    public static final byte ENCLOSING_MARK = SPACING_MARK + 1;
    public static final byte NONSPACING_MARK = ENCLOSING_MARK + 1;
    public static final byte NUMBER = NONSPACING_MARK + 1;
    public static final byte DECIMAL_NUMBER = NUMBER + 1;
    public static final byte LETTER_NUMBER = DECIMAL_NUMBER + 1;
    public static final byte OTHER_NUMBER = LETTER_NUMBER + 1;
    public static final byte PUNCTUATION = OTHER_NUMBER + 1;
    public static final byte CONNECTOR_PUNCTUATION = PUNCTUATION + 1;
    public static final byte DASH_PUNCTUATION = CONNECTOR_PUNCTUATION + 1;
    public static final byte CLOSE_PUNCTUATION = DASH_PUNCTUATION + 1;
    public static final byte FINAL_PUNCTUATION = CLOSE_PUNCTUATION + 1;
    public static final byte INITIAL_PUNCTUATION = FINAL_PUNCTUATION + 1;
    public static final byte OTHER_PUNCTUATION = INITIAL_PUNCTUATION + 1;
    public static final byte OPEN_PUNCTUATION = OTHER_PUNCTUATION + 1;
    public static final byte SYMBOL = OPEN_PUNCTUATION + 1;
    public static final byte CURRENCY_SYMBOL = SYMBOL + 1;
    public static final byte MODIFIER_SYMBOL = CURRENCY_SYMBOL + 1;
    public static final byte MATH_SYMBOL = MODIFIER_SYMBOL + 1;
    public static final byte OTHER_SYMBOL = MATH_SYMBOL + 1;
    public static final byte SEPARATOR = OTHER_SYMBOL + 1;
    public static final byte LINE_SEPARATOR = SEPARATOR + 1;
    public static final byte PARAGRAPH_SEPARATOR = LINE_SEPARATOR + 1;
    public static final byte SPACE_SEPARATOR = PARAGRAPH_SEPARATOR + 1;

    // --------------------------------------------------
    // Binary Property Values
    // --------------------------------------------------
    public static final byte TRUE = SPACE_SEPARATOR + 1;
    public static final byte FALSE = TRUE + 1;

    // ==================================================================================
    // PROPERTY NAME MAPPINGS
    // ==================================================================================
    public static final Map<String, Byte> PROPERTY_NAMES;

    static {
        Map<String, Byte> m = new HashMap<>();
        // Binary Properties (ECMA-262)
        add(m, ALPHABETIC, "Alphabetic", "Alpha");
        add(m, ASCII, "ASCII");
        add(m, CASE_IGNORABLE, "Case_Ignorable", "CI");
        add(m, GENERAL_CATEGORY, "General_Category", "gc");
        add(m, SCRIPT, "Script", "sc");
        add(m, SCRIPT_EXTENSIONS, "Script_Extensions", "scx");
        add(m, ASCII_HEX_DIGIT, "ASCII_Hex_Digit", "AHex");
        add(m, HEX_DIGIT, "Hex_Digit", "Hex");
        add(m, ID_CONTINUE, "ID_Continue", "IDC");
        add(m, ID_START, "ID_Start", "IDS");
        add(m, LOWERCASE, "Lowercase", "Lower");
        add(m, UPPERCASE, "Uppercase", "Upper");
        add(m, WHITE_SPACE, "White_Space", "space");
        add(m, ANY, "Any");
        add(m, ASSIGNED, "Assigned");
        add(m, XID_START, "XID_Start", "XIDS");
        add(m, XID_CONTINUE, "XID_Continue", "XIDC");
        add(m, CASED, "Cased");
        // High-Impact Basic Properties
        add(m, IDEOGRAPHIC, "Ideographic", "Ideo");
        add(m, MATH, "Math");
        add(m, DASH, "Dash");
        add(m, NONCHARACTER_CODE_POINT, "Noncharacter_Code_Point", "NChar");
        add(m, BIDI_MIRRORED, "Bidi_Mirrored", "Bidi_M");
        add(m, PATTERN_WHITE_SPACE, "Pattern_White_Space", "Pat_WS");
        // Text Processing Properties
        add(m, GRAPHEME_BASE, "Grapheme_Base", "Gr_Base");
        add(m, GRAPHEME_EXTEND, "Grapheme_Extend", "Gr_Ext");
        add(m, EXTENDER, "Extender", "Ext");
        add(m, PATTERN_SYNTAX, "Pattern_Syntax", "Pat_Syn");
        add(m, JOIN_CONTROL, "Join_Control", "Join_C");
        // Case Change Properties
        add(m, CHANGES_WHEN_LOWERCASED, "Changes_When_Lowercased", "CWL");
        add(m, CHANGES_WHEN_UPPERCASED, "Changes_When_Uppercased", "CWU");
        add(m, CHANGES_WHEN_TITLECASED, "Changes_When_Titlecased", "CWT");
        add(m, CHANGES_WHEN_CASEFOLDED, "Changes_When_Casefolded", "CWCF");
        add(m, CHANGES_WHEN_CASEMAPPED, "Changes_When_Casemapped", "CWCM");
        add(m, CHANGES_WHEN_NFKC_CASEFOLDED, "Changes_When_NFKC_Casefolded", "CWKCF");
        // Additional ICU4J-based properties
        add(m, DEFAULT_IGNORABLE_CODE_POINT, "Default_Ignorable_Code_Point", "DI");
        add(m, EMOJI, "Emoji");
        add(m, EMOJI_PRESENTATION, "Emoji_Presentation", "EPres");
        add(m, EMOJI_MODIFIER, "Emoji_Modifier", "EMod");
        add(m, EMOJI_MODIFIER_BASE, "Emoji_Modifier_Base", "EBase");
        add(m, EMOJI_COMPONENT, "Emoji_Component", "EComp");
        add(m, EXTENDED_PICTOGRAPHIC, "Extended_Pictographic", "ExtPict");
        add(m, REGIONAL_INDICATOR, "Regional_Indicator", "RI");
        add(m, QUOTATION_MARK, "Quotation_Mark", "QMark");
        add(m, SENTENCE_TERMINAL, "Sentence_Terminal", "STerm");
        add(m, TERMINAL_PUNCTUATION, "Terminal_Punctuation", "Term");
        add(m, VARIATION_SELECTOR, "Variation_Selector", "VS");
        add(m, RADICAL, "Radical");
        add(m, UNIFIED_IDEOGRAPH, "Unified_Ideograph", "UIdeo");
        add(m, DEPRECATED, "Deprecated", "Dep");
        add(m, SOFT_DOTTED, "Soft_Dotted", "SD");
        add(m, LOGICAL_ORDER_EXCEPTION, "Logical_Order_Exception", "LOE");
        add(m, DIACRITIC, "Diacritic", "Dia");
        add(m, IDS_BINARY_OPERATOR, "IDS_Binary_Operator", "IDSB");
        add(m, IDS_TRINARY_OPERATOR, "IDS_Trinary_Operator", "IDST");
        PROPERTY_NAMES = Map.copyOf(m);

        // Property Values (General Category names and aliases)
        Map<String, Byte> v = new HashMap<>();
        add(v, OTHER, "Other", "C");
        add(v, CONTROL, "Control", "Cc", "cntrl");
        add(v, FORMAT, "Format", "Cf");
        add(v, UNASSIGNED, "Unassigned", "Cn");
        add(v, PRIVATE_USE, "Private_Use", "Co");
        add(v, SURROGATE, "Surrogate", "Cs");
        add(v, LETTER, "Letter", "L");
        add(v, LOWERCASE_LETTER, "Lowercase_Letter", "Ll");
        add(v, MODIFIER_LETTER, "Modifier_Letter", "Lm");
        add(v, OTHER_LETTER, "Other_Letter", "Lo");
        add(v, TITLECASE_LETTER, "Titlecase_Letter", "Lt");
        add(v, UPPERCASE_LETTER, "Uppercase_Letter", "Lu");
        add(v, MARK, "Mark", "M", "Combining_Mark");
        add(v, SPACING_MARK, "Spacing_Mark", "Mc");
        add(v, ENCLOSING_MARK, "Enclosing_Mark", "Me");
        add(v, NONSPACING_MARK, "Nonspacing_Mark", "Mn");
        add(v, NUMBER, "Number", "N", "digit");
        add(v, DECIMAL_NUMBER, "Decimal_Number", "Nd");
        add(v, LETTER_NUMBER, "Letter_Number", "Nl");
        add(v, OTHER_NUMBER, "Other_Number", "No");
        add(v, PUNCTUATION, "Punctuation", "P", "punct");
        add(v, CONNECTOR_PUNCTUATION, "Connector_Punctuation", "Pc");
        add(v, DASH_PUNCTUATION, "Dash_Punctuation", "Pd");
        add(v, CLOSE_PUNCTUATION, "Close_Punctuation", "Pe");
        add(v, FINAL_PUNCTUATION, "Final_Punctuation", "Pf");
        add(v, INITIAL_PUNCTUATION, "Initial_Punctuation", "Pi");
        add(v, OTHER_PUNCTUATION, "Other_Punctuation", "Po");
        add(v, OPEN_PUNCTUATION, "Open_Punctuation", "Ps");
        add(v, SYMBOL, "Symbol", "S");
        add(v, CURRENCY_SYMBOL, "Currency_Symbol", "Sc");
        add(v, MODIFIER_SYMBOL, "Modifier_Symbol", "Sk");
        add(v, MATH_SYMBOL, "Math_Symbol", "Sm");
        add(v, OTHER_SYMBOL, "Other_Symbol", "So");
        add(v, SEPARATOR, "Separator", "Z");
        add(v, LINE_SEPARATOR, "Line_Separator", "Zl");
        add(v, PARAGRAPH_SEPARATOR, "Paragraph_Separator", "Zp");
        add(v, SPACE_SEPARATOR, "Space_Separator", "Zs");
        PROPERTY_VALUES = Map.copyOf(v);
    }

    /** Property Value Map for General Category (canonical names and aliases). */
    public static final Map<String, Byte> PROPERTY_VALUES;

    // Normalized (case-insensitive) lookup maps - created via static initialization
    private static final Map<String, Byte> NORMALIZED_PROPERTY_NAMES;
    private static final Map<String, Byte> NORMALIZED_PROPERTY_VALUES;

    static {
        // Build case-insensitive lookup maps by normalizing all keys
        Map<String, Byte> normPropNames = new HashMap<>();
        for (Map.Entry<String, Byte> entry : PROPERTY_NAMES.entrySet()) {
            normPropNames.put(normalizePropertyName(entry.getKey()), entry.getValue());
        }
        NORMALIZED_PROPERTY_NAMES = Map.copyOf(normPropNames);

        Map<String, Byte> normPropValues = new HashMap<>();
        for (Map.Entry<String, Byte> entry : PROPERTY_VALUES.entrySet()) {
            normPropValues.put(normalizePropertyName(entry.getKey()), entry.getValue());
        }
        NORMALIZED_PROPERTY_VALUES = Map.copyOf(normPropValues);
    }

    /**
     * Normalizes a Unicode property name for case-insensitive comparison per UAX#44.
     *
     * @param name The property name or value to normalize
     * @return Normalized name (lowercase, no underscores/hyphens/spaces)
     */
    private static String normalizePropertyName(String name) {
        return name.replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    // ==================================================================================
    // PUBLIC API METHODS
    // ==================================================================================

    /**
     * Looks up a property name and optionally a value and returns an encoded int. For binary
     * properties, combines the property name with TRUE. For General_Category, combines
     * General_Category with the specified value.
     *
     * @param propertyOrValue Property name or property name=value pair
     * @return Encoded int combining property name and value
     */
    @SuppressWarnings("EnumOrdinal") // We don't persist the ordinals; hence this is safe.
    public static int lookup(String propertyOrValue) {
        if (propertyOrValue == null || propertyOrValue.isEmpty()) {
            return -1;
        }

        Matcher m = PROPERTY_PATTERN.matcher(propertyOrValue);
        if (!m.matches() || m.group("propName") == null) {
            return -1;
        }

        if (m.group("propValue") == null) {
            // It's a single property name (binary property)
            String property = m.group("propName");

            // Normalize property name for case-insensitive lookup
            Byte propByte = NORMALIZED_PROPERTY_NAMES.get(normalizePropertyName(property));

            if (propByte == null) {
                // Check if it's a general category value without the gc= prefix
                Byte valueByte = NORMALIZED_PROPERTY_VALUES.get(normalizePropertyName(property));
                if (valueByte != null) {
                    // It's a GC value, encode it with GC property
                    return encodeProperty(GENERAL_CATEGORY, valueByte);
                }
                return -1;
            }

            if (requiresValue(propByte)) {
                return -1;
            }

            // It's a binary property, encode with TRUE
            return encodeProperty(propByte, TRUE);
        } else {
            // It's a property=value format
            String property = m.group("propName");
            String value = m.group("propValue");

            // Normalize both property name and value for case-insensitive lookup
            Byte propByte = NORMALIZED_PROPERTY_NAMES.get(normalizePropertyName(property));
            if (propByte == null) {
                return -1;
            }

            switch (propByte) {
                case GENERAL_CATEGORY:
                    Byte valueByte = NORMALIZED_PROPERTY_VALUES.get(normalizePropertyName(value));
                    if (valueByte == null) {
                        return -1;
                    }
                    return encodeProperty(GENERAL_CATEGORY, valueByte);
                case SCRIPT:
                    try {
                        return encodeProperty(
                                SCRIPT, (byte) Character.UnicodeScript.forName(value).ordinal());
                    } catch (IllegalArgumentException e) {
                        return -1;
                    }
                case SCRIPT_EXTENSIONS:
                    try {
                        // Use ICU4JAdapter to get script code by name
                        int scriptCode = ICU4JAdapter.getScriptCodeFromName(value);
                        if (scriptCode == -1) {
                            return -1;
                        }
                        return encodeProperty(SCRIPT_EXTENSIONS, (byte) scriptCode);
                    } catch (IllegalArgumentException e) {
                        return -1;
                    }
                default:
                    // Binary properties don't have values
                    return -1;
            }
        }
    }

    // ==================================================================================
    // INTERNAL HELPER METHODS
    // ==================================================================================

    /**
     * Encodes a property name and value into a single int. The property name is in the high 16
     * bits, the value in the low 16 bits.
     *
     * @param property Property name constant
     * @param value Property value constant
     * @return Encoded int
     */
    private static int encodeProperty(byte property, byte value) {
        return ((property & 0xFF) << 8) | (value & 0xFF);
    }

    /**
     * Checks if a property requires a value to be specified. Properties like General_Category,
     * Script, and Script_Extensions cannot be used without a value (e.g., "\p{Script}" is invalid,
     * must be "\p{Script=Latin}").
     *
     * @param property Property byte constant
     * @return true if the property requires a value
     */
    private static boolean requiresValue(byte property) {
        return property == GENERAL_CATEGORY || property == SCRIPT || property == SCRIPT_EXTENSIONS;
    }

    /** Cached array of all Unicode script values for fast lookup. */
    private static final Character.UnicodeScript[] UnicodeScriptValues =
            Character.UnicodeScript.values();

    /**
     * Tests if a code point has a specific Unicode property.
     *
     * @param property Encoded property (from lookup method)
     * @param codePoint Character code point to test
     * @return true if the code point has the property
     */
    public static boolean hasProperty(int property, int codePoint) {
        byte propByte = (byte) ((property >> 8) & 0xFF);
        int valueByte = (property & 0xFF);

        switch (propByte) {
            case ALPHABETIC:
                return Character.isAlphabetic(codePoint) == (valueByte == TRUE);

            case ASCII:
                return (codePoint <= 0x7F) == (valueByte == TRUE);

            case CASE_IGNORABLE:
                // Java doesn't have a direct method for this
                // This is an approximation
                return (Character.getType(codePoint) == Character.MODIFIER_SYMBOL
                                || Character.getType(codePoint) == Character.MODIFIER_LETTER
                                || Character.getType(codePoint) == Character.NON_SPACING_MARK)
                        == (valueByte == TRUE);

            case GENERAL_CATEGORY:
                int javaCategory = Character.getType(codePoint);
                return checkGeneralCategory(valueByte, javaCategory);
            case ASCII_HEX_DIGIT:
                return isHexDigit(codePoint) == (valueByte == TRUE);
            case HEX_DIGIT:
                return (Character.digit(codePoint, 16) != -1) == (valueByte == TRUE);
            case ID_CONTINUE:
                return Character.isUnicodeIdentifierPart(codePoint) == (valueByte == TRUE);

            case ID_START:
                return Character.isUnicodeIdentifierStart(codePoint) == (valueByte == TRUE);

            case LOWERCASE:
                return Character.isLowerCase(codePoint) == (valueByte == TRUE);

            case UPPERCASE:
                return Character.isUpperCase(codePoint) == (valueByte == TRUE);

            case WHITE_SPACE:
                {
                    // Note: This only a good approximation of the Unicode white space property
                    return (valueByte == TRUE)
                            == (Character.isSpaceChar(codePoint)
                                    || Character.isWhitespace(codePoint));
                }

            // Quick Wins properties
            case ANY:
                // Any matches all code points
                return valueByte == TRUE;

            case ASSIGNED:
                // Assigned matches all code points except unassigned ones
                return (Character.getType(codePoint) != Character.UNASSIGNED)
                        == (valueByte == TRUE);

            case XID_START:
                // XID_Start is like ID_Start but with some modifications for identifiers
                return Character.isUnicodeIdentifierStart(codePoint) == (valueByte == TRUE);

            case XID_CONTINUE:
                // XID_Continue is like ID_Continue but with some modifications for identifiers
                return Character.isUnicodeIdentifierPart(codePoint) == (valueByte == TRUE);

            case CASED:
                // Cased characters have uppercase, lowercase, or titlecase variants
                return (Character.isLowerCase(codePoint)
                                || Character.isUpperCase(codePoint)
                                || Character.isTitleCase(codePoint))
                        == (valueByte == TRUE);

            // High-Impact Basic Properties
            case IDEOGRAPHIC:
                // Ideographic characters (CJK ideographs, etc.)
                return Character.isIdeographic(codePoint) == (valueByte == TRUE);

            case MATH:
                // Math symbols - characters with Math property
                // Java doesn't have direct Math property, approximate with Math_Symbol category
                return (Character.getType(codePoint) == Character.MATH_SYMBOL)
                        == (valueByte == TRUE);

            case DASH:
                // Dash punctuation and hyphen-like characters
                return (Character.getType(codePoint) == Character.DASH_PUNCTUATION)
                        == (valueByte == TRUE);

            case NONCHARACTER_CODE_POINT:
                // Noncharacter code points (U+FDD0..U+FDEF and U+xxFFFE, U+xxFFFF)
                return isNoncharacterCodePoint(codePoint) == (valueByte == TRUE);

            case BIDI_MIRRORED:
                // Characters with Bidi_Mirrored property
                return Character.isMirrored(codePoint) == (valueByte == TRUE);

            case PATTERN_WHITE_SPACE:
                // Pattern_White_Space: whitespace characters in patterns
                // Pattern_White_Space includes: U+0009..U+000D, U+0020, U+0085, U+200E, U+200F,
                // U+2028, U+2029
                return isPatternWhiteSpace(codePoint) == (valueByte == TRUE);

            // Text Processing Properties
            case GRAPHEME_BASE:
                {
                    // Grapheme_Base: characters that can serve as a base for grapheme clusters
                    // Approximation: assigned characters that are not marks or control characters
                    int type = Character.getType(codePoint);
                    return (type != Character.UNASSIGNED
                                    && type != Character.NON_SPACING_MARK
                                    && type != Character.ENCLOSING_MARK
                                    && type != Character.COMBINING_SPACING_MARK
                                    && type != Character.CONTROL
                                    && type != Character.FORMAT
                                    && type != Character.SURROGATE
                                    && type != Character.PRIVATE_USE)
                            == (valueByte == TRUE);
                }

            case GRAPHEME_EXTEND:
                // Grapheme_Extend: combining marks and other extending characters
                return (Character.getType(codePoint) == Character.NON_SPACING_MARK
                                || Character.getType(codePoint) == Character.ENCLOSING_MARK
                                || Character.getType(codePoint) == Character.COMBINING_SPACING_MARK)
                        == (valueByte == TRUE);

            case EXTENDER:
                // Extender: characters that extend the value or shape of a preceding character
                return isExtender(codePoint) == (valueByte == TRUE);

            case PATTERN_SYNTAX:
                // Pattern_Syntax: characters used for syntax in patterns
                return isPatternSyntax(codePoint) == (valueByte == TRUE);

            case JOIN_CONTROL:
                // Join_Control: U+200C ZERO WIDTH NON-JOINER, U+200D ZERO WIDTH JOINER
                return (codePoint == 0x200C || codePoint == 0x200D) == (valueByte == TRUE);

            // Case Change Properties
            case CHANGES_WHEN_LOWERCASED:
                // Characters that change when lowercased
                return (Character.toLowerCase(codePoint) != codePoint) == (valueByte == TRUE);

            case CHANGES_WHEN_UPPERCASED:
                // Characters that change when uppercased
                return (Character.toUpperCase(codePoint) != codePoint) == (valueByte == TRUE);

            case CHANGES_WHEN_TITLECASED:
                // Characters that change when titlecased
                return (Character.toTitleCase(codePoint) != codePoint) == (valueByte == TRUE);

            case CHANGES_WHEN_CASEFOLDED:
                // Characters that change when casefolded
                // Case folding is similar to lowercasing but more aggressive
                return (Character.toLowerCase(codePoint) != codePoint) == (valueByte == TRUE);

            case CHANGES_WHEN_CASEMAPPED:
                // Characters that change under any case mapping
                return (Character.toLowerCase(codePoint) != codePoint
                                || Character.toUpperCase(codePoint) != codePoint
                                || Character.toTitleCase(codePoint) != codePoint)
                        == (valueByte == TRUE);

            case CHANGES_WHEN_NFKC_CASEFOLDED:
                // Characters that change when NFKC casefolded
                // This is an approximation - full NFKC requires normalization
                return (Character.toLowerCase(codePoint) != codePoint) == (valueByte == TRUE);

            // ICU4J-based properties (handled via helper method)
            case DEFAULT_IGNORABLE_CODE_POINT:
            case EMOJI:
            case EMOJI_PRESENTATION:
            case EMOJI_MODIFIER:
            case EMOJI_MODIFIER_BASE:
            case EMOJI_COMPONENT:
            case EXTENDED_PICTOGRAPHIC:
            case REGIONAL_INDICATOR:
            case QUOTATION_MARK:
            case SENTENCE_TERMINAL:
            case TERMINAL_PUNCTUATION:
            case VARIATION_SELECTOR:
            case RADICAL:
            case UNIFIED_IDEOGRAPH:
            case DEPRECATED:
            case SOFT_DOTTED:
            case LOGICAL_ORDER_EXCEPTION:
            case DIACRITIC:
            case IDS_BINARY_OPERATOR:
            case IDS_TRINARY_OPERATOR:
                return checkICU4JProperty(propByte, codePoint, (byte) valueByte);

            case SCRIPT:
                return Character.UnicodeScript.of(codePoint) == UnicodeScriptValues[valueByte];
            case SCRIPT_EXTENSIONS:
                // Script_Extensions uses ICU4J to check if codepoint belongs to the script
                // Create new BitSet for each call to prevent memory leak
                BitSet scriptExtensions = new BitSet();
                ICU4JAdapter.getScriptExtensions(codePoint, scriptExtensions);
                // Check if the desired script is in the extensions
                return scriptExtensions.get(valueByte);
            default:
                return false;
        }
    }

    // --------------------------------------------------
    // ICU4J Property Mapping
    // --------------------------------------------------

    /**
     * Maps our property byte constants to ICU4J UProperty constant names.
     *
     * <p>Uses String names instead of direct UProperty references to avoid compile-time dependency
     * on ICU4J. ICU4JAdapter uses reflection to look up these constants at runtime.
     */
    private static final Map<Byte, String> ICU4J_PROPERTY_NAMES =
            Map.<Byte, String>ofEntries(
                    Map.entry(DEFAULT_IGNORABLE_CODE_POINT, "DEFAULT_IGNORABLE_CODE_POINT"),
                    Map.entry(EMOJI, "EMOJI"),
                    Map.entry(EMOJI_PRESENTATION, "EMOJI_PRESENTATION"),
                    Map.entry(EMOJI_MODIFIER, "EMOJI_MODIFIER"),
                    Map.entry(EMOJI_MODIFIER_BASE, "EMOJI_MODIFIER_BASE"),
                    Map.entry(EMOJI_COMPONENT, "EMOJI_COMPONENT"),
                    Map.entry(EXTENDED_PICTOGRAPHIC, "EXTENDED_PICTOGRAPHIC"),
                    Map.entry(REGIONAL_INDICATOR, "REGIONAL_INDICATOR"),
                    Map.entry(QUOTATION_MARK, "QUOTATION_MARK"),
                    Map.entry(SENTENCE_TERMINAL, "S_TERM"),
                    Map.entry(TERMINAL_PUNCTUATION, "TERMINAL_PUNCTUATION"),
                    Map.entry(VARIATION_SELECTOR, "VARIATION_SELECTOR"),
                    Map.entry(RADICAL, "RADICAL"),
                    Map.entry(UNIFIED_IDEOGRAPH, "UNIFIED_IDEOGRAPH"),
                    Map.entry(DEPRECATED, "DEPRECATED"),
                    Map.entry(SOFT_DOTTED, "SOFT_DOTTED"),
                    Map.entry(LOGICAL_ORDER_EXCEPTION, "LOGICAL_ORDER_EXCEPTION"),
                    Map.entry(DIACRITIC, "DIACRITIC"),
                    Map.entry(IDS_BINARY_OPERATOR, "IDS_BINARY_OPERATOR"),
                    Map.entry(IDS_TRINARY_OPERATOR, "IDS_TRINARY_OPERATOR"));

    /**
     * Helper to check ICU4J binary properties via reflection.
     *
     * <p>Uses ICU4JAdapter to avoid direct compile-time dependency on ICU4J classes. This allows
     * Rhino to compile and run without ICU4J on the classpath (with reduced Unicode property
     * support).
     */
    private static boolean checkICU4JProperty(byte property, int codePoint, byte valueByte) {
        String propertyName = ICU4J_PROPERTY_NAMES.get(property);
        if (propertyName != null) {
            return ICU4JAdapter.hasBinaryProperty(codePoint, propertyName) == (valueByte == TRUE);
        }
        return false;
    }

    // --------------------------------------------------
    // Property Checking Helpers
    // --------------------------------------------------

    /** Maps our property value bytes to Java's Character.getType() values. */
    private static boolean checkGeneralCategory(int propertyValueByte, int javaCategory) {
        switch (propertyValueByte) {
            case LETTER:
                return javaCategory == Character.UPPERCASE_LETTER
                        || javaCategory == Character.LOWERCASE_LETTER
                        || javaCategory == Character.TITLECASE_LETTER
                        || javaCategory == Character.MODIFIER_LETTER
                        || javaCategory == Character.OTHER_LETTER;
            case UPPERCASE_LETTER:
                return javaCategory == Character.UPPERCASE_LETTER;
            case LOWERCASE_LETTER:
                return javaCategory == Character.LOWERCASE_LETTER;
            case TITLECASE_LETTER:
                return javaCategory == Character.TITLECASE_LETTER;
            case MODIFIER_LETTER:
                return javaCategory == Character.MODIFIER_LETTER;
            case OTHER_LETTER:
                return javaCategory == Character.OTHER_LETTER;
            case MARK:
                return javaCategory == Character.NON_SPACING_MARK
                        || javaCategory == Character.ENCLOSING_MARK
                        || javaCategory == Character.COMBINING_SPACING_MARK;
            case NONSPACING_MARK:
                return javaCategory == Character.NON_SPACING_MARK;
            case ENCLOSING_MARK:
                return javaCategory == Character.ENCLOSING_MARK;
            case SPACING_MARK:
                return javaCategory == Character.COMBINING_SPACING_MARK;
            case NUMBER:
                return javaCategory == Character.DECIMAL_DIGIT_NUMBER
                        || javaCategory == Character.LETTER_NUMBER
                        || javaCategory == Character.OTHER_NUMBER;
            case DECIMAL_NUMBER:
                return javaCategory == Character.DECIMAL_DIGIT_NUMBER;
            case LETTER_NUMBER:
                return javaCategory == Character.LETTER_NUMBER;
            case OTHER_NUMBER:
                return javaCategory == Character.OTHER_NUMBER;

            case SEPARATOR:
                return javaCategory == Character.SPACE_SEPARATOR
                        || javaCategory == Character.LINE_SEPARATOR
                        || javaCategory == Character.PARAGRAPH_SEPARATOR;
            case SPACE_SEPARATOR:
                return javaCategory == Character.SPACE_SEPARATOR;
            case LINE_SEPARATOR:
                return javaCategory == Character.LINE_SEPARATOR;
            case PARAGRAPH_SEPARATOR:
                return javaCategory == Character.PARAGRAPH_SEPARATOR;

            case OTHER:
                return javaCategory == Character.OTHER_LETTER
                        || javaCategory == Character.OTHER_NUMBER
                        || javaCategory == Character.OTHER_PUNCTUATION
                        || javaCategory == Character.OTHER_SYMBOL;
            case CONTROL:
                return javaCategory == Character.CONTROL;
            case FORMAT:
                return javaCategory == Character.FORMAT;
            case SURROGATE:
                return javaCategory == Character.SURROGATE;
            case PRIVATE_USE:
                return javaCategory == Character.PRIVATE_USE;

            case PUNCTUATION:
                return javaCategory == Character.CONNECTOR_PUNCTUATION
                        || javaCategory == Character.DASH_PUNCTUATION
                        || javaCategory == Character.START_PUNCTUATION
                        || javaCategory == Character.END_PUNCTUATION
                        || javaCategory == Character.OTHER_PUNCTUATION
                        || javaCategory == Character.INITIAL_QUOTE_PUNCTUATION
                        || javaCategory == Character.FINAL_QUOTE_PUNCTUATION;
            case DASH_PUNCTUATION:
                return javaCategory == Character.DASH_PUNCTUATION;
            case OPEN_PUNCTUATION:
                return javaCategory == Character.START_PUNCTUATION;
            case CLOSE_PUNCTUATION:
                return javaCategory == Character.END_PUNCTUATION;
            case CONNECTOR_PUNCTUATION:
                return javaCategory == Character.CONNECTOR_PUNCTUATION;
            case OTHER_PUNCTUATION:
                return javaCategory == Character.OTHER_PUNCTUATION;
            case INITIAL_PUNCTUATION:
                return javaCategory == Character.INITIAL_QUOTE_PUNCTUATION;
            case FINAL_PUNCTUATION:
                return javaCategory == Character.FINAL_QUOTE_PUNCTUATION;

            case SYMBOL:
                return javaCategory == Character.MATH_SYMBOL
                        || javaCategory == Character.CURRENCY_SYMBOL
                        || javaCategory == Character.MODIFIER_SYMBOL
                        || javaCategory == Character.OTHER_SYMBOL;
            case MATH_SYMBOL:
                return javaCategory == Character.MATH_SYMBOL;
            case CURRENCY_SYMBOL:
                return javaCategory == Character.CURRENCY_SYMBOL;
            case MODIFIER_SYMBOL:
                return javaCategory == Character.MODIFIER_SYMBOL;
            case OTHER_SYMBOL:
                return javaCategory == Character.OTHER_SYMBOL;
            case UNASSIGNED:
                return javaCategory == Character.UNASSIGNED;

            default:
                return false;
        }
    }

    /** Checks if a code point is a hex digit. */
    private static boolean isHexDigit(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'a' && codePoint <= 'f')
                || (codePoint >= 'A' && codePoint <= 'F');
    }

    /**
     * Checks if a code point is a noncharacter. Noncharacters: U+FDD0..U+FDEF and U+xxFFFE,
     * U+xxFFFF for all planes
     */
    private static boolean isNoncharacterCodePoint(int codePoint) {
        // U+FDD0..U+FDEF
        if (codePoint >= 0xFDD0 && codePoint <= 0xFDEF) {
            return true;
        }
        // Last two code points of any plane (U+xxFFFE, U+xxFFFF)
        return (codePoint & 0xFFFE) == 0xFFFE;
    }

    /**
     * Checks if a code point is Pattern_White_Space. Pattern_White_Space: U+0009..U+000D, U+0020,
     * U+0085, U+200E, U+200F, U+2028, U+2029
     */
    private static boolean isPatternWhiteSpace(int codePoint) {
        return (codePoint >= 0x0009 && codePoint <= 0x000D) // TAB, LF, VT, FF, CR
                || codePoint == 0x0020 // SPACE
                || codePoint == 0x0085 // NEL (Next Line)
                || codePoint == 0x200E // LEFT-TO-RIGHT MARK
                || codePoint == 0x200F // RIGHT-TO-LEFT MARK
                || codePoint == 0x2028 // LINE SEPARATOR
                || codePoint == 0x2029; // PARAGRAPH SEPARATOR
    }

    /** Checks if a code point is an Extender. Common extenders from Unicode data. */
    private static boolean isExtender(int codePoint) {
        // Most common extenders - subset from UCD
        return codePoint == 0x00B7 // MIDDLE DOT
                || codePoint == 0x02D0 // MODIFIER LETTER TRIANGULAR COLON
                || codePoint == 0x02D1 // MODIFIER LETTER HALF TRIANGULAR COLON
                || codePoint == 0x0640 // ARABIC TATWEEL
                || codePoint == 0x07FA // NKO LAJANYALAN
                || codePoint == 0x0E46 // THAI CHARACTER MAIYAMOK
                || codePoint == 0x0EC6 // LAO KO LA
                || codePoint == 0x3005 // IDEOGRAPHIC ITERATION MARK
                || codePoint == 0x3031 // VERTICAL KANA REPEAT MARK
                || codePoint == 0x3032 // VERTICAL KANA REPEAT WITH VOICED SOUND MARK
                || codePoint == 0x3033 // VERTICAL KANA REPEAT MARK UPPER HALF
                || codePoint == 0x3034 // VERTICAL KANA REPEAT WITH VOICED SOUND MARK UPPER HALF
                || codePoint == 0x3035 // VERTICAL KANA REPEAT MARK LOWER HALF
                || codePoint == 0x309D // HIRAGANA ITERATION MARK
                || codePoint == 0x309E // HIRAGANA VOICED ITERATION MARK
                || codePoint == 0x30FC // KATAKANA-HIRAGANA PROLONGED SOUND MARK
                || codePoint == 0x30FD // KATAKANA ITERATION MARK
                || codePoint == 0x30FE // KATAKANA VOICED ITERATION MARK
                || codePoint == 0xFF70; // HALFWIDTH KATAKANA-HIRAGANA PROLONGED SOUND MARK
    }

    /**
     * Checks if a code point is Pattern_Syntax. Pattern_Syntax characters are used for syntax in
     * patterns.
     */
    private static boolean isPatternSyntax(int codePoint) {
        // Pattern_Syntax includes various punctuation and symbol ranges
        return (codePoint >= 0x0021 && codePoint <= 0x002F) // ! to /
                || (codePoint >= 0x003A && codePoint <= 0x0040) // : to @
                || (codePoint >= 0x005B && codePoint <= 0x005E) // [ to ^
                || codePoint == 0x0060 // `
                || (codePoint >= 0x007B && codePoint <= 0x007E) // { to ~
                || (codePoint >= 0x00A1 && codePoint <= 0x00A7)
                || codePoint == 0x00A9
                || (codePoint >= 0x00AB && codePoint <= 0x00AC)
                || codePoint == 0x00AE
                || codePoint == 0x00B0
                || codePoint == 0x00B1
                || codePoint == 0x00B6
                || codePoint == 0x00BB
                || codePoint == 0x00BF
                || codePoint == 0x00D7
                || codePoint == 0x00F7
                || (codePoint >= 0x2010 && codePoint <= 0x2027)
                || (codePoint >= 0x2030 && codePoint <= 0x203E)
                || (codePoint >= 0x2041 && codePoint <= 0x2053)
                || (codePoint >= 0x2055 && codePoint <= 0x205E)
                || (codePoint >= 0x2190 && codePoint <= 0x245F)
                || (codePoint >= 0x2500 && codePoint <= 0x2775)
                || (codePoint >= 0x2794 && codePoint <= 0x2BFF)
                || (codePoint >= 0x2E00 && codePoint <= 0x2E7F)
                || (codePoint >= 0x3001 && codePoint <= 0x3003)
                || (codePoint >= 0x3008 && codePoint <= 0x3020)
                || codePoint == 0x3030
                || (codePoint >= 0xFD3E && codePoint <= 0xFD3F)
                || (codePoint >= 0xFE45 && codePoint <= 0xFE46);
    }

    /**
     * Helper to add multiple property name aliases to a map.
     *
     * @param map the map to add entries to
     * @param value the property value
     * @param names one or more property name aliases
     */
    private static void add(Map<String, Byte> map, byte value, String... names) {
        for (String name : names) {
            map.put(name, value);
        }
    }
}
