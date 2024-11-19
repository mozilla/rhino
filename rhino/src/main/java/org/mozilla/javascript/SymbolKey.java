package org.mozilla.javascript;

import java.io.Serializable;

/**
 * A SymbolKey is one of the implementations of Symbol. It is really there so that we can easily use
 * pre-defined symbols as keys in native code. A SymbolKey has the special property that two
 * NativeSymbol objects with the same key are equal.
 */
public class SymbolKey implements Symbol, Serializable {
    private static final long serialVersionUID = -6019782713330994754L;

    // These are common SymbolKeys that are equivalent to well-known symbols
    // defined in ECMAScript.
    public static final SymbolKey ITERATOR = new SymbolKey("Symbol.iterator");
    public static final SymbolKey TO_STRING_TAG = new SymbolKey("Symbol.toStringTag");
    public static final SymbolKey SPECIES = new SymbolKey("Symbol.species");
    public static final SymbolKey HAS_INSTANCE = new SymbolKey("Symbol.hasInstance");
    public static final SymbolKey IS_CONCAT_SPREADABLE = new SymbolKey("Symbol.isConcatSpreadable");
    public static final SymbolKey IS_REGEXP = new SymbolKey("Symbol.isRegExp");
    public static final SymbolKey TO_PRIMITIVE = new SymbolKey("Symbol.toPrimitive");
    public static final SymbolKey MATCH = new SymbolKey("Symbol.match");
    public static final SymbolKey MATCH_ALL = new SymbolKey("Symbol.matchAll");
    public static final SymbolKey REPLACE = new SymbolKey("Symbol.replace");
    public static final SymbolKey SEARCH = new SymbolKey("Symbol.search");
    public static final SymbolKey SPLIT = new SymbolKey("Symbol.split");
    public static final SymbolKey UNSCOPABLES = new SymbolKey("Symbol.unscopables");

    // If passed a javascript undefined, this will be a (java) null
    private final String name;

    public SymbolKey(String name) {
        this.name = name;
    }

    /**
     * Returns the symbol's name. Returns empty string for anonymous symbol (i.e. something created
     * with <code>Symbol()</code>).
     */
    public String getName() {
        return name != null ? name : "";
    }

    /**
     * Returns the symbol's description - will return {@link Undefined#instance} if we have an
     * anonymous symbol (i.e. something created with <code>Symbol()</code>).
     */
    public Object getDescription() {
        return name != null ? name : Undefined.instance;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SymbolKey) {
            return o == this;
        }
        if (o instanceof NativeSymbol) {
            return ((NativeSymbol) o).getKey() == this;
        }
        return false;
    }

    @Override
    public String toString() {
        if (name == null) {
            return "Symbol()";
        }
        return "Symbol(" + name + ')';
    }
}
