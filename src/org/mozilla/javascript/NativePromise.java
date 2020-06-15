/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;

public class NativePromise
    extends ScriptableObject {

  enum State {PENDING, FULFILLED, REJECTED};
  enum ReactionType {FULFILL, REJECT};

  private boolean isHandled = false;
  private State state = State.PENDING;
  private Object result = null;

  private ArrayList<Reaction> fulfillReactions = new ArrayList<>();
  private ArrayList<Reaction> rejectReactions = new ArrayList<>();

  public static void init(Context cx, Scriptable scope, boolean sealed) {
    LambdaConstructor constructor =
        new LambdaConstructor(scope, "Promise", 1,
            NativePromise::constructor
        );

    constructor.defineConstructorMethod(scope, "resolve", 1, NativePromise::resolve);
    constructor.defineConstructorMethod(scope, "reject", 1, NativePromise::reject);

    constructor.definePrototypeMethod(scope, "then", 1,
        (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
          NativePromise self = LambdaConstructor.convertThisObject(thisObj,  NativePromise.class);
          return self.then(lcx, lscope, args);
        });

    ScriptableObject.defineProperty(scope, "Promise", constructor, PERMANENT);

    constructor.optimizeStorage();
    if (sealed) {
      constructor.sealObject();
    }
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    if (args.length < 1) {
      throw ScriptRuntime.typeError0("msg.function.expected");
    }
    if (!(args[0] instanceof Callable)) {
      throw ScriptRuntime.typeError0("msg.function.expected");
    }
    Callable executor = (Callable) args[0];

    NativePromise promise = new NativePromise();

    ResolvingFunctions resolving = new ResolvingFunctions(cx, scope, promise);

    try {
      executor.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{resolving.resolve, resolving.reject});
    } catch (JavaScriptException je) {
      resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{je.getValue()});
    } catch (RhinoException re) {
      resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{re});
    }

    return promise;
  }

  @Override
  public String getClassName() {
    return "Promise";
  }

  private static Object resolve(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);
    if (arg instanceof NativePromise) {
      // If it's a NativePromise, then its constructor is our constructor
      return arg;
    }
    Capability cap = new Capability(cx, scope, thisObj);
    cap.resolve.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] { arg });
    return cap.promise;
  }

  private static Object reject(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);
    Capability cap = new Capability(cx, scope, thisObj);
    cap.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] { arg });
    return cap.promise;
  }

  private Object then(Context cx, Scriptable scope, Object[] args) {
    Object ourNativeConstructor = ScriptableObject.getProperty(this, "constructor");
    Capability capability = new Capability(cx, scope, ourNativeConstructor);

    Callable onFulfilled = null;
    if (args.length >= 1 && args[0] instanceof Callable) {
      onFulfilled = (Callable)args[0];
    }
    Callable onRejected = null;
    if (args.length >= 2 && args[1] instanceof Callable) {
      onRejected = (Callable)args[1];
    }

    if (state == State.PENDING) {
      if (onFulfilled != null) {
        fulfillReactions.add(new Reaction(capability, ReactionType.FULFILL, onFulfilled));
        rejectReactions.add(new Reaction(capability, ReactionType.REJECT, onRejected));
      }
    } else if (state == State.FULFILLED) {
      final Callable localFulfilled = onFulfilled;
      cx.enqueueMicrotask(() -> {
        callReaction(cx, scope, new Reaction(capability, ReactionType.FULFILL, localFulfilled),
            result);
    });
    } else {
      assert(state == State.REJECTED);
      final Callable localRejected = onRejected;
      cx.enqueueMicrotask(() -> {
        callReaction(cx, scope, new Reaction(capability, ReactionType.REJECT, localRejected),
            result);
      });
    }

    isHandled = true;
    return capability.promise;
  }

  private Object fulfillPromise(Context cx, Scriptable scope, Object value) {
    assert (state == State.PENDING);
    result = value;
    ArrayList<Reaction> reactions = fulfillReactions;
    fulfillReactions = new ArrayList<>();
    if (!rejectReactions.isEmpty()) {
      rejectReactions = new ArrayList<>();
    }
    state = State.FULFILLED;
    for (Reaction r: reactions) {
      cx.enqueueMicrotask(() -> {
        callReaction(cx, scope, r, value);
      });
    }
    return Undefined.instance;
  }

  private Object rejectPromise(Context cx, Scriptable scope, Object reason) {
    assert (state == State.PENDING);
    result = reason;
    ArrayList<Reaction> reactions = rejectReactions;
    rejectReactions = new ArrayList<>();
    if (!fulfillReactions.isEmpty()) {
      fulfillReactions = new ArrayList<>();
    }
    state = State.REJECTED;
    for (Reaction r: reactions) {
      cx.enqueueMicrotask(() -> {
        callReaction(cx, scope, r, reason);
      });
    }
    return Undefined.instance;
  }

  // Promise Reaction Job
  // This gets called when fulfilling and rejecting promises
  private void callReaction(Context cx, Scriptable scope, Reaction reaction, Object arg) {
    Object result;
    if (reaction.handler == null) {
      result = arg;
    } else {
      result = reaction.handler.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[] { arg });
    }
    if (reaction.capability != null) {
      if (result instanceof JavaScriptException) {
        reaction.capability.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
            new Object[]{((JavaScriptException) result).getValue()});
      } else if (result instanceof RhinoException) {
          reaction.capability.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
              new Object[] { result });
      } else {
        reaction.capability.resolve.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
            new Object[] { result });
      }
    }
  }

  // Promise Resolve Thenable Job.
  // This gets called by the "resolving func" as a microtask.
  private Object callThenable(Context cx, Scriptable scope,
      Object resolution, Callable thenFunc) {
    ResolvingFunctions resolving = new ResolvingFunctions(cx, scope, this);
    Scriptable thisObj = (resolution instanceof Scriptable ? (Scriptable)resolution :
      Undefined.SCRIPTABLE_UNDEFINED);
    Object result;
    try {
      result = thenFunc.call(cx, scope, thisObj, new Object[]{resolving.resolve, resolving.reject});
    } catch (JavaScriptException e) {
      result = resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{e.getValue()});
    } catch (RhinoException re) {
      result = resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{re});
    }
    // This is shown as a "Completion" in the spec
    return result;
  }

  // Output of "CreateResolvingFunctions." Carries with it an "alreadyResolved" state,
  // so we make it a separate object
  private static class ResolvingFunctions {
    private boolean alreadyResolved = false;
    Callable resolve;
    Callable reject;

    ResolvingFunctions(Context topCx, Scriptable topScope, NativePromise promise) {
      resolve = new LambdaFunction(topScope, "stepsResolve", 1,
          (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) ->
            resolve(cx, scope, promise, (args.length > 0 ? args[0] : Undefined.instance))
      );
      reject = new LambdaFunction(topScope, "stepsReject", 1,
          (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) ->
              reject(cx, scope, promise, (args.length > 0 ? args[0] : Undefined.instance))
      );
    }

    private Object reject(Context cx, Scriptable scope, NativePromise promise,
        Object reason) {
      if (alreadyResolved) {
        return Undefined.instance;
      }
      alreadyResolved = true;
      return promise.rejectPromise(cx, scope, reason);
    }

    private Object resolve(Context cx, Scriptable scope, NativePromise promise,
        Object resolution) {
      if (alreadyResolved) {
        return Undefined.instance;
      }
      alreadyResolved = true;

      if (resolution == promise) {
        Object err = ScriptRuntime.typeError0("msg.promise.self.resolution");
        return promise.rejectPromise(cx, scope, err);
      }

      if (!ScriptRuntime.isObject(resolution)) {
        return promise.fulfillPromise(cx, scope, resolution);
      }

      Scriptable sresolution = ScriptableObject.ensureScriptable(resolution);
      Object thenObj = ScriptableObject.getProperty(sresolution, "then");
      if (!(thenObj instanceof Callable)) {
        return promise.fulfillPromise(cx, scope, resolution);
      }

      cx.enqueueMicrotask(() -> {
        promise.callThenable(cx, scope, resolution, (Callable)thenObj);
      });
      return Undefined.instance;
    }
  }

  // "Promise Reaction" record
  private static class Reaction {
    Capability capability = null;
    ReactionType reaction = ReactionType.REJECT;
    Callable handler = null;

    Reaction(Capability cap, ReactionType type, Callable handler) {
      this.capability = cap;
      this.reaction = type;
      this.handler = handler;
    }
  }

  // "Promise Capability Record".
  private static class Capability {
    Object promise = Undefined.instance;
    private Object rawResolve = Undefined.instance;
    Callable resolve = null;
    private Object rawReject = Undefined.instance;
    Callable reject = null;

    // Given an object that represents a constructor function, execute it as if it
    // meets the "Promise" constructor pattern, which takes a function that will
    // be called with "resolve" and "reject" functions.
    Capability(Context cx, Scriptable scope, Object pc) {
      if (!(pc instanceof Constructable)) {
        throw ScriptRuntime.typeError0("msg.constructor.expected");
      }
      Constructable promiseConstructor = (Constructable)pc;
      LambdaFunction executorFunc = new LambdaFunction(scope, "", 2,
          (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
            executor(args));

      promise = promiseConstructor.construct(cx, scope, new Object[] { executorFunc });

      if (!(rawResolve instanceof Callable)) {
        throw ScriptRuntime.typeError0("msg.function.expected");
      }
      resolve = (Callable)rawResolve;

      if (!(rawReject instanceof Callable)) {
        throw ScriptRuntime.typeError0("msg.function.expected");
      }
      reject = (Callable)rawReject;
    }

    private Object executor(Object[] args) {
      if (!Undefined.isUndefined(rawResolve) || !Undefined.isUndefined(rawReject)) {
        throw ScriptRuntime.typeError0("msg.promise.capability.state");
      }
      if (args.length > 0) {
        rawResolve = args[0];
      }
      if (args.length > 1) {
        rawReject = args[1];
      }
      return Undefined.instance;
    }
  }
}
