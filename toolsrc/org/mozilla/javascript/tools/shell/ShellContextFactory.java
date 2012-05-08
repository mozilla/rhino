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
 *   Igor Bukanov
 *   Bob Jervis
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

import org.mozilla.javascript.*;

public class ShellContextFactory extends ContextFactory
{
    private boolean strictMode;
    private boolean warningAsError;
    private int languageVersion = Context.VERSION_1_7;
    private int optimizationLevel;
    private boolean generatingDebug;
    private boolean allowReservedKeywords = true;
    private ErrorReporter errorReporter;
    private String characterEncoding;

    @Override
    protected boolean hasFeature(Context cx, int featureIndex)
    {
        switch (featureIndex) {
          case Context.FEATURE_STRICT_VARS:
          case Context.FEATURE_STRICT_EVAL:
          case Context.FEATURE_STRICT_MODE:
            return strictMode;

          case Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER:
            return allowReservedKeywords;

          case Context.FEATURE_WARNING_AS_ERROR:
            return warningAsError;

          case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
            return generatingDebug;
        }
        return super.hasFeature(cx, featureIndex);
    }

    @Override
    protected void onContextCreated(Context cx)
    {
        cx.setLanguageVersion(languageVersion);
        cx.setOptimizationLevel(optimizationLevel);
        if (errorReporter != null) {
            cx.setErrorReporter(errorReporter);
        }
        cx.setGeneratingDebug(generatingDebug);
        super.onContextCreated(cx);
    }

    public void setStrictMode(boolean flag)
    {
        checkNotSealed();
        this.strictMode = flag;
    }

    public void setWarningAsError(boolean flag)
    {
        checkNotSealed();
        this.warningAsError = flag;
    }

    public void setLanguageVersion(int version)
    {
        Context.checkLanguageVersion(version);
        checkNotSealed();
        this.languageVersion = version;
    }

    public void setOptimizationLevel(int optimizationLevel)
    {
        Context.checkOptimizationLevel(optimizationLevel);
        checkNotSealed();
        this.optimizationLevel = optimizationLevel;
    }

    public void setErrorReporter(ErrorReporter errorReporter)
    {
        if (errorReporter == null) throw new IllegalArgumentException();
        this.errorReporter = errorReporter;
    }

    public void setGeneratingDebug(boolean generatingDebug)
    {
        this.generatingDebug = generatingDebug;
    }

    public String getCharacterEncoding()
    {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding)
    {
        this.characterEncoding = characterEncoding;
    }

    public void setAllowReservedKeywords(boolean allowReservedKeywords) {
        this.allowReservedKeywords = allowReservedKeywords;
    }
}
