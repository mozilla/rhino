package org.mozilla.javascript;

import java.io.Serializable;

/** Can provide source code for a built in Function */
public class BuiltInSourceCodeProvider implements SourceCodeProvider, Serializable {
    private static final long serialVersionUID = 1L;
    public static final BuiltInSourceCodeProvider INSTANCE = new BuiltInSourceCodeProvider();

    private BuiltInSourceCodeProvider() {}

    @Override
    public String getSource(JSDescriptor<?> desc, int start, int end) {
        return String.format("function %s() {\n\t[native code]\n}", desc.getFunctionName());
    }
}
