/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

 package org.mozilla.javascript.optimizer;

import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.dynalink.linker.support.CompositeTypeBasedGuardingDynamicLinker;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ScriptableObjectSlot;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

public class Linker
{
    // Cache transformations of standard call patterns to actual calls
    protected static final MethodHandle GETSCRIPTABLE =
            MethodHandles.dropArguments(InvokeDynamicSupport.GETSCRIPTABLEOBJPROP,
                    3, Scriptable.class);

    static final Operation CALL1 =
        new NamespaceOperation(StandardOperation.CALL, StandardNamespace.PROPERTY);
    static final Operation CALL2 =
            new NamespaceOperation(StandardOperation.CALL, StandardNamespace.PROPERTY);
    static final Operation CALLN =
            new NamespaceOperation(StandardOperation.CALL, StandardNamespace.PROPERTY);

    private static final Linker self = new Linker();

    private final DynamicLinker linker;

    public static DynamicLinker getLinker() {
        return self.linker;
    }

    private Linker()
    {
        DynamicLinkerFactory factory = new DynamicLinkerFactory();
        // Set up the linker. The list of type-specific linkers must be ordered from
        // most to least-specific or nothing will happen.
        factory.setPrioritizedLinker(
            new CompositeTypeBasedGuardingDynamicLinker(
                Arrays.asList(
                    new ScriptableObjectLinker(),
                    new ScriptableLinker(),
                    new ObjectLinker()
                )
            )
        );
        linker = factory.createLinker();
    }

    /**
     * This is the most basic linker. It just delegates permanently to ScriptRuntime
     * like the old, pre-dynalink runtime.
     */
    private static class ObjectLinker
            implements TypeBasedGuardingDynamicLinker
    {
        @Override
        public boolean canLinkType(Class<?> type) {
            return true;
        }

        @Override
        public GuardedInvocation getGuardedInvocation(
            LinkRequest req, LinkerServices services)
            throws Exception
        {
            final Operation op = req.getCallSiteDescriptor().getOperation();
            if (op == StandardOperation.GET) {
                return new GuardedInvocation(InvokeDynamicSupport.GETOBJPROP_FALLBACK);
            } else if (op == StandardOperation.CALL) {
                return new GuardedInvocation(InvokeDynamicSupport.CALLPROP0_FALLBACK);
            } else if (op == CALL1) {
                return new GuardedInvocation(InvokeDynamicSupport.CALLPROP1_FALLBACK);
            } else if (op == CALL2) {
                return new GuardedInvocation(InvokeDynamicSupport.CALLPROP2_FALLBACK);
            } else if (op == CALLN) {
                return new GuardedInvocation(InvokeDynamicSupport.CALLPROPN_FALLBACK);
            }
            return null;
        }
    }

    /**
     * This linker is ever-so-slightly optimized for Scriptable objects.
     */
    private static class ScriptableLinker
            extends ObjectLinker
            implements TypeBasedGuardingDynamicLinker
    {
        @Override
        public boolean canLinkType(Class<?> type) {
            return Scriptable.class.isAssignableFrom(type);
        }

        @Override
        public GuardedInvocation getGuardedInvocation(
                LinkRequest req, LinkerServices services)
                throws Exception
        {
            final Operation op = req.getCallSiteDescriptor().getOperation();
            if (op == StandardOperation.GET) {
                return new GuardedInvocation(GETSCRIPTABLE);
            }
            return super.getGuardedInvocation(req, services);
        }
    }

