package org.mozilla.javascript.tests;

import static org.junit.Assert.assertNotNull;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;


public class TestJavaAdapter{
	Context cx=null;
	Scriptable topScope=null;
	@Before
	public void enterContext(){
		cx=Context.enter();
		topScope = cx.initStandardObjects();
	}
	@After
	public void exitContext() {
		cx.exit();
	}
	
	interface C {
		int methodInC(String str);
	}
	interface B extends C {
		int methodInB(String str);
	}
	public abstract class A implements B{
	}
	@Test
	public void testOverrideMethodInMultiLayerInterface() {
		String testCode =
	            "JavaAdapter(Packages."+A.class.getName()+",{methodInC:function(){},methodInB:function(){}},null)";
		
		NativeJavaObject aJavaObject=(NativeJavaObject)cx.evaluateString(topScope, testCode,"", 1, null);
		
		
		Method overrideMethod=null;
		try {
			overrideMethod=aJavaObject.unwrap().getClass().getDeclaredMethod("methodInC", String.class);
		}catch(NoSuchMethodException e) {
		}
		
		assertNotNull("Failed to override method 'public int methodInC(String str)' from multi-layer interface C"
				     	,overrideMethod);	
	}
}
