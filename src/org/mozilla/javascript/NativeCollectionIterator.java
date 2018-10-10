package org.mozilla.javascript;

import java.io.Serializable;
import java.util.Iterator;

public class NativeCollectionIterator
    extends ES6Iterator
{
    private final Iterator<Hashtable.Entry> iterator;
    private final String className;
    private final Type type;

    private static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();

    /**
     * Collections.emptyIterator() is not serializable.
     */
    private static final class EmptyIterator implements Iterator, Serializable
    {
        @Override
        public Object next() { throw new AssertionError("EmptyIterator: Method next() called"); }

        @Override
        public boolean hasNext() { return false; }

        @Override
        public void remove() { throw new UnsupportedOperationException("EmptyIterator: Method remove() not supported"); }
    }


    enum Type { KEYS, VALUES, BOTH };

    static void init(ScriptableObject scope, String tag, boolean sealed) {
        ES6Iterator.init(scope, sealed, new NativeCollectionIterator(tag), tag);
    }

    public NativeCollectionIterator(String tag)
    {
        this.className = tag;
        this.iterator = EMPTY_ITERATOR;
        this.type = Type.BOTH;
    }

    public NativeCollectionIterator(
        Scriptable scope, String className,
        Type type, Iterator<Hashtable.Entry> iterator)
    {
        super(scope, className);
        this.className = className;
        this.iterator = iterator;
        this.type = type;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    protected boolean isDone(Context cx, Scriptable scope) {
        return !iterator.hasNext();
    }

    @Override
    protected Object nextValue(Context cx, Scriptable scope) {
        final Hashtable.Entry e = iterator.next();
        switch (type) {
            case KEYS:
                return e.key;
            case VALUES:
                return e.value;
            case BOTH:
                return cx.newArray(scope, new Object[] { e.key, e.value });
            default:
                throw new AssertionError();
        }
    }
}
