package org.mozilla.javascript.optimizer;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.support.CompositeTypeBasedGuardingDynamicLinker;
import jdk.dynalink.support.ChainedCallSite;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;

/**
 * The Bootstrapper contains the method that is called by invokedynamic instructions in the bytecode
 * to map a call site to a method. The "bootstrap" method here is called the first time the runtime
 * encounters a particular "invokedynamic" call site, and it is responsible for setting up method
 * handles that may be used to invoke code. To learn more about this entire sequence, read up on the
 * "jdk.dynalink" package.
 *
 * <p>We will never go down this entire code path on Android because we do not support bytecode
 * generation there.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
public class Bootstrapper {
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /**
     * This is the method handle that's wired in to the bytecode for every dynamic call site in the
     * bytecode.
     */
    public static final ClassFileWriter.MHandle BOOTSTRAP_HANDLE =
            new ClassFileWriter.MHandle(
                    ByteCode.MH_INVOKESTATIC,
                    "org.mozilla.javascript.optimizer.Bootstrapper",
                    "bootstrap",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;"
                            + "Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");

    private static final DynamicLinker linker;

    static {
        // Set up the linkers that will map each call site to a method handle.
        DynamicLinkerFactory factory = new DynamicLinkerFactory();
        // Set up a linker that will delegate to other linkers based on the class
        // of the first argument to each dynamic invocation. (That's why the method
        // signatures in "Signatures" sometimes have different orders than their
        // counterparts in "ScriptRuntime".)
        // The linker caches the results so that it can efficiently only delegate to
        // compatible linkers. It will still go in order, so we put the linkers
        // likely to have the biggest impact on performance at the top of the list.
        CompositeTypeBasedGuardingDynamicLinker typeLinker =
                new CompositeTypeBasedGuardingDynamicLinker(
                        Arrays.asList(
                                new ConstAwareLinker(),
                                new BooleanLinker(),
                                new IntegerLinker(),
                                new DoubleLinker(),
                                new StringLinker(),
                                new ConsStringLinker(),
                                new NativeArrayLinker(),
                                new BaseFunctionLinker()));
        // Add the default linker, which can link anything no matter what.
        factory.setPrioritizedLinkers(typeLinker, new DefaultLinker());
        linker = factory.createLinker();
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final WeakHashMap<CallSiteDescriptor, WeakReference<CallSiteDescriptor>>
            CALLSITE_DESC_CACHE = new WeakHashMap<>();

    /**
     * This is the method called by every call site in the bytecode to map it to a method handle.
     */
    @SuppressWarnings("unused")
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType mType)
            throws NoSuchMethodException {
        Operation op = parseOperation(name);

        // ChainedCallSite lets a call site have a few options for complex situations.
        // It caches up to eight invocations, so that we can quickly select the best
        // implementation in situations where the same call site is invoked in different
        // contexts.
        return linker.link(
                new ChainedCallSite(dedupDesc(new CallSiteDescriptor(LOOKUP, op, mType))));
    }

    private static CallSiteDescriptor dedupDesc(CallSiteDescriptor desc) {
        synchronized (CALLSITE_DESC_CACHE) {
            return CALLSITE_DESC_CACHE.computeIfAbsent(desc, k -> new WeakReference<>(desc)).get();
        }
    }

    private static NamespaceOperation GET_PROOPERTY =
            StandardOperation.GET.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETNOWARN_PROPERTY =
            RhinoOperation.GETNOWARN.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETSUPER_PROPERTY =
            RhinoOperation.GETSUPER.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETWITHTHIS_PROPERTY =
            RhinoOperation.GETWITHTHIS.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETWITHTHISOPTIONAL_PROPERTY =
            RhinoOperation.GETWITHTHISOPTIONAL.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETELEMENT_PROPERTY =
            RhinoOperation.GETELEMENT.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETELEMENTSUPER_PROPERTY =
            RhinoOperation.GETELEMENTSUPER.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation GETINDEX_PROPERTY =
            RhinoOperation.GETINDEX.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation SET_PROPERTY =
            StandardOperation.SET.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation SETSUPER_PROPERTY =
            RhinoOperation.SETSUPER.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation SETELEMENT_PROPERTY =
            RhinoOperation.SETELEMENT.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation SETELEMENTSUPER_PROPERTY =
            RhinoOperation.SETELEMENTSUPER.withNamespace(StandardNamespace.PROPERTY);
    private static NamespaceOperation SETINDEX_PROPERTY =
            RhinoOperation.SETINDEX.withNamespace(StandardNamespace.PROPERTY);

    private static NamespaceOperation BIND_NAME =
            RhinoOperation.BIND.withNamespace(RhinoNamespace.NAME);
    private static NamespaceOperation GET_NAME =
            StandardOperation.GET.withNamespace(RhinoNamespace.NAME);
    private static NamespaceOperation GETWITHTHIS_NAME =
            RhinoOperation.GETWITHTHIS.withNamespace(RhinoNamespace.NAME);
    private static NamespaceOperation GETWITHTHISOPTIONAL_NAME =
            RhinoOperation.GETWITHTHISOPTIONAL.withNamespace(RhinoNamespace.NAME);
    private static NamespaceOperation SET_NAME =
            StandardOperation.SET.withNamespace(RhinoNamespace.NAME);
    private static NamespaceOperation SETSTRICT_NAME =
            RhinoOperation.SETSTRICT.withNamespace(RhinoNamespace.NAME);
    private static NamespaceOperation SETCONST_NAME =
            RhinoOperation.SETCONST.withNamespace(RhinoNamespace.NAME);

    private static NamespaceOperation ADD_MATH =
            RhinoOperation.ADD.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation TOBOOLEAN_MATH =
            RhinoOperation.TOBOOLEAN.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation TOINT32_MATH =
            RhinoOperation.TOINT32.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation TOUINT32_MATH =
            RhinoOperation.TOUINT32.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation EQ_MATH =
            RhinoOperation.EQ.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation SHALLOWEQ_MATH =
            RhinoOperation.SHALLOWEQ.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation TONUMBER_MATH =
            RhinoOperation.TONUMBER.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation TONUMERIC_MATH =
            RhinoOperation.TONUMERIC.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation COMPAREGT_MATH =
            RhinoOperation.COMPARE_GT.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation COMPARELT_MATH =
            RhinoOperation.COMPARE_LT.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation COMPAREGE_MATH =
            RhinoOperation.COMPARE_GE.withNamespace(RhinoNamespace.MATH);
    private static NamespaceOperation COMPARELE_MATH =
            RhinoOperation.COMPARE_LE.withNamespace(RhinoNamespace.MATH);

    /**
     * Operation names in the bytecode are names like "PROP:GET:[NAME]" and "NAME:BIND:[NAME]". (See
     * the "Signatures" interface for a description of these.) This method translates them the first
     * time a call site is seen to an object that can be easily consumed by the various types of
     * linkers.
     */
    private static Operation parseOperation(String name) throws NoSuchMethodException {
        String[] tokens = SEPARATOR.split(name, -1);
        String namespaceName = getNameSegment(tokens, name, 0);
        String opName = getNameSegment(tokens, name, 1);

        if ("PROP".equals(namespaceName)) {
            switch (opName) {
                case "GET":
                    // Get an object property with a constant name
                    return GET_PROOPERTY.named(getNameSegment(tokens, name, 2));
                case "GETNOWARN":
                    // Same with no warning of strict mode
                    return GETNOWARN_PROPERTY.named(getNameSegment(tokens, name, 2));
                case "GETSUPER":
                    // Get an object property from super with a constant name
                    return GETSUPER_PROPERTY.named(getNameSegment(tokens, name, 2));
                case "GETWITHTHIS":
                    // Same but also return "this" so that it is found by "lastStoredScriptable"
                    return GETWITHTHIS_PROPERTY.named(getNameSegment(tokens, name, 2));
                case "GETWITHTHISOPTIONAL":
                    // Similar to the above, but won't complain if prop is not found
                    return GETWITHTHISOPTIONAL_PROPERTY.named(getNameSegment(tokens, name, 2));
                case "GETELEMENT":
                    // Get the value of an element from a property that is on the stack,\
                    // as if using "[]" notation. Could be a String, number, or Symbol
                    return GETELEMENT_PROPERTY;
                case "GETELEMENTSUPER":
                    // Get the value of an element from a property that is on the stack,\
                    // as if using "[]" notation. Could be a String, number, or Symbol
                    return GETELEMENTSUPER_PROPERTY;
                case "GETINDEX":
                    // Same but the value is definitely a numeric index
                    return GETINDEX_PROPERTY;
                case "SET":
                    // Set an object property with a constant name
                    return SET_PROPERTY.named(getNameSegment(tokens, name, 2));
                case "SETSUPER":
                    // Set an object property in super with a constant name
                    return SETSUPER_PROPERTY.named(getNameSegment(tokens, name, 2));
                case "SETELEMENT":
                    // Set an object property as if by "[]", with a property on the stack
                    return SETELEMENT_PROPERTY;
                case "SETELEMENTSUPER":
                    // Set an object property in super as if by "[]", with a property on the stack
                    return SETELEMENTSUPER_PROPERTY;
                case "SETINDEX":
                    // Same but the property name is definitely a number
                    return SETINDEX_PROPERTY;
            }
        } else if ("NAME".equals(namespaceName)) {
            switch (opName) {
                case "BIND":
                    // Bind a new variable to the context with a constant name
                    return BIND_NAME.named(getNameSegment(tokens, name, 2));
                case "GET":
                    // Get a variable from the context with a constant name
                    return GET_NAME.named(getNameSegment(tokens, name, 2));
                case "GETWITHTHIS":
                    // Same but also return "this" so that it is found by "lastStoredScriptable"
                    return GETWITHTHIS_NAME.named(getNameSegment(tokens, name, 2));
                case "GETWITHTHISOPTIONAL":
                    // Similar to the above, but won't complain if prop is not found
                    return GETWITHTHISOPTIONAL_NAME.named(getNameSegment(tokens, name, 2));
                case "SET":
                    // Set an object in the context with a constant name
                    return SET_NAME.named(getNameSegment(tokens, name, 2));
                case "SETSTRICT":
                    // Same but implement strict mode checks
                    return SETSTRICT_NAME.named(getNameSegment(tokens, name, 2));
                case "SETCONST":
                    // Same but try to set a constant
                    return SETCONST_NAME.named(getNameSegment(tokens, name, 2));
            }

        } else if ("MATH".equals(namespaceName)) {
            switch (opName) {
                case "ADD":
                    return ADD_MATH;
                case "TOBOOLEAN":
                    return TOBOOLEAN_MATH;
                case "TOINT32":
                    return TOINT32_MATH;
                case "TOUINT32":
                    return TOUINT32_MATH;
                case "EQ":
                    return EQ_MATH;
                case "SHALLOWEQ":
                    return SHALLOWEQ_MATH;
                case "TONUMBER":
                    return TONUMBER_MATH;
                case "TONUMERIC":
                    return TONUMERIC_MATH;
                case "COMPAREGT":
                    return COMPAREGT_MATH;
                case "COMPARELT":
                    return COMPARELT_MATH;
                case "COMPAREGE":
                    return COMPAREGE_MATH;
                case "COMPARELE":
                    return COMPARELE_MATH;
            }
        }

        // Fall through to no match. This will only happen if the name in the bytecode
        // does not match the pattern that this method understands, which means that
        // there is a mismatch between the bytecode and the runtime.
        throw new NoSuchMethodException(name);
    }

    // Given a list of name segments and a position, return the interned name at the
    // specified position. This allows us, to pull a name like "foo" from an operation
    // named, for example, "NAME:GET:foo".
    private static String getNameSegment(String[] segments, String name, int pos) {
        if (pos >= segments.length) {
            return "";
        }
        // The "slot maps" in ScriptableObject-based classes can shortcut when property names
        // are "==", so interning strings improves performance in a measurable way, because
        // the property names that we pull from the INDY operation descriptors are essentially
        // constants.
        return segments[pos].intern();
    }
}
