package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class RefNsMember extends Instruction {
    private final Operand object;
    private final Operand namespace;
    private final Operand property;
    private final int flags;

    public RefNsMember(Operand object, Operand namespace, Operand property, int flags) {
        this.object = object;
        this.namespace = namespace;
        this.property = property;
        this.flags = flags;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object propertyValue = property.retrieveAndWrap(cx, frame);
        Object namespaceValue = namespace.retrieveAndWrap(cx, frame);
        Object objectValue = object.retrieveAndWrap(cx, frame);

        Object result =
                ScriptRuntime.memberRef(objectValue, namespaceValue, propertyValue, cx, flags);
        frame.push(result);
    }

    @Override
    public int stackChange() {
        return 1 + object.stackChange() + namespace.stackChange() + property.stackChange();
    }
}
