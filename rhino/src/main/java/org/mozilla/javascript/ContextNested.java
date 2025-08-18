package org.mozilla.javascript;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.mozilla.javascript.debug.Debugger;

public class ContextNested implements Context {
    private final Context delegate;

    protected ContextNested(Context delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isStrictMode() {
        return delegate.isStrictMode();
    }

    @Override
    public boolean isActivationNeeded(String name) {
        return delegate.isActivationNeeded(name);
    }

    @Override
    public boolean isGeneratingDebugChanged() {
        return delegate.isGeneratingDebugChanged();
    }

    @Override
    public void setApplicationClassLoader(ClassLoader loader) {
        delegate.setApplicationClassLoader(loader);
    }

    @Override
    public ClassLoader getApplicationClassLoader() {
        return delegate.getApplicationClassLoader();
    }

    @Override
    public void setInstructionObserverThreshold(int threshold) {
        delegate.setInstructionObserverThreshold(threshold);
    }

    @Override
    public int getInstructionObserverThreshold() {
        return delegate.getInstructionObserverThreshold();
    }

    @Override
    public void setDebugger(Debugger debugger, Object contextData) {
        delegate.setDebugger(debugger, contextData);
    }

    @Override
    public Object getDebuggerContextData() {
        return delegate.getDebuggerContextData();
    }

    @Override
    public Debugger getDebugger() {
        return delegate.getDebugger();
    }

    @Override
    public WrapFactory getWrapFactory() {
        return delegate.getWrapFactory();
    }

    @Override
    public void setWrapFactory(WrapFactory wrapFactory) {
        delegate.setWrapFactory(wrapFactory);
    }

    @Override
    public void removeThreadLocal(Object key) {
        delegate.removeThreadLocal(key);
    }

    @Override
    public void putThreadLocal(Object key, Object value) {
        delegate.putThreadLocal(key, value);
    }

    @Override
    public Object getThreadLocal(Object key) {
        return delegate.getThreadLocal(key);
    }

    @Override
    public ClassShutterSetter getClassShutterSetter() {
        return delegate.getClassShutterSetter();
    }

    @Override
    public void setClassShutter(ClassShutter shutter) {
        delegate.setClassShutter(shutter);
    }

    @Override
    public void setSecurityController(SecurityController controller) {
        delegate.setSecurityController(controller);
    }

    @Override
    public void setMaximumInterpreterStackDepth(int max) {
        delegate.setMaximumInterpreterStackDepth(max);
    }

    @Override
    public int getMaximumInterpreterStackDepth() {
        return delegate.getMaximumInterpreterStackDepth();
    }

    @Override
    public void setInterpretedMode(boolean interpretedMode) {
        delegate.setInterpretedMode(interpretedMode);
    }

    @Override
    public boolean isInterpretedMode() {
        return delegate.isInterpretedMode();
    }

    @Override
    public void setGeneratingSource(boolean generatingSource) {
        delegate.setGeneratingSource(generatingSource);
    }

    @Override
    public boolean isGeneratingSource() {
        return delegate.isGeneratingSource();
    }

    @Override
    public void setGeneratingDebug(boolean generatingDebug) {
        delegate.setGeneratingDebug(generatingDebug);
    }

    @Override
    public boolean isGeneratingDebug() {
        return delegate.isGeneratingDebug();
    }

    @Override
    public Object[] getElements(Scriptable object) {
        return delegate.getElements(object);
    }

    @Override
    public String decompileFunctionBody(Function fun, int indent) {
        return delegate.decompileFunctionBody(fun, indent);
    }

    @Override
    public String decompileFunction(Function fun, int indent) {
        return delegate.decompileFunction(fun, indent);
    }

    @Override
    public String decompileScript(Script script, int indent) {
        return delegate.decompileScript(script, indent);
    }

    @Override
    public Function compileFunction(
            Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
        return delegate.compileFunction(scope, source, sourceName, lineno, securityDomain);
    }

    @Override
    public Script compileString(
            String source, String sourceName, int lineno, Object securityDomain) {
        return delegate.compileString(source, sourceName, lineno, securityDomain);
    }

    @Override
    public Script compileReader(Reader in, String sourceName, int lineno, Object securityDomain)
            throws IOException {
        return delegate.compileReader(in, sourceName, lineno, securityDomain);
    }

    @Override
    public boolean stringIsCompilableUnit(String source) {
        return delegate.stringIsCompilableUnit(source);
    }

    @Override
    public Object evaluateReader(
            Scriptable scope, Reader in, String sourceName, int lineno, Object securityDomain)
            throws IOException {
        return delegate.evaluateReader(scope, in, sourceName, lineno, securityDomain);
    }

    @Override
    public Object evaluateString(
            Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
        return delegate.evaluateString(scope, source, sourceName, lineno, securityDomain);
    }

    @Override
    public Scriptable initSafeStandardObjects(ScriptableObject scope) {
        return delegate.initSafeStandardObjects(scope);
    }

    @Override
    public Scriptable initStandardObjects(ScriptableObject scope) {
        return delegate.initStandardObjects(scope);
    }

    @Override
    public ScriptableObject initSafeStandardObjects() {
        return delegate.initSafeStandardObjects();
    }

    @Override
    public ScriptableObject initStandardObjects() {
        return delegate.initStandardObjects();
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        delegate.removePropertyChangeListener(l);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        delegate.addPropertyChangeListener(l);
    }

    @Override
    public TimeZone setTimeZone(TimeZone tz) {
        return delegate.setTimeZone(tz);
    }

    @Override
    public TimeZone getTimeZone() {
        return delegate.getTimeZone();
    }

    @Override
    public Locale setLocale(Locale loc) {
        return delegate.setLocale(loc);
    }

    @Override
    public Locale getLocale() {
        return delegate.getLocale();
    }

    @Override
    public ErrorReporter setErrorReporter(ErrorReporter reporter) {
        return delegate.setErrorReporter(reporter);
    }

    @Override
    public ErrorReporter getErrorReporter() {
        return delegate.getErrorReporter();
    }

    @Override
    public String getImplementationVersion() {
        return delegate.getImplementationVersion();
    }

    @Override
    public int getLanguageVersion() {
        return delegate.getLanguageVersion();
    }

    @Override
    public void unseal(Object sealKey) {
        delegate.unseal(sealKey);
    }

    @Override
    public void seal(Object sealKey) {
        delegate.seal(sealKey);
    }

    @Override
    public boolean isSealed() {
        return delegate.isSealed();
    }

    @Override
    public ContextFactory getFactory() {
        return delegate.getFactory();
    }

    @Override
    public ContextImpl impl() {
        return delegate.impl();
    }

    @Override
    public void removeActivationName(String name) {
        delegate.removeActivationName(name);
    }

    @Override
    public void addActivationName(String name) {
        delegate.addActivationName(name);
    }

    @Override
    public UnhandledRejectionTracker getUnhandledPromiseTracker() {
        return delegate.getUnhandledPromiseTracker();
    }

    @Override
    public void setTrackUnhandledPromiseRejections(boolean track) {
        delegate.setTrackUnhandledPromiseRejections(track);
    }

    @Override
    public void processMicrotasks() {
        delegate.processMicrotasks();
    }

    @Override
    public void enqueueMicrotask(Runnable task) {
        delegate.enqueueMicrotask(task);
    }

    @Override
    public GeneratedClassLoader createClassLoader(ClassLoader parent) {
        return delegate.createClassLoader(parent);
    }

    @Override
    public boolean isGenerateObserverCount() {
        return delegate.isGenerateObserverCount();
    }

    @Override
    public void setGenerateObserverCount(boolean generateObserverCount) {
        delegate.setGenerateObserverCount(generateObserverCount);
    }

    @Override
    public boolean hasFeature(int featureIndex) {
        return delegate.hasFeature(featureIndex);
    }

    @Override
    public void setJavaToJSONConverter(UnaryOperator<Object> javaToJSONConverter)
            throws IllegalArgumentException {
        delegate.setJavaToJSONConverter(javaToJSONConverter);
    }

    @Override
    public UnaryOperator<Object> getJavaToJSONConverter() {
        return delegate.getJavaToJSONConverter();
    }

    @Override
    public Scriptable newArray(Scriptable scope, Object[] elements) {
        return delegate.newArray(scope, elements);
    }

    @Override
    public Scriptable newArray(Scriptable scope, int length) {
        return delegate.newArray(scope, length);
    }

    @Override
    public Scriptable newObject(Scriptable scope, String constructorName, Object[] args) {
        return delegate.newObject(scope, constructorName, args);
    }

    @Override
    public Scriptable newObject(Scriptable scope, String constructorName) {
        return delegate.newObject(scope, constructorName);
    }

    @Override
    public Scriptable newObject(Scriptable scope) {
        return delegate.newObject(scope);
    }

    @Override
    public Script compileReader(
            Reader in,
            String sourceName,
            int lineno,
            Object securityDomain,
            Consumer<CompilerEnvirons> compilerEnvironsProcessor)
            throws IOException {
        return delegate.compileReader(
                in, sourceName, lineno, securityDomain, compilerEnvironsProcessor);
    }

    @Override
    public Object resumeContinuation(Object continuation, Scriptable scope, Object functionResult)
            throws ContinuationPending {
        return delegate.resumeContinuation(continuation, scope, functionResult);
    }

    @Override
    public ContinuationPending captureContinuation() {
        return delegate.captureContinuation();
    }

    @Override
    public Object callFunctionWithContinuations(Callable function, Scriptable scope, Object[] args)
            throws ContinuationPending {
        return delegate.callFunctionWithContinuations(function, scope, args);
    }

    @Override
    public Object executeScriptWithContinuations(Script script, Scriptable scope)
            throws ContinuationPending {
        return delegate.executeScriptWithContinuations(script, scope);
    }

    @Override
    public ScriptableObject initSafeStandardObjects(ScriptableObject scope, boolean sealed) {
        return delegate.initSafeStandardObjects(scope, sealed);
    }

    @Override
    public ScriptableObject initStandardObjects(ScriptableObject scope, boolean sealed) {
        return delegate.initStandardObjects(scope, sealed);
    }

    @Override
    public void setLanguageVersion(int version) {
        delegate.setLanguageVersion(version);
    }

    @Override
    public void close() {
        // NO-OP!
    }
}
