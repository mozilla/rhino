package org.mozilla.javascript;

class NullSourceCodeProvider implements SourceCodeProvider {
    static final SourceCodeProvider NULL_PROVIDER = new NullSourceCodeProvider();

    private NullSourceCodeProvider() {}

    public String getSource(JSDescriptor<?> desc, int start, int end) {
        return String.format(
                "function %s() {\n\t/* Source unavailable */\n}", desc.getFunctionName());
    }
}
