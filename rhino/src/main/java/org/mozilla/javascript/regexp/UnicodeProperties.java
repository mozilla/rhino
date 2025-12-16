package org.mozilla.javascript.regexp;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Unicode properties handler for Java 11 Character class. Handles binary properties from ECMA-262
 * and general category values.
 *
 * <p>This class provides lookups for Unicode properties used in regular expressions, including
 * binary properties (Alphabetic, Emoji, etc.), general categories (Letter, Number, etc.), and
 * scripts. It uses both Java's built-in Character class and ICU4J for comprehensive Unicode
 * support.
 */
public class UnicodeProperties {

    // ==================================================================================
    // THREAD-LOCAL REUSABLE OBJECTS
    // ==================================================================================

    /**
     * Thread-local BitSet for Script_Extensions lookups. Reusing BitSet objects across calls
     * prevents millions of allocations when running large test suites.
     */
    private static final ThreadLocal<BitSet> SCRIPT_EXTENSIONS_BITSET =
            ThreadLocal.withInitial(BitSet::new);

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
    public static final Map<String, Byte> PROPERTY_NAMES =
            Map.ofEntries(
                    Map.entry("Alphabetic", ALPHABETIC),
                    Map.entry("Alpha", ALPHABETIC),
                    Map.entry("ASCII", ASCII),
                    Map.entry("Case_Ignorable", CASE_IGNORABLE),
                    Map.entry("CI", CASE_IGNORABLE),
                    Map.entry("General_Category", GENERAL_CATEGORY),
                    Map.entry("gc", GENERAL_CATEGORY),
                    Map.entry("Script", SCRIPT),
                    Map.entry("sc", SCRIPT),
                    Map.entry("Script_Extensions", SCRIPT_EXTENSIONS),
                    Map.entry("scx", SCRIPT_EXTENSIONS),
                    Map.entry("ASCII_Hex_Digit", ASCII_HEX_DIGIT),
                    Map.entry("AHex", ASCII_HEX_DIGIT),
                    Map.entry("Hex_Digit", HEX_DIGIT),
                    Map.entry("Hex", HEX_DIGIT),
                    Map.entry("ID_Continue", ID_CONTINUE),
                    Map.entry("IDC", ID_CONTINUE),
                    Map.entry("ID_Start", ID_START),
                    Map.entry("IDS", ID_START),
                    Map.entry("Lowercase", LOWERCASE),
                    Map.entry("Lower", LOWERCASE),
                    Map.entry("Uppercase", UPPERCASE),
                    Map.entry("Upper", UPPERCASE),
                    Map.entry("White_Space", WHITE_SPACE),
                    Map.entry("space", WHITE_SPACE),
                    Map.entry("Any", ANY),
                    Map.entry("Assigned", ASSIGNED),
                    Map.entry("XID_Start", XID_START),
                    Map.entry("XIDS", XID_START),
                    Map.entry("XID_Continue", XID_CONTINUE),
                    Map.entry("XIDC", XID_CONTINUE),
                    Map.entry("Cased", CASED),
                    // High-Impact Basic Properties
                    Map.entry("Ideographic", IDEOGRAPHIC),
                    Map.entry("Ideo", IDEOGRAPHIC),
                    Map.entry("Math", MATH),
                    Map.entry("Dash", DASH),
                    Map.entry("Noncharacter_Code_Point", NONCHARACTER_CODE_POINT),
                    Map.entry("NChar", NONCHARACTER_CODE_POINT),
                    Map.entry("Bidi_Mirrored", BIDI_MIRRORED),
                    Map.entry("Bidi_M", BIDI_MIRRORED),
                    Map.entry("Pattern_White_Space", PATTERN_WHITE_SPACE),
                    Map.entry("Pat_WS", PATTERN_WHITE_SPACE),
                    // Text Processing Properties
                    Map.entry("Grapheme_Base", GRAPHEME_BASE),
                    Map.entry("Gr_Base", GRAPHEME_BASE),
                    Map.entry("Grapheme_Extend", GRAPHEME_EXTEND),
                    Map.entry("Gr_Ext", GRAPHEME_EXTEND),
                    Map.entry("Extender", EXTENDER),
                    Map.entry("Ext", EXTENDER),
                    Map.entry("Pattern_Syntax", PATTERN_SYNTAX),
                    Map.entry("Pat_Syn", PATTERN_SYNTAX),
                    Map.entry("Join_Control", JOIN_CONTROL),
                    Map.entry("Join_C", JOIN_CONTROL),
                    // Case Change Properties
                    Map.entry("Changes_When_Lowercased", CHANGES_WHEN_LOWERCASED),
                    Map.entry("CWL", CHANGES_WHEN_LOWERCASED),
                    Map.entry("Changes_When_Uppercased", CHANGES_WHEN_UPPERCASED),
                    Map.entry("CWU", CHANGES_WHEN_UPPERCASED),
                    Map.entry("Changes_When_Titlecased", CHANGES_WHEN_TITLECASED),
                    Map.entry("CWT", CHANGES_WHEN_TITLECASED),
                    Map.entry("Changes_When_Casefolded", CHANGES_WHEN_CASEFOLDED),
                    Map.entry("CWCF", CHANGES_WHEN_CASEFOLDED),
                    Map.entry("Changes_When_Casemapped", CHANGES_WHEN_CASEMAPPED),
                    Map.entry("CWCM", CHANGES_WHEN_CASEMAPPED),
                    Map.entry("Changes_When_NFKC_Casefolded", CHANGES_WHEN_NFKC_CASEFOLDED),
                    Map.entry("CWKCF", CHANGES_WHEN_NFKC_CASEFOLDED),
                    // Additional ICU4J-based properties
                    Map.entry("Default_Ignorable_Code_Point", DEFAULT_IGNORABLE_CODE_POINT),
                    Map.entry("DI", DEFAULT_IGNORABLE_CODE_POINT),
                    Map.entry("Emoji", EMOJI),
                    Map.entry("Emoji_Presentation", EMOJI_PRESENTATION),
                    Map.entry("EPres", EMOJI_PRESENTATION),
                    Map.entry("Emoji_Modifier", EMOJI_MODIFIER),
                    Map.entry("EMod", EMOJI_MODIFIER),
                    Map.entry("Emoji_Modifier_Base", EMOJI_MODIFIER_BASE),
                    Map.entry("EBase", EMOJI_MODIFIER_BASE),
                    Map.entry("Emoji_Component", EMOJI_COMPONENT),
                    Map.entry("EComp", EMOJI_COMPONENT),
                    Map.entry("Extended_Pictographic", EXTENDED_PICTOGRAPHIC),
                    Map.entry("ExtPict", EXTENDED_PICTOGRAPHIC),
                    Map.entry("Regional_Indicator", REGIONAL_INDICATOR),
                    Map.entry("RI", REGIONAL_INDICATOR),
                    Map.entry("Quotation_Mark", QUOTATION_MARK),
                    Map.entry("QMark", QUOTATION_MARK),
                    Map.entry("Sentence_Terminal", SENTENCE_TERMINAL),
                    Map.entry("STerm", SENTENCE_TERMINAL),
                    Map.entry("Terminal_Punctuation", TERMINAL_PUNCTUATION),
                    Map.entry("Term", TERMINAL_PUNCTUATION),
                    Map.entry("Variation_Selector", VARIATION_SELECTOR),
                    Map.entry("VS", VARIATION_SELECTOR),
                    Map.entry("Radical", RADICAL),
                    Map.entry("Unified_Ideograph", UNIFIED_IDEOGRAPH),
                    Map.entry("UIdeo", UNIFIED_IDEOGRAPH),
                    Map.entry("Deprecated", DEPRECATED),
                    Map.entry("Dep", DEPRECATED),
                    Map.entry("Soft_Dotted", SOFT_DOTTED),
                    Map.entry("SD", SOFT_DOTTED),
                    Map.entry("Logical_Order_Exception", LOGICAL_ORDER_EXCEPTION),
                    Map.entry("LOE", LOGICAL_ORDER_EXCEPTION),
                    Map.entry("Diacritic", DIACRITIC),
                    Map.entry("Dia", DIACRITIC),
                    Map.entry("IDS_Binary_Operator", IDS_BINARY_OPERATOR),
                    Map.entry("IDSB", IDS_BINARY_OPERATOR),
                    Map.entry("IDS_Trinary_Operator", IDS_TRINARY_OPERATOR),
                    Map.entry("IDST", IDS_TRINARY_OPERATOR));