    /**
     * This is the linker for ScriptableObject and its subclasses. It'll cache
     * the Slot from each lookup so it can avoid the whole hash table mess.
     */
    private static final class ScriptableObjectLinker
            implements TypeBasedGuardingDynamicLinker
    {
        @Override
        public boolean canLinkType(Class<?> type) {
            return ScriptableObject.class.isAssignableFrom(type);
        }

        @Override
        public GuardedInvocation getGuardedInvocation(
                LinkRequest req, LinkerServices services)
                throws Exception
        {
            final Operation op = req.getCallSiteDescriptor().getOperation();
            final ScriptableObject so = (ScriptableObject)req.getReceiver();
            final Object[] args = req.getArguments();

            if (op == StandardOperation.GET) {
                if (req.isCallSiteUnstable()) {
                    // Fallback if caching just doesn't work for this CallSite
                    return new GuardedInvocation(GETSCRIPTABLE);
                }

                final ScriptableObjectSlot slot = so.getSlot((String)args[1]);
                if (slot == null) {
                    // Slot not found or not in a Slot, perhaps a subclass
                    return new GuardedInvocation(GETSCRIPTABLE);
                } else {
                    // Return a method handle that will check object identity
                    // and also object generation on every request
                    final int generation = so.getGeneration();
                    MethodHandle getValue =
                        MethodHandles.insertArguments(
                            InvokeDynamicSupport.GETSLOTVALUE, 0, slot);
                    MethodHandle checkGeneration =
                        MethodHandles.insertArguments(
                            InvokeDynamicSupport.CHECKOBJECTGENERATION, 0, generation, so);
                    return new GuardedInvocation(getValue, checkGeneration);
                }

            } else if (op == StandardOperation.CALL) {
                if (req.isCallSiteUnstable()) {
                    // Fallback if caching just doesn't work for this CallSite
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROP0_FALLBACK);
                }

                final ScriptableObjectSlot slot = so.getSlot((String)args[1]);
                if ((slot == null) || !(slot.getValue(so) instanceof Callable)) {
                    // Slot not found or not in a Slot, perhaps a subclass
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROP0_FALLBACK);
                } else {
                    final Callable f = (Callable)slot.getValue(so);
                    final int generation = so.getGeneration();

                    // Return a method handle that will check object identity
                    // and also object generation on every request

                    MethodHandle getValue =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.INVOKESLOT0, 0, slot);
                    MethodHandle checkGeneration =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.CHECKOBJECTGENERATION, 0, generation, so);
                    return new GuardedInvocation(getValue, checkGeneration);
                }

            } else if (op == CALL1) {
                if (req.isCallSiteUnstable()) {
                    // Fallback if caching just doesn't work for this CallSite
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROP1_FALLBACK);
                }

                final ScriptableObjectSlot slot = so.getSlot((String)args[1]);
                if (slot == null) {
                    // Slot not found or not in a Slot, perhaps a subclass
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROP1_FALLBACK);
                } else {
                    // Return a method handle that will check object identity
                    // and also object generation on every request
                    final int generation = so.getGeneration();
                    MethodHandle getValue =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.INVOKESLOT1, 0, slot);
                    MethodHandle checkGeneration =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.CHECKOBJECTGENERATION, 0, generation, so);
                    return new GuardedInvocation(getValue, checkGeneration);
                }

            } else if (op == CALL2) {
                if (req.isCallSiteUnstable()) {
                    // Fallback if caching just doesn't work for this CallSite
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROP2_FALLBACK);
                }

                final ScriptableObjectSlot slot = so.getSlot((String)args[1]);
                if (slot == null) {
                    // Slot not found or not in a Slot, perhaps a subclass
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROP2_FALLBACK);
                } else {
                    // Return a method handle that will check object identity
                    // and also object generation on every request
                    final int generation = so.getGeneration();
                    MethodHandle getValue =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.INVOKESLOT2, 0, slot);
                    MethodHandle checkGeneration =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.CHECKOBJECTGENERATION, 0, generation, so);
                    return new GuardedInvocation(getValue, checkGeneration);
                }

            } else if (op == CALLN) {
                if (req.isCallSiteUnstable()) {
                    // Fallback if caching just doesn't work for this CallSite
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROPN_FALLBACK);
                }

                final ScriptableObjectSlot slot = so.getSlot((String)args[1]);
                if (slot == null) {
                    // Slot not found or not in a Slot, perhaps a subclass
                    return new GuardedInvocation(InvokeDynamicSupport.CALLPROPN_FALLBACK);
                } else {
                    // Return a method handle that will check object identity
                    // and also object generation on every request
                    final int generation = so.getGeneration();
                    MethodHandle getValue =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.INVOKESLOTN, 0, slot);
                    MethodHandle checkGeneration =
                            MethodHandles.insertArguments(
                                    InvokeDynamicSupport.CHECKOBJECTGENERATION, 0, generation, so);
                    return new GuardedInvocation(getValue, checkGeneration);
                }
            }

            return null;
        }
    }
}
