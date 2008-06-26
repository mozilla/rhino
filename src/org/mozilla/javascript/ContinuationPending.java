package org.mozilla.javascript;

import org.mozilla.javascript.continuations.Continuation;

/**
 * Exception thrown by 
 * {@link org.mozilla.javascript.Context#executeScriptWithContinuations(Script, Scriptable)}
 * and {@link org.mozilla.javascript.Context#callFunctionWithContinuations(Callable, Scriptable, Object[])}
 * when execution encounters a continuation captured by
 * {@link org.mozilla.javascript.Context#captureContinuation()}.
 * Exception will contain the captured state needed to restart the continuation
 * with {@link org.mozilla.javascript.Context#resumeContinuation(ContinuationPending, Object)}.
 * @author Norris Boyd
 */
public class ContinuationPending extends RuntimeException {
    private Continuation continuationState;
    private Object applicationState;
    private Scriptable scope;
    
    /**
     * Construct a ContinuationPending exception. Internal call only;
     * users of the API should get continuations created on their behalf by
     * calling {@link org.mozilla.javascript.Context#executeScriptWithContinuations(Script, Scriptable)}
     * and {@link org.mozilla.javascript.Context#callFunctionWithContinuations(Callable, Scriptable, Object[])}
     * @param continuationState Internal Continuation object
     */
    ContinuationPending(Continuation continuationState) {
        this.continuationState = continuationState;
    }
    
    /**
     * @return internal continuation state
     */
    Continuation getContinuationState() {
        return continuationState;
    }
    
    /**
     * Store an arbitrary object that applications can use to associate
     * their state with the continuation.
     * Note that this application state must be serializable if the application
     * wishes to serialize the continuation.
     * @param applicationState arbitrary application state
     */
    public void setApplicationState(Object applicationState) {
        this.applicationState = applicationState;
    }

    /**
     * @return arbitrary application state
     */
    public Object getApplicationState() {
        return applicationState;
    }

    public Scriptable getScope() {
        return scope;
    }

    public void setScope(Scriptable scope) {
        this.scope = scope;
    }
}
