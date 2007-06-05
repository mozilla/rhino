/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * This class implements generator objects. See 
 * http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Generators
 *
 * @author Norris Boyd
 */
public final class NativeGenerator extends IdScriptableObject {
    private static final Object GENERATOR_TAG = new Object();
    
    static void init(Scriptable scope, boolean sealed) {
        // Generator
        new NativeGenerator().exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);

        // StopIteration
        NativeObject obj = new StopIteration();
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) { obj.sealObject(); }
        ScriptableObject.defineProperty(scope, STOP_ITERATION, obj,
                                        ScriptableObject.DONTENUM);
    }
    
    /**
     * Only for constructing the prototype object.
     */
    private NativeGenerator() { }
    
    NativeGenerator(NativeFunction function, Object savedState) {
        this.function = function;
        this.savedState = savedState;
    }
    
    public static final String STOP_ITERATION = "StopIteration";
    
    static class StopIteration extends NativeObject {
        public String getClassName() { return STOP_ITERATION; }
    }
    
    public static final int GENERATOR_SEND  = 0,
                            GENERATOR_THROW = 1,
                            GENERATOR_CLOSE = 2;

    public String getClassName() {
        return "Generator";
    }

    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
          case Id_constructor:    arity=1; s="constructor";    break;
          case Id_close:          arity=1; s="close";          break;
          case Id_next:           arity=1; s="next";           break;
          case Id_send:           arity=0; s="send";           break;
          case Id_throw:          arity=0; s="throw";          break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(GENERATOR_TAG, id, s, arity);
    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(GENERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (!(thisObj instanceof NativeGenerator))
            throw incompatibleCallError(f);
        
        NativeGenerator generator = (NativeGenerator) thisObj;
        
        switch (id) {
        
          case Id_constructor:
            // TODO(js1.7gen): Shouldn't have a constructor. Currently need 
            // one to get Generator.prototype
            return null;
            
          case Id_close:
            // need to run any pending finally clauses
	        return generator.resume(cx, scope, GENERATOR_CLOSE,
	          		                new RuntimeException());

          case Id_next: 
            // arguments to next() are ignored
            generator.firstTime = false;
            return generator.resume(cx, scope, GENERATOR_SEND,
            		                Undefined.instance);

          case Id_send:
            if (generator.firstTime) {
                throw ScriptRuntime.typeError0("msg.send.newborn");
            }
            return generator.resume(cx, scope, GENERATOR_SEND,
            		args.length > 0 ? args[0] : Undefined.instance);

          case Id_throw:
            return generator.resume(cx, scope, GENERATOR_THROW,
            		args.length > 0 ? args[0] : Undefined.instance);

          default: 
        	throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private Object resume(Context cx, Scriptable scope, int operation,
    		              Object value)
    {
        if (savedState == null) {
        	if (operation == GENERATOR_CLOSE)
        		return Undefined.instance;
        	Object thrown = operation == GENERATOR_THROW
                ? value
                : ScriptableObject.getTopLevelScope(scope).get(STOP_ITERATION, 
                        scope);
            throw new JavaScriptException(thrown, lineSource, lineNumber);
        }
        try {
            synchronized (this) {
              // generator execution is necessarily single-threaded and
              // non-reentrant. 
              // See https://bugzilla.mozilla.org/show_bug.cgi?id=349263
              if (locked)
                  throw ScriptRuntime.typeError0("msg.already.exec.gen");
              locked = true;
            }
            return function.resumeGenerator(cx, scope, operation, savedState, 
                                            value);
    	} catch (RhinoException e) {
    		lineNumber = e.lineNumber();
    		lineSource = e.lineSource();
    		savedState = null;
    		throw e;
    	} finally {
            synchronized (this) {
              locked = false;
            }
    		if (operation == GENERATOR_CLOSE)
    			savedState = null;
    	}
    }

// #string_id_map#

    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2007-05-09 08:23:27 EDT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==4) {
                c=s.charAt(0);
                if (c=='n') { X="next";id=Id_next; }
                else if (c=='s') { X="send";id=Id_send; }
            }
            else if (s_length==5) {
                c=s.charAt(0);
                if (c=='c') { X="close";id=Id_close; }
                else if (c=='t') { X="throw";id=Id_throw; }
            }
            else if (s_length==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor           = 1,
        Id_close                 = 2,
        Id_next                  = 3,
        Id_send                  = 4,
        Id_throw                 = 5,
        MAX_PROTOTYPE_ID         = 5;

// #/string_id_map#

    private NativeFunction function;
    private Object savedState;
    private String lineSource;
    private int lineNumber;
    private boolean firstTime = true;
    private boolean locked;
}

