/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import org.mozilla.classfile.DynamicConstantDescriber;

/**
 * Describes compiled regular expressions as dynamic constants that are recompiled from their source
 * and flags when the constant is first resolved.
 *
 * <p>The described value is not the one that will exist at run time: it is a compiled form produced
 * by {@link RegExpImpl#prepareRegExpConstant}, which this reads the source and flags back out of.
 * Compiling it up front is what lets {@link NativeRegExp#regExpConstant} resolve the constant
 * without a context, since anything wrong with the expression has already been reported.
 *
 * <p>The source cannot be carried in the constant's name, because a name may not be empty and may
 * not contain the "[" and ";" characters that appear in ordinary regular expressions. Both the
 * source and the flags are therefore bootstrap arguments, and the name is a fixed placeholder.
 */
public class RECompiledDescriber implements DynamicConstantDescriber<RECompiled> {

    private static final ClassDesc CD_NATIVE_REG_EXP = ClassDesc.of(NativeRegExp.class.getName());

    private static final DirectMethodHandleDesc REGEXP_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_NATIVE_REG_EXP,
                    "regExpConstant",
                    ConstantDescs.CD_Object,
                    ConstantDescs.CD_String,
                    ConstantDescs.CD_String);

    @Override
    public Class<RECompiled> describedType() {
        return RECompiled.class;
    }

    @Override
    public DynamicConstantDesc<RECompiled> describe(RECompiled value) {
        return DynamicConstantDesc.ofNamed(
                REGEXP_CONSTANT,
                "regexp",
                // The compiled form is package private, so the constant is typed as an object,
                // which is also what everything consuming it expects.
                ConstantDescs.CD_Object,
                new String(value.source),
                flagsToString(value.flags));
    }

    /**
     * The flags in a canonical order, so that literals written with the same flags in a different
     * order share a constant.
     */
    private static String flagsToString(int flags) {
        var buf = new StringBuilder(6);
        if ((flags & NativeRegExp.JSREG_GLOB) != 0) buf.append('g');
        if ((flags & NativeRegExp.JSREG_FOLD) != 0) buf.append('i');
        if ((flags & NativeRegExp.JSREG_MULTILINE) != 0) buf.append('m');
        if ((flags & NativeRegExp.JSREG_DOTALL) != 0) buf.append('s');
        if ((flags & NativeRegExp.JSREG_STICKY) != 0) buf.append('y');
        if ((flags & NativeRegExp.JSREG_UNICODE) != 0) buf.append('u');
        return buf.toString();
    }
}
