/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

/**
 * Architecture tests.
 *
 * @author Ronald Brill
 */
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packages = "org.mozilla.javascript",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    /** Use only the RegExpProxy. */
    @ArchTest
    public static final ArchRule regExpProxyRule =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("org.mozilla.javascript.regexp..")
                    .and()
                    .doNotHaveFullyQualifiedName("org.mozilla.javascript.RegExpProxy")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.mozilla.javascript.regexp..");
}
