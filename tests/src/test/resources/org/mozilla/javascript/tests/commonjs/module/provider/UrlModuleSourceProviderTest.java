/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.commonjs.module.provider;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlConnectionExpiryCalculator;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

public class UrlModuleSourceProviderTest {

    private static final UrlConnectionExpiryCalculator ALWAYS_CHECK_EXPIRED = urlConnection -> 0;

    @Test
    public void moduleNotModified() throws Exception {
        // given
        final Path filePath = Files.createTempFile("test", ".js");
        final ModuleSource result;
        try {
            final URI moduleURI = getModuleURI(filePath);
            final UrlModuleSourceProvider sourceProvider =
                    new UrlModuleSourceProvider(null, null, ALWAYS_CHECK_EXPIRED, null);
            final ModuleSource moduleSource = sourceProvider.loadSource(moduleURI, null, null);
            moduleSource.getReader().close();

            // when
            result = sourceProvider.loadSource(moduleURI, null, moduleSource.getValidator());
        } finally {
            Files.deleteIfExists(filePath);
        }

        // then
        Assert.assertEquals("Not modified", ModuleSourceProvider.NOT_MODIFIED, result);
    }

    @Test
    public void moduleModified() throws Exception {
        // given
        final Path filePath = Files.createTempFile("test", ".js");
        final ModuleSource result;
        try {
            final URI moduleURI = getModuleURI(filePath);
            final UrlModuleSourceProvider sourceProvider =
                    new UrlModuleSourceProvider(null, null, ALWAYS_CHECK_EXPIRED, null);
            final ModuleSource moduleSource = sourceProvider.loadSource(moduleURI, null, null);
            moduleSource.getReader().close();

            // when
            Files.setLastModifiedTime(filePath, FileTime.fromMillis(Long.MAX_VALUE));
            result = sourceProvider.loadSource(moduleURI, null, moduleSource.getValidator());
            result.getReader().close();
        } finally {
            Files.deleteIfExists(filePath);
        }

        // then
        Assert.assertNotNull(result);
        Assert.assertNotEquals("Modified", ModuleSourceProvider.NOT_MODIFIED, result);
    }

    @Test
    public void getCharacterEncodingCanBeModifiedInSubclass() throws NoSuchMethodException {
        Method method =
                UrlModuleSourceProvider.class.getDeclaredMethod(
                        "getCharacterEncoding", new Class[] {URLConnection.class});
        int mods = method.getModifiers();
        Assert.assertTrue(Modifier.isPublic(mods) || Modifier.isProtected(mods));
    }

    private static URI getModuleURI(final Path filePath) throws URISyntaxException {
        final String uriString = filePath.toUri().toASCIIString();
        return new URI(uriString.substring(0, uriString.lastIndexOf('.')));
    }
}
