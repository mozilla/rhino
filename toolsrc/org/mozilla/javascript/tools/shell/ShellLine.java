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
 * May 6, 1998.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Matthieu Riou
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

package org.mozilla.javascript.tools.shell;

import java.io.InputStream;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;

import org.mozilla.javascript.Kit;

/**
 * Provides a specialized input stream for consoles to handle line
 * editing, history and completion. Relies on the JLine library (see
 * <http://jline.sourceforge.net>).
 */
public class ShellLine {

    public static InputStream getStream() {
        // We don't want a compile-time dependency on the JLine jar, so use
        // reflection to load and reference the JLine classes.
        ClassLoader classLoader = ShellLine.class.getClassLoader();
        Class<?> readerClass = Kit.classOrNull(classLoader,
                                               "jline.ConsoleReader");
        if (readerClass == null)
            return null;
        try {
            // ConsoleReader reader = new ConsoleReader();
            Constructor<?> c = readerClass.getConstructor();
            Object reader = c.newInstance();

            // reader.setBellEnabled(false);
            Method m = readerClass.getMethod("setBellEnabled", Boolean.TYPE);
            m.invoke(reader, Boolean.FALSE);

            String[] prefixes = { "arguments", "defineClass(", "defineClass(",
                "deserialize(", "load(", "print(", "readFile(", "readUrl(",
                "runCommand", "seal(", "serialize(", "spawn(", "sync(",
                "quit()", "version(" };

            // reader.addCompletor(new FlexibleCompletor(prefixes));
            Class<?> completorClass = Kit.classOrNull(classLoader,
                "jline.Completor");
            m = readerClass.getMethod("addCompletor", completorClass);
            Object completor = Proxy.newProxyInstance(classLoader,
                    new Class[] { completorClass },
                    new FlexibleCompletor(completorClass, prefixes));
            m.invoke(reader, completor);

            // return new ConsoleReaderInputStream(reader);
            Class<?> inputStreamClass = Kit.classOrNull(classLoader,
                "jline.ConsoleReaderInputStream");
            c = inputStreamClass.getConstructor(readerClass);
            return (InputStream) c.newInstance(reader);
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return null;
    }
}

/**
 * The completors provided with JLine are pretty uptight, they only
 * complete on a line that it can fully recognize (only composed of
 * completed strings). This one completes whatever came before.
 */
class FlexibleCompletor implements java.lang.reflect.InvocationHandler {
    private String[] radicals;
    private Method completeMethod;

    FlexibleCompletor(Class<?> completorClass, String... radicals)
        throws NoSuchMethodException
    {
        this.radicals = radicals;
        this.completeMethod = completorClass.getMethod("complete", String.class,

                Integer.TYPE, List.class);
    }

    @SuppressWarnings({"unchecked"})
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.equals(this.completeMethod)) {
            int result = complete((String)args[0],
                                  ((Integer) args[1]).intValue(),
                                  (List<String>) args[2]);
            return new Integer(result);
        }
        throw new NoSuchMethodError(method.toString());
    }

    public int complete(String buffer, int cursor, List<String> candidates) {
        int m = cursor - 1;
        for (; m >= 0; m--) if (!Character.isLetter(buffer.charAt(m))) break;
        String lastPart = buffer.substring(m+1, cursor);
        for (String radical : radicals) {
            if (radical.startsWith(lastPart)) candidates.add(radical);
        }
        return buffer.length()-lastPart.length();
    }
}
