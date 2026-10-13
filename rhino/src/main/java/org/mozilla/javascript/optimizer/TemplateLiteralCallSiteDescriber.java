/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.util.Map;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;
import org.mozilla.javascript.TemplateLiteralCallSite;

/**
 * Describes {@link TemplateLiteralCallSite} as a dynamic constant reconstructed from its encoded
 * values and raw values.
 *
 * <p>Each template literal must produce its own site object, even when two have the same contents,
 * so an identity hash is included to keep distinct call sites from sharing a constant.
 */
public class TemplateLiteralCallSiteDescriber
        extends DynamicConstantDescriber<TemplateLiteralCallSite> {

    private Map<Class<?>, DynamicConstantDescriber<?>> registry;

    private static final ClassDesc CD_TEMPLATE_LITERAL_CALL_SITE =
            ClassDesc.of(TemplateLiteralCallSite.class.getName());

    private static final DirectMethodHandleDesc CALL_SITE_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_TEMPLATE_LITERAL_CALL_SITE,
                    "templateLiteralCallSiteConstant",
                    CD_TEMPLATE_LITERAL_CALL_SITE,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_Object.arrayType());

    public TemplateLiteralCallSiteDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<TemplateLiteralCallSite> describedType() {
        return TemplateLiteralCallSite.class;
    }

    @Override
    public DynamicConstantDesc<TemplateLiteralCallSite> describe(TemplateLiteralCallSite value) {
        var values = value.encoded();
        var descs = new ConstantDesc[values.length + 1];
        descs[0] = Integer.valueOf(System.identityHashCode(value));

        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof ConstantDesc cd) {
                descs[i + 1] = cd;
            } else {
                descs[i + 1] = getRegistry().describe(values[i]);
            }
        }

        return DynamicConstantDesc.ofNamed(
                CALL_SITE_CONSTANT, "callSite", CD_TEMPLATE_LITERAL_CALL_SITE, descs);
    }
}
