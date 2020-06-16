/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import org.mozilla.javascript.TopLevel.NativeErrors;

public class NativePromise
    extends ScriptableObject {

  enum State {PENDING, FULFILLED, REJECTED}

  enum ReactionType {FULFILL, REJECT}

  private boolean isHandled = false;
  private State state = State.PENDING;
  private Object result = null;

  private ArrayList<Reaction> fulfillReactions = new ArrayList<>();
  private ArrayList<Reaction> rejectReactions = new ArrayList<>();

  public static void init(Context cx, Scriptable scope, boolean sealed) {
    LambdaConstructor constructor =
        new LambdaConstructor(scope, "Promise", 1,
            LambdaConstructor.CONSTRUCTOR_NEW,
            NativePromise::constructor
        );

    constructor.defineConstructorMethod(scope, "resolve", 1, NativePromise::resolve);
    constructor.defineConstructorMethod(scope, "reject", 1, NativePromise::reject);
    constructor.defineConstructorMethod(scope, "all", 1, NativePromise::all);
    constructor.defineConstructorMethod(scope, "race", 1, NativePromise::race);

    constructor.definePrototypeMethod(scope, "then", 2,
        (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
          NativePromise self = LambdaConstructor.convertThisObject(thisObj, NativePromise.class);
          return self.then(lcx, lscope, args);
        });
    constructor.definePrototypeMethod(scope, "catch", 1, NativePromise::doCatch);

    constructor.definePrototypeProperty(SymbolKey.TO_STRING_TAG, "Promise", DONTENUM | READONLY);

    ScriptableObject.defineProperty(scope, "Promise", constructor, DONTENUM);

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
    } catch (RhinoException re) {
      resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{getErrorObject(cx, scope, re)});
    }

    return promise;
  }

  @Override
  public String getClassName() {
    return "Promise";
  }

  // Promise resolve
  private static Object resolve(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (!ScriptRuntime.isObject(thisObj)) {
      throw ScriptRuntime.typeError0("msg.this.not.object");
    }
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);
    if (arg instanceof NativePromise) {
      Object argConstructor = ScriptRuntime.getObjectProp(arg, "constructor", cx, scope);
      if (argConstructor == thisObj) {
        return arg;
      }
    }
    Capability cap = new Capability(cx, scope, thisObj);
    cap.resolve.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{arg});
    return cap.promise;
  }

  // Promise.reject
  private static Object reject(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (!ScriptRuntime.isObject(thisObj)) {
      throw ScriptRuntime.typeError0("msg.this.not.object");
    }
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);
    Capability cap = new Capability(cx, scope, thisObj);
    cap.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{arg});
    return cap.promise;
  }

  // Promise.all
  private static Object all(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    Capability cap = new Capability(cx, scope, thisObj);
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);

    IteratorLikeIterable iterable;
    try {
      Object maybeIterable = ScriptRuntime.callIterator(arg, cx, scope);
      iterable = new IteratorLikeIterable(cx, scope, maybeIterable);
    } catch (RhinoException re) {
      cap.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{getErrorObject(cx, scope, re)});
      return cap.promise;
    }

    IteratorLikeIterable.Itr iterator = iterable.iterator();
    try {
      PromiseAllResolver resolver = new PromiseAllResolver(iterator, thisObj, cap);
      try {
        return resolver.resolve(cx, scope);
      } finally {
        if (!iterator.isDone()) {
          iterable.close();
        }
      }
    } catch (RhinoException re) {
      cap.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{getErrorObject(cx, scope, re)});
      return cap.promise;
    }
  }

  // Promise.race
  private static Object race(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    Capability cap = new Capability(cx, scope, thisObj);
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);

    IteratorLikeIterable iterable;
    try {
      Object maybeIterable = ScriptRuntime.callIterator(arg, cx, scope);
      iterable = new IteratorLikeIterable(cx, scope, maybeIterable);
    } catch (RhinoException re) {
      cap.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{getErrorObject(cx, scope, re)});
      return cap.promise;
    }

    IteratorLikeIterable.Itr iterator = iterable.iterator();
    try {
      try {
        return performRace(cx, scope, iterator, thisObj, cap);
      } finally {
        if (!iterator.isDone()) {
          iterable.close();
        }
      }
    } catch (RhinoException re) {
      cap.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{getErrorObject(cx, scope, re)});
      return cap.promise;
    }
  }

  private static Object performRace(Context cx, Scriptable scope,
      IteratorLikeIterable.Itr iterator, Scriptable thisObj, Capability cap) {
    // Manually iterate for exception handling purposes
    while (true) {
      boolean hasNext;
      Object nextVal = Undefined.instance;
      boolean nextOk = false;
      try {
        hasNext = iterator.hasNext();
        if (hasNext) {
          nextVal = iterator.next();
        }
        nextOk = true;
      } finally {
        if (!nextOk) {
          iterator.setDone(true);
        }
      }

      if (!hasNext) {
        return cap.promise;
      }

      // Call "resolve" to get the next promise in the chain
      Callable resolve = ScriptRuntime.getPropFunctionAndThis(thisObj, "resolve",
          cx, scope);
      Object nextPromise = resolve
          .call(cx, scope, ScriptRuntime.lastStoredScriptable(cx),
              new Object[]{nextVal});

      // And then call "then" on it.
      // Logic in the resolution function ensures we don't deliver duplicate results
      Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(nextPromise, "then",
          cx, scope);
      thenFunc.call(cx, scope, ScriptRuntime.lastStoredScriptable(cx),
          new Object[]{cap.resolve, cap.reject});
    }
  }

  // Promise.prototype.then
  private Object then(Context cx, Scriptable scope, Object[] args) {
    Object ourNativeConstructor = ScriptableObject.getProperty(this, "constructor");
    Capability capability = new Capability(cx, scope, ourNativeConstructor);

    Callable onFulfilled = null;
    if (args.length >= 1 && args[0] instanceof Callable) {
      onFulfilled = (Callable) args[0];
    }
    Callable onRejected = null;
    if (args.length >= 2 && args[1] instanceof Callable) {
      onRejected = (Callable) args[1];
    }

    Reaction fulfillReaction = new Reaction(capability, ReactionType.FULFILL, onFulfilled);
    Reaction rejectReaction = new Reaction(capability, ReactionType.REJECT, onRejected);

    if (state == State.PENDING) {
      fulfillReactions.add(fulfillReaction);
      rejectReactions.add(rejectReaction);
    } else if (state == State.FULFILLED) {
      cx.enqueueMicrotask(() -> {
        fulfillReaction.invoke(cx, scope, result);
      });
    } else {
      assert (state == State.REJECTED);
      cx.enqueueMicrotask(() -> {
        rejectReaction.invoke(cx, scope, result);
      });
    }

    isHandled = true;
    return capability.promise;
  }

  // Promise.prototype.catch
  private static Object doCatch(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    Object arg = (args.length > 0 ? args[0] : Undefined.instance);
    // No guarantee that the caller didn't change the prototype of "then"!
    Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(thisObj, "then",
        cx, scope);
    return thenFunc.call(cx, scope, ScriptRuntime.lastStoredScriptable(cx),
        new Object[]{Undefined.instance, arg});
  }

  // Abstract operation to fulfill a promise
  private Object fulfillPromise(Context cx, Scriptable scope, Object value) {
    assert (state == State.PENDING);
    result = value;
    ArrayList<Reaction> reactions = fulfillReactions;
    fulfillReactions = new ArrayList<>();
    if (!rejectReactions.isEmpty()) {
      rejectReactions = new ArrayList<>();
    }
    state = State.FULFILLED;
    for (Reaction r : reactions) {
      cx.enqueueMicrotask(() -> {
        r.invoke(cx, scope, value);
      });
    }
    return Undefined.instance;
  }

  // Abstract operation to reject a promise.
  private Object rejectPromise(Context cx, Scriptable scope, Object reason) {
    assert (state == State.PENDING);
    result = reason;
    ArrayList<Reaction> reactions = rejectReactions;
    rejectReactions = new ArrayList<>();
    if (!fulfillReactions.isEmpty()) {
      fulfillReactions = new ArrayList<>();
    }
    state = State.REJECTED;
    for (Reaction r : reactions) {
      cx.enqueueMicrotask(() -> {
        r.invoke(cx, scope, reason);
      });
    }
    return Undefined.instance;
  }

  // Promise Resolve Thenable Job.
  // This gets called by the "resolving func" as a microtask.
  private Object callThenable(Context cx, Scriptable scope,
      Object resolution, Callable thenFunc) {
    ResolvingFunctions resolving = new ResolvingFunctions(cx, scope, this);
    Scriptable thisObj = (resolution instanceof Scriptable ? (Scriptable) resolution :
        Undefined.SCRIPTABLE_UNDEFINED);
    try {
      thenFunc.call(cx, scope, thisObj, new Object[]{resolving.resolve, resolving.reject});
    } catch (RhinoException re) {
      resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
          new Object[]{getErrorObject(cx, scope, re)});
    }
    return Undefined.instance;
  }

  private static Object getErrorObject(Context cx, Scriptable scope, RhinoException re) {
    if (re instanceof JavaScriptException) {
      return ((JavaScriptException) re).getValue();
    }

    TopLevel.NativeErrors constructor = NativeErrors.Error;
    if (re instanceof EcmaError) {
      EcmaError ee = (EcmaError) re;
      switch (ee.getName()) {
        case "TypeError":
          constructor = NativeErrors.TypeError;
          break;
        default:
          break;
      }
    }
    return ScriptRuntime.newNativeError(cx, scope, constructor, new Object[]{re.getMessage()});
  }

  // Output of "CreateResolvingFunctions." Carries with it an "alreadyResolved" state,
  // so we make it a separate object. This actually fires resolution functions on
  // the passed callbacks.
  private static class ResolvingFunctions {

    private boolean alreadyResolved = false;
    Callable resolve;
    Callable reject;

    ResolvingFunctions(Context topCx, Scriptable topScope, NativePromise promise) {
      resolve = new LambdaFunction(topScope, 1,
          (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) ->
              resolve(cx, scope, promise, (args.length > 0 ? args[0] : Undefined.instance))
      );
      reject = new LambdaFunction(topScope, 1,
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
        Object err = ScriptRuntime.newNativeError(cx, scope,
            NativeErrors.TypeError, new Object[]{"No promise self-resolution"});
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
        promise.callThenable(cx, scope, resolution, (Callable) thenObj);
      });
      return Undefined.instance;
    }
  }

  // "Promise Reaction" record. This is an input to the microtask.
  private static class Reaction {

    Capability capability = null;
    ReactionType reaction = ReactionType.REJECT;
    Callable handler = null;

    Reaction(Capability cap, ReactionType type, Callable handler) {
      this.capability = cap;
      this.reaction = type;
      this.handler = handler;
    }

    void invoke(Context cx, Scriptable scope, Object arg) {
      try {
        Object result = null;
        if (handler == null) {
          switch (reaction) {
            case FULFILL:
              result = arg;
              break;
            case REJECT:
              capability.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
                  new Object[]{arg});
              return;
          }
        } else {
          result = handler.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
              new Object[]{arg});
        }

        capability.resolve.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
            new Object[]{result});

      } catch (RhinoException re) {
        capability.reject.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED,
            new Object[]{getErrorObject(cx, scope, re)});
      }
    }
  }

  // "Promise Capability Record"
  // This abstracts a promise from the specific native implementation by keeping track
  // of the "resolve" and "reject" functions.
  private static class Capability {

    Object promise;
    private Object rawResolve = Undefined.instance;
    Callable resolve;
    private Object rawReject = Undefined.instance;
    Callable reject;

    // Given an object that represents a constructor function, execute it as if it
    // meets the "Promise" constructor pattern, which takes a function that will
    // be called with "resolve" and "reject" functions.
    Capability(Context topCx, Scriptable topScope, Object pc) {
      if (!(pc instanceof Constructable)) {
        throw ScriptRuntime.typeError0("msg.constructor.expected");
      }
      Constructable promiseConstructor = (Constructable) pc;
      LambdaFunction executorFunc = new LambdaFunction(topScope, 2,
          (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) ->
              executor(args));

      promise = promiseConstructor.construct(topCx, topScope, new Object[]{executorFunc});

      if (!(rawResolve instanceof Callable)) {
        throw ScriptRuntime.typeError0("msg.function.expected");
      }
      resolve = (Callable) rawResolve;

      if (!(rawReject instanceof Callable)) {
        throw ScriptRuntime.typeError0("msg.function.expected");
      }
      reject = (Callable) rawReject;
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

  // This object keeps track of the state necessary to execute Promise.all
  private static class PromiseAllResolver {

    final ArrayList<Object> values = new ArrayList<>();
    int remainingElements = 1;

    IteratorLikeIterable.Itr iterator;
    Scriptable thisObj;
    Capability capability;

    PromiseAllResolver(
        IteratorLikeIterable.Itr iter, Scriptable thisObj, Capability cap) {
      this.iterator = iter;
      this.thisObj = thisObj;
      this.capability = cap;
    }

    Object resolve(Context topCx, Scriptable topScope) {
      int index = 0;

      // Iterate manually because we need to catch exceptions in a special way.
      while (true) {
        boolean hasNext;
        Object nextVal = Undefined.instance;
        boolean nextOk = false;
        try {
          hasNext = iterator.hasNext();
          if (hasNext) {
            nextVal = iterator.next();
          }
          nextOk = true;
        } finally {
          if (!nextOk) {
            iterator.setDone(true);
          }
        }

        if (!hasNext) {
          if (--remainingElements == 0) {
            finalResolution(topCx, topScope);
          }
          return capability.promise;
        }

        values.add(Undefined.instance);

        // Call "resolve" to get the next promise in the chain
        Callable resolve = ScriptRuntime.getPropFunctionAndThis(thisObj, "resolve",
            topCx, topScope);
        Object nextPromise = resolve
            .call(topCx, topScope, ScriptRuntime.lastStoredScriptable(topCx),
                new Object[]{nextVal});

        // Create a resolution func that will stash its result in the right place
        PromiseElementResolver eltResolver = new PromiseElementResolver(index);
        LambdaFunction resolveFunc = new LambdaFunction(topScope, 1,
            (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) ->
                eltResolver.resolve(cx, scope, (args.length > 0 ? args[0] : Undefined.instance),
                    this)
        );
        remainingElements++;

        // Call "then" on the promise with the resolution func
        Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(nextPromise, "then",
            topCx, topScope);
        thenFunc.call(topCx, topScope, ScriptRuntime.lastStoredScriptable(topCx),
            new Object[]{resolveFunc, capability.reject});
        index++;
      }
    }

    void finalResolution(Context cx, Scriptable scope) {
      Scriptable newArray = cx.newArray(scope, values.toArray());
      capability.resolve.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{newArray});
    }
  }

  // This object keeps track of the state necessary to resolve one element in Promise.all
  private static class PromiseElementResolver {

    private boolean alreadyCalled = false;
    private final int index;

    PromiseElementResolver(int ix) {
      this.index = ix;
    }

    Object resolve(Context cx, Scriptable scope, Object result,
        PromiseAllResolver resolver) {
      if (alreadyCalled) {
        return Undefined.instance;
      }
      alreadyCalled = true;
      resolver.values.set(index, result);
      if (--resolver.remainingElements == 0) {
        resolver.finalResolution(cx, scope);
      }
      return Undefined.instance;
    }
  }
}
