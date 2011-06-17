/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   John R. Rose, Sun Microsystems
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

// API class

package org.mozilla.javascript.classy;

import java.util.*;

/**
 * Data structure for profiling call sites.
 * Supports polymorphism.
 * <p>
 *
 * @see org.mozilla.javascript.classy.ClassyCallSite
 * @author John R. Rose
 */

public abstract class TypeProfile {
    int count = 1;

    /** number of times this case has been seen */
    public int count() { return count; }
    /** number of subcases */
    public int morphism() { return 1; }

    public TypeProfile leadCase() { return this; }

    public boolean matchAndIncrement(Object x) {
        if (!match(x))
            return false;
        incrementCount(1);
        return true;
    }
    public int incrementCount(int d) {
        int c1 = count + d;
        if (c1 <= 0)
            return c1 - d;  // freeze count on overflow
        count = c1;
        return c1;
    }

    abstract public String toString();

    public boolean isMonomorphic() {
        return !(this instanceof Polymorph);
    }
    public TypeProfile[] cases() {
        return new TypeProfile[]{ this };
    }
    public TypeProfile append(TypeProfile that) {
        if (that == null || that == this)  return this;
        return new Polymorph(this, that);
    }
    public abstract boolean match(Object x);

    public static TypeProfile forSingleton(Object token) {
        return new ForSingleton(token);
    }
    public static final class ForSingleton extends TypeProfile {
        final Object token;
        ForSingleton(Object token) { this.token = token; }
        public Object matchObject() { return token; }
        public boolean match(Object x) {
            return (x == token);
        }
        public String toString() {
            return ""+count+" of =="+token;
        }
    }

    public static TypeProfile forLayout(ClassyLayout token) {
        return new ForLayout(token);
    }
    public static final class ForLayout extends TypeProfile {
        final ClassyLayout token;
        ForLayout(ClassyLayout token) { this.token = token; }
        public ClassyLayout matchLayout() { return token; }
        public boolean match(Object x) {
            return (x instanceof ClassyLayout.HasLayout
                    && ((ClassyLayout.HasLayout)x).getLayout() == token);
        }
        public String toString() {
            return ""+count+" of "+token;
        }
    }

    public static TypeProfile forClass(Class<?> token) {
        return new ForClass(token);
    }
    public static final class ForClass extends TypeProfile {
        final Class<?> token;
        ForClass(Class<?> token) { this.token = token; }
        public Class<?> matchClass() { return token; }
        public boolean match(Object x) {
            return token.isInstance(x);
        }
        public String toString() {
            return ""+count+" of <:"+token.getSimpleName();
        }
    }

    static final class Polymorph extends TypeProfile {
        final ArrayList<TypeProfile> cases;
        Polymorph(TypeProfile prof) {
            count = 0;
            this.cases = new ArrayList<TypeProfile>(4);
            add(prof);
        }
        Polymorph(TypeProfile prof, TypeProfile prof2) {
            this(prof);
            add(prof2);
        }
        private void add(TypeProfile prof) {
            notPoly(prof);
            for (int pos = cases.size(); ; pos--) {
                if (pos == 0 || cases.get(pos-1).count() >= prof.count()) {
                    cases.add(pos, prof);
                    break;
                }
            }
        }
        public TypeProfile append(TypeProfile prof) {
            add(prof);
            return this;
        }
        public int morphism() {
            return cases.size();
        }
        public int count() {
            int sum = this.count;
            for (TypeProfile prof : cases) {
                sum += prof.count();
            }
            return sum;
        }
        public boolean match(Object x) {
            for (TypeProfile prof : cases) {
                if (prof.match(x))
                    return true;
            }
            return false;
        }
        public boolean matchAndIncrement(Object x) {
            TypeProfile prev = null;
            for (TypeProfile prof : cases) {
                if (prof.match(x)) {
                    int nc = prof.incrementCount(1);
                    if (prev != null && prev.count() < nc) {
                        int pos = cases.indexOf(prof);
                        for (;;) {
                            Collections.swap(cases, pos-1, pos);
                            pos--;
                            if (pos == 0 || cases.get(pos-1).count() >= nc)
                                break;
                        }
                    }
                    return true;
                }
                prev = prof;
            }
            return false;
        }
        public TypeProfile[] cases() {
            return cases.toArray(new TypeProfile[cases.size()]);
        }

        public TypeProfile leadCase() {
            return cases.get(0);
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            String sep = "";
            buf.append(""+morphism()+"[");
            for (TypeProfile prof : cases) {
                buf.append(sep); sep = ", ";
                buf.append(prof.toString());
            }
            buf.append("]");
            return buf.toString();
        }

        private static void notPoly(TypeProfile prof) {
            if (prof instanceof Polymorph)  throw new InternalError();
        }
    }
}
