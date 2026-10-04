package org.mozilla.javascript.reflect;

import java.util.Arrays;
import org.mozilla.javascript.Wrapper;

class ResolvedOverload {
    final Class<?>[] types;
    final int index;

    ResolvedOverload(Object[] args, int index) {
        this.index = index;
        types = new Class<?>[args.length];
        for (int i = 0, l = args.length; i < l; i++) {
            Object arg = args[i];
            if (arg instanceof Wrapper) arg = ((Wrapper) arg).unwrap();
            types[i] = arg == null ? null : arg.getClass();
        }
    }

    boolean matches(Object[] args) {
        if (args.length != types.length) {
            return false;
        }
        for (int i = 0, l = args.length; i < l; i++) {
            Object arg = args[i];
            if (arg instanceof Wrapper) arg = ((Wrapper) arg).unwrap();
            if (arg == null) {
                if (types[i] != null) return false;
            } else if (arg.getClass() != types[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ResolvedOverload)) {
            return false;
        }
        ResolvedOverload ovl = (ResolvedOverload) other;
        return Arrays.equals(types, ovl.types) && index == ovl.index;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types);
    }
}
