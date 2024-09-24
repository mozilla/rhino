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
                    // Get an object property with a constant name
                    return StandardOperation.GET
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "GETNOWARN":
                    // Same with no warning of strict mode
                    return RhinoOperation.GETNOWARN
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "GETWITHTHIS":
                    // Same but also return "this" so that it is found by "lastStoredScriptable"
                    return RhinoOperation.GETWITHTHIS
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "GETELEMENT":
                    // Get the value of an element from a property that is on the stack,\
                    // as if using "[]" notation. Could be a String, number, or Symbol
                    return RhinoOperation.GETELEMENT.withNamespace(StandardNamespace.PROPERTY);
                case "GETINDEX":
                    // Same but the value is definitely a numeric index
                    return RhinoOperation.GETINDEX.withNamespace(StandardNamespace.PROPERTY);
                case "SET":
                    // Set an object property with a constant name
                    return StandardOperation.SET
                            .withNamespace(StandardNamespace.PROPERTY)
                            .named(getNameSegment(tokens, name, 2));
                case "SETELEMENT":
                    // Set an object property as if by "[]", with a property on the stack
                    return RhinoOperation.SETELEMENT.withNamespace(StandardNamespace.PROPERTY);
                case "SETINDEX":
                    // Same but the property name is definitely a number
                    return RhinoOperation.SETINDEX.withNamespace(StandardNamespace.PROPERTY);
            }
        } else if ("NAME".equals(namespaceName)) {
            switch (opName) {
                case "BIND":
                    // Bind a new variable to the context with a constant name
                    return RhinoOperation.BIND
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "GET":
                    // Get a variable from the context with a constant name
                    return StandardOperation.GET
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "GETWITHTHIS":
                    // Same but also return "this" so that it is found by "lastStoredScriptable"
                    return RhinoOperation.GETWITHTHIS
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "SET":
                    // Set an object in the context with a constant name
                    return StandardOperation.SET
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "SETSTRICT":
                    // Same but implement strict mode checks
                    return RhinoOperation.SETSTRICT
                            .withNamespace(RhinoNamespace.NAME)
                            .named(getNameSegment(tokens, name, 2));
                case "SETCONST":
                    // Same but try to set a constant
                    return RhinoOperation.SETCONST
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
    private static String getNameSegment(String[] segments, String name, int pos) {
        if (pos >= segments.length) {
            return "";
        }
        // Because segments of operation names, especially property names, are essentially
        // wired in to the bootstrapping result, interning works and has a big impact on
        // performance.
        return segments[pos].intern();
    }
}
