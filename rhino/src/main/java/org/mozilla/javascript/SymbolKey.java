package org.mozilla.javascript;

import static org.mozilla.javascript.Symbol.Kind.BUILT_IN;

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
    public static final SymbolKey ITERATOR = new SymbolKey("Symbol.iterator", BUILT_IN);
    public static final SymbolKey TO_STRING_TAG = new SymbolKey("Symbol.toStringTag", BUILT_IN);
    public static final SymbolKey SPECIES = new SymbolKey("Symbol.species", BUILT_IN);
    public static final SymbolKey HAS_INSTANCE = new SymbolKey("Symbol.hasInstance", BUILT_IN);
    public static final SymbolKey IS_CONCAT_SPREADABLE =
            new SymbolKey("Symbol.isConcatSpreadable", BUILT_IN);
    public static final SymbolKey IS_REGEXP = new SymbolKey("Symbol.isRegExp", BUILT_IN);
    public static final SymbolKey TO_PRIMITIVE = new SymbolKey("Symbol.toPrimitive", BUILT_IN);
    public static final SymbolKey MATCH = new SymbolKey("Symbol.match", BUILT_IN);
    public static final SymbolKey MATCH_ALL = new SymbolKey("Symbol.matchAll", BUILT_IN);
    public static final SymbolKey REPLACE = new SymbolKey("Symbol.replace", BUILT_IN);
    public static final SymbolKey SEARCH = new SymbolKey("Symbol.search", BUILT_IN);
    public static final SymbolKey SPLIT = new SymbolKey("Symbol.split", BUILT_IN);
    public static final SymbolKey UNSCOPABLES = new SymbolKey("Symbol.unscopables", BUILT_IN);

    // If passed a javascript undefined, this will be a (java) null
    private final String name;
    private final Symbol.Kind kind;

    public SymbolKey(String name, Symbol.Kind kind) {
        this.name = name;
        this.kind = kind;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    /** {@inheritDoc} */
    @Override
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