    /** Property Value Map for General Category (canonical names and aliases). */
    public static final Map<String, Byte> PROPERTY_VALUES =
            Map.<String, Byte>ofEntries(
                    Map.entry("Other", OTHER),
                    Map.entry("C", OTHER),
                    Map.entry("Control", CONTROL),
                    Map.entry("Cc", CONTROL),
                    Map.entry("cntrl", CONTROL),
                    Map.entry("Format", FORMAT),
                    Map.entry("Cf", FORMAT),
                    Map.entry("Unassigned", UNASSIGNED),
                    Map.entry("Cn", UNASSIGNED),
                    Map.entry("Private_Use", PRIVATE_USE),
                    Map.entry("Co", PRIVATE_USE),
                    Map.entry("Surrogate", SURROGATE),
                    Map.entry("Cs", SURROGATE),
                    Map.entry("Letter", LETTER),
                    Map.entry("L", LETTER),
                    Map.entry("Lowercase_Letter", LOWERCASE_LETTER),
                    Map.entry("Ll", LOWERCASE_LETTER),
                    Map.entry("Modifier_Letter", MODIFIER_LETTER),
                    Map.entry("Lm", MODIFIER_LETTER),
                    Map.entry("Other_Letter", OTHER_LETTER),
                    Map.entry("Lo", OTHER_LETTER),
                    Map.entry("Titlecase_Letter", TITLECASE_LETTER),
                    Map.entry("Lt", TITLECASE_LETTER),
                    Map.entry("Uppercase_Letter", UPPERCASE_LETTER),
                    Map.entry("Lu", UPPERCASE_LETTER),
                    Map.entry("Mark", MARK),
                    Map.entry("M", MARK),
                    Map.entry("Combining_Mark", MARK),
                    Map.entry("Spacing_Mark", SPACING_MARK),
                    Map.entry("Mc", SPACING_MARK),
                    Map.entry("Enclosing_Mark", ENCLOSING_MARK),
                    Map.entry("Me", ENCLOSING_MARK),
                    Map.entry("Nonspacing_Mark", NONSPACING_MARK),
                    Map.entry("Mn", NONSPACING_MARK),
                    Map.entry("Number", NUMBER),
                    Map.entry("N", NUMBER),
                    Map.entry("Decimal_Number", DECIMAL_NUMBER),
                    Map.entry("Nd", DECIMAL_NUMBER),
                    Map.entry("digit", NUMBER),
                    Map.entry("Letter_Number", LETTER_NUMBER),
                    Map.entry("Nl", LETTER_NUMBER),
                    Map.entry("Other_Number", OTHER_NUMBER),
                    Map.entry("No", OTHER_NUMBER),
                    Map.entry("Punctuation", PUNCTUATION),
                    Map.entry("P", PUNCTUATION),
                    Map.entry("punct", PUNCTUATION),
                    Map.entry("Connector_Punctuation", CONNECTOR_PUNCTUATION),
                    Map.entry("Pc", CONNECTOR_PUNCTUATION),
                    Map.entry("Dash_Punctuation", DASH_PUNCTUATION),
                    Map.entry("Pd", DASH_PUNCTUATION),
                    Map.entry("Close_Punctuation", CLOSE_PUNCTUATION),
                    Map.entry("Pe", CLOSE_PUNCTUATION),
                    Map.entry("Final_Punctuation", FINAL_PUNCTUATION),
                    Map.entry("Pf", FINAL_PUNCTUATION),
                    Map.entry("Initial_Punctuation", INITIAL_PUNCTUATION),
                    Map.entry("Pi", INITIAL_PUNCTUATION),
                    Map.entry("Other_Punctuation", OTHER_PUNCTUATION),
                    Map.entry("Po", OTHER_PUNCTUATION),
                    Map.entry("Open_Punctuation", OPEN_PUNCTUATION),
                    Map.entry("Ps", OPEN_PUNCTUATION),
                    Map.entry("Symbol", SYMBOL),
                    Map.entry("S", SYMBOL),
                    Map.entry("Currency_Symbol", CURRENCY_SYMBOL),
                    Map.entry("Sc", CURRENCY_SYMBOL),
                    Map.entry("Modifier_Symbol", MODIFIER_SYMBOL),
                    Map.entry("Sk", MODIFIER_SYMBOL),
                    Map.entry("Math_Symbol", MATH_SYMBOL),
                    Map.entry("Sm", MATH_SYMBOL),
                    Map.entry("Other_Symbol", OTHER_SYMBOL),
                    Map.entry("So", OTHER_SYMBOL),
                    Map.entry("Separator", SEPARATOR),
                    Map.entry("Z", SEPARATOR),
                    Map.entry("Line_Separator", LINE_SEPARATOR),
                    Map.entry("Zl", LINE_SEPARATOR),
                    Map.entry("Paragraph_Separator", PARAGRAPH_SEPARATOR),
                    Map.entry("Zp", PARAGRAPH_SEPARATOR),
                    Map.entry("Space_Separator", SPACE_SEPARATOR),
                    Map.entry("Zs", SPACE_SEPARATOR));

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
     * Normalizes a Unicode property name or value for case-insensitive, whitespace-insensitive
     * comparison. According to UAX#44, property names and values are matched by removing all
     * underscores, hyphens, and spaces, and comparing case-insensitively.
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
                        // Use ICU4J to get script code by name
                        int scriptCode = UScript.getCodeFromName(value);
                        if (scriptCode == UScript.INVALID_CODE) {
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
                // Reuse thread-local BitSet to avoid millions of allocations
                BitSet scriptExtensions = SCRIPT_EXTENSIONS_BITSET.get();
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

    /** Maps our property byte constants to ICU4J UProperty integer constants. */
    private static final Map<Byte, Integer> ICU4J_PROPERTY_MAP =
            Map.<Byte, Integer>ofEntries(
                    Map.entry(
                            DEFAULT_IGNORABLE_CODE_POINT,
                            com.ibm.icu.lang.UProperty.DEFAULT_IGNORABLE_CODE_POINT),
                    Map.entry(EMOJI, com.ibm.icu.lang.UProperty.EMOJI),
                    Map.entry(EMOJI_PRESENTATION, com.ibm.icu.lang.UProperty.EMOJI_PRESENTATION),
                    Map.entry(EMOJI_MODIFIER, com.ibm.icu.lang.UProperty.EMOJI_MODIFIER),
                    Map.entry(EMOJI_MODIFIER_BASE, com.ibm.icu.lang.UProperty.EMOJI_MODIFIER_BASE),
                    Map.entry(EMOJI_COMPONENT, com.ibm.icu.lang.UProperty.EMOJI_COMPONENT),
                    Map.entry(
                            EXTENDED_PICTOGRAPHIC,
                            com.ibm.icu.lang.UProperty.EXTENDED_PICTOGRAPHIC),
                    Map.entry(REGIONAL_INDICATOR, com.ibm.icu.lang.UProperty.REGIONAL_INDICATOR),
                    Map.entry(QUOTATION_MARK, com.ibm.icu.lang.UProperty.QUOTATION_MARK),
                    Map.entry(SENTENCE_TERMINAL, com.ibm.icu.lang.UProperty.S_TERM),
                    Map.entry(
                            TERMINAL_PUNCTUATION, com.ibm.icu.lang.UProperty.TERMINAL_PUNCTUATION),
                    Map.entry(VARIATION_SELECTOR, com.ibm.icu.lang.UProperty.VARIATION_SELECTOR),
                    Map.entry(RADICAL, com.ibm.icu.lang.UProperty.RADICAL),
                    Map.entry(UNIFIED_IDEOGRAPH, com.ibm.icu.lang.UProperty.UNIFIED_IDEOGRAPH),
                    Map.entry(DEPRECATED, com.ibm.icu.lang.UProperty.DEPRECATED),
                    Map.entry(SOFT_DOTTED, com.ibm.icu.lang.UProperty.SOFT_DOTTED),
                    Map.entry(
                            LOGICAL_ORDER_EXCEPTION,
                            com.ibm.icu.lang.UProperty.LOGICAL_ORDER_EXCEPTION),
                    Map.entry(DIACRITIC, com.ibm.icu.lang.UProperty.DIACRITIC),
                    Map.entry(IDS_BINARY_OPERATOR, com.ibm.icu.lang.UProperty.IDS_BINARY_OPERATOR),
                    Map.entry(
                            IDS_TRINARY_OPERATOR, com.ibm.icu.lang.UProperty.IDS_TRINARY_OPERATOR));

    /** Helper to check ICU4J binary properties */
    private static boolean checkICU4JProperty(byte property, int codePoint, byte valueByte) {
        Integer icu4jProperty = ICU4J_PROPERTY_MAP.get(property);
        if (icu4jProperty != null) {
            return com.ibm.icu.lang.UCharacter.hasBinaryProperty(codePoint, icu4jProperty)
                    == (valueByte == TRUE);
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
}
