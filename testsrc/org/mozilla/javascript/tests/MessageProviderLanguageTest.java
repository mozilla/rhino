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

    private final String messageId = "msg.invalid.date";
    private final String englishMessage = "Date is invalid.";
    private final String frenchMessage = "Date incorrecte.";
    private final String defaultMessage = englishMessage;

    private Context cx;

    @Before
    public void init() {
        cx = Context.enter();
    }

    @After
    public void cleanup() {
        Context.exit();
    }

    @Test
    public void defaultLocaleUsesDefaultLanguageAndContextLocaleIsNotSpecified() {
        assertEquals(englishMessage, getMessageUsingDefaultLocale(US));
    }

    @Test
    public void defaultLocaleUsesOtherTranslatedLanguageAndContextLocaleIsNotSpecified() {
        assertEquals(frenchMessage, getMessageUsingDefaultLocale(FRANCE));
    }

    @Test
    public void defaultLocaleUsesUntranslatedLanguageAndContextLocaleIsNotSpecified() {
        assertEquals(defaultMessage, getMessageUsingDefaultLocale(NOT_TRANSLATED));
    }

    @Test
    public void defaultLocaleUsesDefaultLanguageAndContextLocaleIsTheSame() {
        assertEquals(englishMessage, getMessageUsingContextLocale(US, US));
    }

    @Test
    public void defaultLocaleUsesDefaultLanguageAndContextLocaleUsesOtherTranslatedLanguage() {
        assertEquals(frenchMessage, getMessageUsingContextLocale(FRANCE, US));
    }

    @Test
    public void defaultLocaleUsesDefaultLanguageAndContextLocaleIsNotTranslated() {
        assertEquals(englishMessage, getMessageUsingContextLocale(NOT_TRANSLATED, US));
    }

    @Test
    public void defaultLocaleUsesOtherTranslatedLanguageAndContextLocaleIsTheSame() {
        assertEquals(frenchMessage, getMessageUsingContextLocale(FRANCE, FRANCE));
    }

    @Test
    public void defaultLocaleUsesOtherTranslatedLanguageAndContextLocaleUsesDefaultLanguage() {
        assertEquals(englishMessage, getMessageUsingContextLocale(US, FRANCE));
    }

    @Test
    public void defaultLocaleUsesOtherTranslatedLanguageAndContextLocaleIsNotTranslated() {
        assertEquals(frenchMessage, getMessageUsingContextLocale(NOT_TRANSLATED, FRANCE));
    }

    @Test
    public void defaultLocaleIsNotTranslatedAndContextLocaleIsTheSame() {
        assertEquals(defaultMessage, getMessageUsingContextLocale(NOT_TRANSLATED, NOT_TRANSLATED));
    }

    @Test
    public void defaultLocaleIsNotTranslatedAndContextLocaleUsesDefaultLanguage() {
        assertEquals(englishMessage, getMessageUsingContextLocale(US, NOT_TRANSLATED));
    }

    @Test
    public void defaultLocaleIsNotTranslatedAndContextLocaleUsesOtherTranslatedLanguage() {
        assertEquals(frenchMessage, getMessageUsingContextLocale(FRANCE, NOT_TRANSLATED));
    }

    private String getMessageUsingDefaultLocale(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
        return ScriptRuntime.getMessageById(messageId);
    }

    private String getMessageUsingContextLocale(Locale contextLocale, Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
        cx.setLocale(contextLocale);
        return ScriptRuntime.getMessageById(messageId);
    }
}
