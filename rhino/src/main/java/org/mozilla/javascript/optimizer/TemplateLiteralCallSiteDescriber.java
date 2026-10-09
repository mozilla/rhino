/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.javascript.TemplateLiteralCallSite;

/**
 * Describes {@link TemplateLiteralCallSite} as a dynamic constant reconstructed from its encoded
 * values and raw values.
 *
 * <p>Each template literal must produce its own site object, even when two have the same contents,
 * so an identity hash is included to keep distinct call sites from sharing a constant.
 */
public class TemplateLiteralCallSiteDescriber
        implements DynamicConstantDescriber<TemplateLiteralCallSite> {

    private static final ClassDesc CD_TEMPLATE_LITERAL_CALL_SITE =
            ClassDesc.of(TemplateLiteralCallSite.class.getName());

    private static final DirectMethodHandleDesc CALL_SITE_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_TEMPLATE_LITERAL_CALL_SITE,
                    "templateLiteralCallSiteConstant",
                    CD_TEMPLATE_LITERAL_CALL_SITE,
                    ConstantDescs.CD_String,
                    ConstantDescs.CD_String,
                    ConstantDescs.CD_int);

    @Override
    public Class<TemplateLiteralCallSite> describedType() {
        return TemplateLiteralCallSite.class;
    }

    @Override
    public DynamicConstantDesc<TemplateLiteralCallSite> describe(TemplateLiteralCallSite value) {
        return DynamicConstantDesc.ofNamed(
                CALL_SITE_CONSTANT,
                "callSite",
                CD_TEMPLATE_LITERAL_CALL_SITE,
                value.encodedValues(),
                value.encodedRawValues(),
                Integer.valueOf(System.identityHashCode(value)));
    }
}
