/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Unit tests to check forEach loops.
 *
 * @author Roland Praml
 */
public class ForEachForOfTest {

    private ContextFactory contextFactoryIntl402 =
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == Context.FEATURE_E4X) {
                        return true;
                    }
                    return super.hasFeature(cx, featureIndex);
                }
            };

    public static class Dto {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    @Test
    public void testForEach() {
        testIt("dtos.forEach(dto => { dto.data = 'bar' })");
    }

    @Test
    public void testForOf() {
        testIt("for (var dto of dtos) { dto.data = 'bar' }");
    }

    @Test
    public void testForEachStrict() {
        testIt("'use strict'; dtos.forEach(dto => { dto.data = 'bar' })");
    }

    @Test
    public void testForOfStrict() {
        testIt("'use strict'; for (var dto of dtos) { dto.data = 'bar' }");
    }

    private void testIt(final String script) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Global scope = new Global();
                    scope.init(cx);
                    List<Dto> dtos = new ArrayList<>();

                    Dto dto = new Dto();
                    dto.setData("foo");
                    dtos.add(dto);
                    scope.put("dtos", scope, dtos);

                    Object ret = cx.evaluateString(scope, script, "myScript.js", 1, null);
                    if (ret instanceof Wrapper) {
                        ret = ((Wrapper) ret).unwrap();
                    }
                    Assert.assertEquals("bar", dtos.get(0).getData());
                    return null;
                });
    }
}
