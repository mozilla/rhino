package org.mozilla.javascript;

/**
 * This is a specialization of Slot to store values that are retrieved via calls to script
 * functions. It's used to load built-in objects more efficiently.
 */
public class LazyLoadSlot<T extends PropHolder<T>> extends Slot<T> {
    LazyLoadSlot(Object name, int index) {
        super(name, index, 0);
    }

    LazyLoadSlot(Slot<T> oldSlot) {
        super(oldSlot);
    }

    @Override
    LazyLoadSlot<T> copySlot() {
        var newSlot = new LazyLoadSlot<T>(this);
        newSlot.value = value;
        newSlot.next = null;
        newSlot.orderedNext = null;
        return newSlot;
    }

    @Override
    public Object getValue(T start) {
        Object val = this.value;
        if (val instanceof LazilyLoadedCtor) {
            LazilyLoadedCtor initializer = (LazilyLoadedCtor) val;
            try {
                initializer.init();
            } finally {
                this.value = val = initializer.getValue();
            }
        }
        return val;
    }
}
