/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript.tests;

import static java.util.Locale.FRANCE;
import static java.util.Locale.US;
import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public class MessageProviderLanguageTest {

    // Scottish Gaelic Language- assumption is this language does not have a translation
    private final Locale NOT_TRANSLATED = new Locale("gd");

    private final String MESSAGE_ID = "msg.invalid.date";
    private final String BASE_MESSAGE = "Date is invalid.";
    private final String FRENCH_MESSAGE = "Date incorrecte.";
    private final String ENGLISH_MESSAGE = BASE_MESSAGE;

    private final Locale DEFAULT_LOCALE = Locale.getDefault();

    private Context cx;

    @Before
    public void init() {
        cx = Context.enter();
    }

    @After
    public void cleanup() {
        Context.exit();
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    public void defaultLocaleUsesEnglishLanguageAndContextLocaleIsNotSpecified() {
        assertEquals(ENGLISH_MESSAGE, getMessageUsingDefaultLocale(US));
    }

    @Test
    public void defaultLocaleUsesFrenchLanguageAndContextLocaleIsNotSpecified() {
        assertEquals(FRENCH_MESSAGE, getMessageUsingDefaultLocale(FRANCE));
    }

    @Test
    public void defaultLocaleUsesUntranslatedLanguageAndContextLocaleIsNotSpecified() {
        assertEquals(BASE_MESSAGE, getMessageUsingDefaultLocale(NOT_TRANSLATED));
    }

    @Test
    public void defaultLocaleUsesEnglishLanguageAndContextLocaleIsTheSame() {
        assertEquals(ENGLISH_MESSAGE, getMessageUsingContextLocale(US, US));
    }

    @Test
    public void defaultLocaleUsesEnglishLanguageAndContextLocaleUsesFrenchLanguage() {
        assertEquals(FRENCH_MESSAGE, getMessageUsingContextLocale(FRANCE, US));
    }

    @Test
    public void defaultLocaleUsesEnglishLanguageAndContextLocaleIsNotTranslated() {
        assertEquals(ENGLISH_MESSAGE, getMessageUsingContextLocale(NOT_TRANSLATED, US));
    }

    @Test
    public void defaultLocaleUsesFrenchLanguageAndContextLocaleIsTheSame() {
        assertEquals(FRENCH_MESSAGE, getMessageUsingContextLocale(FRANCE, FRANCE));
    }

    @Test
    public void defaultLocaleUsesFrenchLanguageAndContextLocaleUsesEnglishLanguage() {
        assertEquals(ENGLISH_MESSAGE, getMessageUsingContextLocale(US, FRANCE));
    }

    @Test
    public void defaultLocaleUsesFrenchLanguageAndContextLocaleIsNotTranslated() {
        assertEquals(FRENCH_MESSAGE, getMessageUsingContextLocale(NOT_TRANSLATED, FRANCE));
    }

    @Test
    public void defaultLocaleIsNotTranslatedAndContextLocaleIsTheSame() {
        assertEquals(BASE_MESSAGE, getMessageUsingContextLocale(NOT_TRANSLATED, NOT_TRANSLATED));
    }

    @Test
    public void defaultLocaleIsNotTranslatedAndContextLocaleUsesEnglishLanguage() {
        assertEquals(ENGLISH_MESSAGE, getMessageUsingContextLocale(US, NOT_TRANSLATED));
    }

    @Test
    public void defaultLocaleIsNotTranslatedAndContextLocaleUsesFrenchLanguage() {
        assertEquals(FRENCH_MESSAGE, getMessageUsingContextLocale(FRANCE, NOT_TRANSLATED));
    }

    private String getMessageUsingDefaultLocale(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
        return ScriptRuntime.getMessageById(MESSAGE_ID);
    }

    private String getMessageUsingContextLocale(Locale contextLocale, Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
        cx.setLocale(contextLocale);
        return ScriptRuntime.getMessageById(MESSAGE_ID);
    }
}
