package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.*;


/**
 * Models application initialization of a real-world rhino application.
 * This fails against Rhino 1.7.8 RC1
 * 
 * @author Raimund Jacob-Bloedorn <jacob@pinuts.de>
 */
public class DynamicScopeTest extends TestCase {

	
	/**
	 * Define the specific behaviour of our application.
	 */
    static class DynamicScopeContextFactory extends ContextFactory
    {
        protected boolean hasFeature(Context cx, int featureIndex)
        {
            if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE ||
                    featureIndex == Context.FEATURE_STRICT_VARS ||
                    featureIndex == Context.FEATURE_E4X ||
                    featureIndex == Context.FEATURE_STRICT_EVAL ||
                    featureIndex == Context.FEATURE_LOCATION_INFORMATION_IN_ERROR ||
                    featureIndex == Context.FEATURE_STRICT_MODE
                    ) {
                return true;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    
    /**
     * Sometimes a new context ist required.
     * The constructor is protected so this  
     */
    private static class FreshContext extends Context {
    	public FreshContext(ContextFactory contextFactory) {
    		super(contextFactory);
    	}
    }

    
    /**
     * Sealed standard objects vs dynamic scopes.
     */
	public void testInitStandardObjectsSealed()
	{
		ContextFactory contextFactory = new DynamicScopeContextFactory();
		
		// This is what we do on initialization ...
		final Context cx = contextFactory.enterContext();
		try {
			cx.setLanguageVersion(Context.VERSION_ES6);
	        cx.setOptimizationLevel(0);

	        // Used to fail with org.mozilla.javascript.EvaluatorException: Cannot modify a property of a sealed object: iterator.
	        final ScriptableObject scope = cx.initStandardObjects(new TopLevel(), true); 
	        
	        Object result = cx.evaluateString(scope, "42", "source", 1, null);
			assertEquals(42, result);
		} finally {
			// cx.exit();
		}
		
		// ... Lots of switches between JS and Java code here ...
		
		// Almost the same code block as above
		final Context cx2 = new FreshContext(contextFactory);
		try {
			cx2.setLanguageVersion(Context.VERSION_ES6);
	        cx2.setOptimizationLevel(0);
	        
	        // Used to fail with org.mozilla.javascript.EvaluatorException: Cannot modify a property of a sealed object: iterator.
	        final ScriptableObject scope = cx.initStandardObjects(new TopLevel(), true);
	        
	        Object result = cx.evaluateString(scope, "23", "source", 1, null);
			assertEquals(23, result);
		} finally {
			// cx.exit
		}
	}
	
}
