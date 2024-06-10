/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.mozilla.javascript.debug.DebuggableObject;

/**
 * An object that implements deep equality test of objects, including their reference graph
 * topology, that is in addition to establishing by-value equality of objects, it also establishes
 * that their reachable object graphs have identical shape. It is capable of custom-comparing a wide
 * range of various objects, including various Rhino Scriptables, Java arrays, Java Lists, and to
 * some degree Java Maps and Sets (sorted Maps are okay, as well as Sets with elements that can be
 * sorted using their Comparable implementation, and Maps whose keysets work the same). The
 * requirement for sortable maps and sets is to ensure deterministic order of traversal, which is
 * necessary for establishing structural equality of object graphs.
 *
 * <p>An instance of this object is stateful in that it memoizes pairs of objects that already
 * compared equal, so reusing an instance for repeated equality tests of potentially overlapping
 * object graph is beneficial for performance as long as all equality test invocations returns true.
 * Reuse is not advised after an equality test returned false since there is a heuristic in
 * comparing cyclic data structures that can memoize false equalities if two cyclic data structures
 * end up being unequal.
 */
final class EqualObjectGraphs {
    private static final ThreadLocal<EqualObjectGraphs> instance = new ThreadLocal<>();

    private static final Set<Class<?>> valueClasses =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    Boolean.class,
                                    Byte.class,
                                    Character.class,
                                    Double.class,
                                    Float.class,
                                    Integer.class,
                                    Long.class,
                                    Short.class)));

    // Object pairs already known to be equal. Used to short-circuit repeated traversals of objects
    // reachable through
    // different paths as well as to detect structural inequality.
    private final IdentityHashMap<Object, Object> knownEquals = new IdentityHashMap<>();
    // Currently compared objects; used to avoid infinite recursion over cyclic object graphs.
    private final IdentityHashMap<Object, Object> currentlyCompared = new IdentityHashMap<>();

    static <T> T withThreadLocal(java.util.function.Function<EqualObjectGraphs, T> action) {
        final EqualObjectGraphs currEq = instance.get();
        if (currEq == null) {
            final EqualObjectGraphs eq = new EqualObjectGraphs();
            instance.set(eq);
            try {
                return action.apply(eq);
            } finally {
                instance.set(null);
            }
        }
        return action.apply(currEq);
    }

    boolean equalGraphs(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else if (o1 == null || o2 == null) {
            return false;
            // String (and ConsStrings), Booleans, and Doubles are considered
            // JavaScript primitive values and are thus compared by value and
            // with no regard to their object identity.
        } else if (o1 instanceof String) {
            if (o2 instanceof ConsString) {
                return o1.equals(o2.toString());
            }
            return o1.equals(o2);
        } else if (o1 instanceof ConsString) {
            if (o2 instanceof String || o2 instanceof ConsString) {
                return o1.toString().equals(o2.toString());
            }
            return false;
        } else if (valueClasses.contains(o1.getClass())) {
            return o1.equals(o2);
        }

        final Object curr2 = currentlyCompared.get(o1);
        if (curr2 == o2) {
            // Provisionally afford that if we're already recursively comparing
            // (o1, o2) that they'll be equal. NOTE: this is the heuristic
            // mentioned in the class JavaDoc that can drive memoizing false
            // equalities if cyclic data structures end up being unequal.
            // While it would be possible to fix that with additional code, the
            // usual usage of equality comparisons is short-circuit-on-false anyway,
            // so this edge case should not arise in normal usage and the additional
            // code complexity to guard against it is not worth it.
            return true;
        } else if (curr2 != null) {
            // If we're already recursively comparing o1 to some other object,
            // this comparison is structurally false.
            return false;
        }

        final Object prev2 = knownEquals.get(o1);
        if (prev2 == o2) {
            // o1 known to be equal to o2.
            return true;
        } else if (prev2 != null) {
            // o1 known to be equal to something other than o2.
            return false;
        }

        final Object prev1 = knownEquals.get(o2);
        assert prev1 != o1; // otherwise we would've already returned at prev2 == o2
        if (prev1 != null) {
            // o2 known to be equal to something other than o1.
            return false;
        }

        currentlyCompared.put(o1, o2);
        final boolean eq = equalGraphsNoMemo(o1, o2);
        if (eq) {
            knownEquals.put(o1, o2);
            knownEquals.put(o2, o1);
        }
        currentlyCompared.remove(o1);
        return eq;
    }

    private boolean equalGraphsNoMemo(Object o1, Object o2) {
        if (o1 instanceof Wrapper) {
            return o2 instanceof Wrapper
                    && equalGraphs(((Wrapper) o1).unwrap(), ((Wrapper) o2).unwrap());
        } else if (o1 instanceof NativeJavaTopPackage) {
            // stateless objects, must check before Scriptable
            return o2 instanceof NativeJavaTopPackage;
        } else if (o1 instanceof Scriptable) {
            return o2 instanceof Scriptable && equalScriptables((Scriptable) o1, (Scriptable) o2);
        } else if (o1 instanceof SymbolKey) {
            return o2 instanceof SymbolKey
                    && equalGraphs(((SymbolKey) o1).getName(), ((SymbolKey) o2).getName());
        } else if (o1 instanceof Object[]) {
            return o2 instanceof Object[] && equalObjectArrays((Object[]) o1, (Object[]) o2);
        } else if (o1.getClass().isArray()) {
            return Objects.deepEquals(o1, o2);
        } else if (o1 instanceof List<?>) {
            return o2 instanceof List<?> && equalLists((List<?>) o1, (List<?>) o2);
        } else if (o1 instanceof Map<?, ?>) {
            return o2 instanceof Map<?, ?> && equalMaps((Map<?, ?>) o1, (Map<?, ?>) o2);
        } else if (o1 instanceof Set<?>) {
            return o2 instanceof Set<?> && equalSets((Set<?>) o1, (Set<?>) o2);
        } else if (o1 instanceof NativeGlobal) {
            return o2 instanceof NativeGlobal; // stateless objects
        } else if (o1 instanceof JavaAdapter) {
            return o2 instanceof JavaAdapter; // stateless objects
        }

        // Fallback case for everything else.
        return o1.equals(o2);
    }

    private boolean equalScriptables(final Scriptable s1, final Scriptable s2) {
        final Object[] ids1 = getSortedIds(s1);
        final Object[] ids2 = getSortedIds(s2);
        if (!equalObjectArrays(ids1, ids2)) {
            return false;
        }
        final int l = ids1.length;
        for (int i = 0; i < l; ++i) {
            if (!equalGraphs(getValue(s1, ids1[i]), getValue(s2, ids2[i]))) {
                return false;
            }
        }
        if (!equalGraphs(s1.getPrototype(), s2.getPrototype())) {
            return false;
        } else if (!equalGraphs(s1.getParentScope(), s2.getParentScope())) {
            return false;
        }

        // Handle special Scriptable implementations
        if (s1 instanceof NativeContinuation) {
            return s2 instanceof NativeContinuation
                    && NativeContinuation.equalImplementations(
                            (NativeContinuation) s1, (NativeContinuation) s2);
        } else if (s1 instanceof NativeJavaPackage) {
            return s1.equals(s2); // Overridden appropriately
        } else if (s1 instanceof IdFunctionObject) {
            return s2 instanceof IdFunctionObject
                    && IdFunctionObject.equalObjectGraphs(
                            (IdFunctionObject) s1, (IdFunctionObject) s2, this);
        } else if (s1 instanceof InterpretedFunction) {
            return s2 instanceof InterpretedFunction
                    && equalInterpretedFunctions(
                            (InterpretedFunction) s1, (InterpretedFunction) s2);
        } else if (s1 instanceof ArrowFunction) {
            return s2 instanceof ArrowFunction
                    && ArrowFunction.equalObjectGraphs(
                            (ArrowFunction) s1, (ArrowFunction) s2, this);
        } else if (s1 instanceof BoundFunction) {
            return s2 instanceof BoundFunction
                    && BoundFunction.equalObjectGraphs(
                            (BoundFunction) s1, (BoundFunction) s2, this);
        } else if (s1 instanceof NativeSymbol) {
            return s2 instanceof NativeSymbol
                    && equalGraphs(((NativeSymbol) s1).getKey(), ((NativeSymbol) s2).getKey());
        }
        return true;
    }

    private boolean equalObjectArrays(final Object[] a1, final Object[] a2) {
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; ++i) {
            if (!equalGraphs(a1[i], a2[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean equalLists(final List<?> l1, final List<?> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }
        final Iterator<?> i1 = l1.iterator();
        final Iterator<?> i2 = l2.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            if (!equalGraphs(i1.next(), i2.next())) {
                return false;
            }
        }
        assert !(i1.hasNext() || i2.hasNext());
        return true;
    }

    @SuppressWarnings("rawtypes")
    private boolean equalMaps(final Map<?, ?> m1, final Map<?, ?> m2) {
        if (m1.size() != m2.size()) {
            return false;
        }
        final Iterator<Map.Entry> i1 = sortedEntries(m1);
        final Iterator<Map.Entry> i2 = sortedEntries(m2);

        while (i1.hasNext() && i2.hasNext()) {
            final Map.Entry kv1 = i1.next();
            final Map.Entry kv2 = i2.next();
            if (!(equalGraphs(kv1.getKey(), kv2.getKey())
                    && equalGraphs(kv1.getValue(), kv2.getValue()))) {
                return false;
            }
        }
        assert !(i1.hasNext() || i2.hasNext());
        // TODO: assert linked maps traversal order?
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Iterator<Map.Entry> sortedEntries(final Map m) {
        // Yes, this throws ClassCastException if the keys aren't comparable. That's okay. We only
        // support maps with
        // deterministic traversal order.
        final Map sortedMap = (m instanceof SortedMap<?, ?> ? m : new TreeMap(m));
        return sortedMap.entrySet().iterator();
    }

    private boolean equalSets(final Set<?> s1, final Set<?> s2) {
        return equalObjectArrays(sortedSet(s1), sortedSet(s2));
    }

    private static Object[] sortedSet(final Set<?> s) {
        final Object[] a = s.toArray();
        Arrays.sort(a); // ClassCastException possible
        return a;
    }

    private static boolean equalInterpretedFunctions(
            final InterpretedFunction f1, final InterpretedFunction f2) {
        return Objects.equals(f1.getRawSource(), f2.getRawSource());
    }

    // Sort IDs deterministically
    private static Object[] getSortedIds(final Scriptable s) {
        final Object[] ids = getIds(s);
        Arrays.sort(
                ids,
                (a, b) -> {
                    if (a instanceof Integer) {
                        if (b instanceof Integer) {
                            return ((Integer) a).compareTo((Integer) b);
                        } else if (b instanceof String || b instanceof Symbol) {
                            return -1; // ints before strings or symbols
                        }
                    } else if (a instanceof String) {
                        if (b instanceof String) {
                            return ((String) a).compareTo((String) b);
                        } else if (b instanceof Integer) {
                            return 1; // strings after ints
                        } else if (b instanceof Symbol) {
                            return -1; // strings before symbols
                        }
                    } else if (a instanceof Symbol) {
                        if (b instanceof Symbol) {
                            // As long as people bother to reasonably name their symbols,
                            // this will work. If there's clashes in symbol names (e.g.
                            // lots of unnamed symbols) it can lead to false inequalities.
                            return getSymbolName((Symbol) a).compareTo(getSymbolName((Symbol) b));
                        } else if (b instanceof Integer || b instanceof String) {
                            return 1; // symbols after ints and strings
                        }
                    }
                    // We can only compare Rhino key types: Integer, String, Symbol
                    throw new ClassCastException();
                });
        return ids;
    }

    private static String getSymbolName(final Symbol s) {
        if (s instanceof SymbolKey) {
            return ((SymbolKey) s).getName();
        } else if (s instanceof NativeSymbol) {
            return ((NativeSymbol) s).getKey().getName();
        } else {
            // We can only handle native Rhino Symbol types
            throw new ClassCastException();
        }
    }

    private static Object[] getIds(final Scriptable s) {
        if (s instanceof ScriptableObject) {
            // Grabs symbols too
            return ((ScriptableObject) s).getIds(true, true);
        } else if (s instanceof DebuggableObject) {
            return ((DebuggableObject) s).getAllIds();
        } else {
            return s.getIds();
        }
    }

    private static Object getValue(final Scriptable s, final Object id) {
        if (id instanceof Symbol) {
            return ScriptableObject.getProperty(s, (Symbol) id);
        } else if (id instanceof Integer) {
            return ScriptableObject.getProperty(s, (Integer) id);
        } else if (id instanceof String) {
            return ScriptableObject.getProperty(s, (String) id);
        } else {
            throw new ClassCastException();
        }
    }
}
