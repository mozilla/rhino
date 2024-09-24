package org.mozilla.javascript.optimizer;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.regex.Pattern;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.support.SimpleRelinkableCallSite;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;

/**
 * The Bootstrapper contains the method that is called by invokedynamic instructions in the bytecode
 * to map a call site to a method.
 */
public class Bootstrapper {
    private static final Pattern SEPARATOR = Pattern.compile(":");

    public static final ClassFileWriter.MHandle BOOTSTRAP_HANDLE =
            new ClassFileWriter.MHandle(
                    ByteCode.MH_INVOKESTATIC,
                    "org.mozilla.javascript.optimizer.Bootstrapper",
                    "bootstrap",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;"
                            + "Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");

    private static final DynamicLinker linker;

    static {
        // Set up the linkers that will be invoked whenever a call site needs to be resolved.
        DynamicLinkerFactory factory = new DynamicLinkerFactory();
        factory.setPrioritizedLinkers(new DefaultLinker());
        linker = factory.createLinker();
    }

    /** This is the method called by every call site in the bytecode to map it to a function. */
    @SuppressWarnings("unused")
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType mType)
            throws NoSuchMethodException, IllegalAccessException {
        Operation op = parseOperation(name);
        // For now, use a very simple call site.
        // When we change to add more linkers, we will likely switch to ChainedCallSite.
        return linker.link(new SimpleRelinkableCallSite(new CallSiteDescriptor(lookup, op, mType)));
    }

    /**
     * Operation names in the bytecode are names like "PROP:GET:<NAME> and "NAME:BIND:<NAME>". This
     * translates them the first time a call site is seen to an object that can be easily consumed
     * by the various types of linkers.
     */
    private static Operation parseOperation(String name) throws NoSuchMethodException {
        String[] tokens = SEPARATOR.split(name, -1);
        String namespaceName = getNameSegment(tokens, name, 0);
        String opName = getNameSegment(tokens, name, 1);

        if ("PROP".equals(namespaceName)) {
            switch (opName) {
                case "GET":
                    return StandardOperation.GET
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "GETNOWARN":
                    return RhinoOperation.GETNOWARN
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "GETWITHTHIS":
                    return RhinoOperation.GETWITHTHIS
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "SET":
                    return StandardOperation.SET
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
            }
        } else if ("NAME".equals(namespaceName)) {
            switch (opName) {
                case "BIND":
                    return RhinoOperation.BIND
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "GET":
                    return StandardOperation.GET
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "GETWITHTHIS":
                    return RhinoOperation.GETWITHTHIS
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
            }
        }

        // Fall through to no match. This should only happen if the name in the bytecode
        // does not match the pattern that this method understands.
        throw new NoSuchMethodException(name);
    }

    // Given a list of name segments and a position, return the interned name at the
    // specified position.
    private static String getNameSegment(String[] segments, String name, int pos)
            throws NoSuchMethodException {
        if (pos >= segments.length) {
            throw new NoSuchMethodException(name);
        }
        // Because segments of operation names, especially property names, are essentially
        // wired in to the bootstrapping result, interning works and has a big impact on
        // performance.
        return segments[pos].intern();
    }
}
