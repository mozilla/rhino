/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Unit tests to check forEach loops.
 *
 * @author Roland Praml
 */
public class ForEachForOfTest {

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
    public void forEach() {
        testList("dtos.forEach(dto => { dto.data = 'bar' })");
        testArray("dtos.forEach(dto => { dto.data = 'bar' })");
        testSet("dtos.forEach(dto => { dto.data = 'bar' })");
        testMap("dtos.forEach(dto => { dto.data = 'bar' })");
    }

    @Test
    public void forOf() {
        testList("for (var dto of dtos) { dto.data = 'bar' }");
        testArray("for (var dto of dtos) { dto.data = 'bar' }");
        testSet("for (var dto of dtos) { dto.data = 'bar' }");
        testMap("for (var dto of dtos) { dto[0].data = 'bar' }");
        testMap("for (var dto of dtos) { dto[1].data = 'bar' }");
    }

    @Test
    public void forEachStrict() {
        testList("'use strict'; dtos.forEach(dto => { dto.data = 'bar' })");
        testArray("'use strict'; dtos.forEach(dto => { dto.data = 'bar' })");
        testSet("'use strict'; dtos.forEach(dto => { dto.data = 'bar' })");
        testMap("'use strict'; dtos.forEach(dto => { dto.data = 'bar' })");
    }

    @Test
    public void forOfStrict() {
        testList("'use strict'; for (var dto of dtos) { dto.data = 'bar' }");
        testArray("'use strict'; for (var dto of dtos) { dto.data = 'bar' }");
        testSet("'use strict'; for (var dto of dtos) { dto.data = 'bar' }");
        testMap("'use strict'; for (var dto of dtos) { dto[0].data = 'bar' }");
        testMap("'use strict'; for (var dto of dtos) { dto[1].data = 'bar' }");
    }

    private static void testList(final String script) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Global scope = new Global();
                    scope.init(cx);

                    Dto dto = new Dto();
                    dto.setData("foo");
                    List<Dto> dtos = Arrays.asList(dto);
                    scope.put("dtos", scope, dtos);

                    cx.evaluateString(scope, script, "myScript.js", 1, null);
                    Assert.assertEquals("bar", dto.getData());
                    return null;
                });
    }

    private static void testArray(final String script) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Global scope = new Global();
                    scope.init(cx);

                    Dto dto = new Dto();
                    dto.setData("foo");
                    Dto[] dtos = new Dto[] {dto};
                    scope.put("dtos", scope, dtos);

                    cx.evaluateString(scope, script, "myScript.js", 1, null);
                    Assert.assertEquals("bar", dto.getData());
                    return null;
                });
    }

    private static void testSet(final String script) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Global scope = new Global();
                    scope.init(cx);

                    Dto dto = new Dto();
                    dto.setData("foo");
                    Set<Dto> dtos = new HashSet<>();
                    dtos.add(dto);
                    scope.put("dtos", scope, dtos);

                    cx.evaluateString(scope, script, "myScript.js", 1, null);
                    Assert.assertEquals("bar", dto.getData());
                    return null;
                });
    }

    private static void testMap(final String script) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Global scope = new Global();
                    scope.init(cx);

                    Dto dto = new Dto();
                    dto.setData("foo");
                    Map<Dto, Dto> dtos = new HashMap<>();
                    dtos.put(dto, dto);
                    scope.put("dtos", scope, dtos);

                    cx.evaluateString(scope, script, "myScript.js", 1, null);
                    Assert.assertEquals("bar", dto.getData());
                    return null;
                });
    }
}
