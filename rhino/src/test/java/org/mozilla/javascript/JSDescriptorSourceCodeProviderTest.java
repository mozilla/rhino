package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JSDescriptorSourceCodeProviderTest {
    private Context cx;

    @BeforeEach
    public void setUp() {
        cx = Context.enter();
    }

    @AfterEach
    public void tearDown() {
        Context.exit();
    }

    private static SourceCodeProvider sourceCodeProviderOf(JSDescriptor<?> descriptor)
            throws ReflectiveOperationException {
        return descriptor.getSourceProvider();
    }

    @Test
    public void compilingWithSourceCodeSupplierUsesLazyProvider()
            throws ReflectiveOperationException {
        String source = "var x = 1;";

        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromSource(source)
                                .sourceName("test.js")
                                .sourceCodeSupplier(() -> source)
                                .build());

        JSDescriptor<JSScript> descriptor = script.getDescriptor();
        assertNotNull(descriptor);
        assertInstanceOf(LazySourceCodeProvider.class, sourceCodeProviderOf(descriptor));
    }

    @Test
    public void lazyProviderReplacesItselfOnAccess() throws ReflectiveOperationException {
        String source = "var x = 1;";

        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromSource(source)
                                .sourceName("test.js")
                                .sourceCodeSupplier(() -> source)
                                .build());

        JSDescriptor<JSScript> descriptor = script.getDescriptor();
        assertNotNull(descriptor);
        assertInstanceOf(LazySourceCodeProvider.class, sourceCodeProviderOf(descriptor));
        String src1 = descriptor.getSource();
        assertInstanceOf(
                LazySourceCodeProvider.ResolvedSourceProvider.class,
                sourceCodeProviderOf(descriptor));
        String src2 = descriptor.getSource();
        assertEquals(src1, src2);
    }

    @Test
    public void compilingWithoutSourceCodeSupplierUsesEagerProvider()
            throws ReflectiveOperationException {
        String source = "var x = 1;";

        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromSource(source).sourceName("test.js").build());

        JSDescriptor<JSScript> descriptor = script.getDescriptor();
        assertNotNull(descriptor);
        assertInstanceOf(EagerSourceCodeProvider.class, sourceCodeProviderOf(descriptor));
    }
}
