/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
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
 * Portions created by eBay are Copyright (c) 2005, 2012 eBay Inc. All rights reserved.
 * 
 * Contributor(s):
 *   Yitao Yao
 *   Justin Early
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
 * ***** END LICENSE BLOCK ***** */
package org.mozilla.javascript;

/**
 * Java Proxy for Native JavaScript Object
 * 
 * an eBay extension to Rhino - EBAY MOD
 */
public interface IJsJavaProxy {
	
	String JS_JAVA_PROXY = "_js_java_proxy";
	
	Scriptable getJsNative();

}
