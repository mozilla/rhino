/* -*- tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 8 -*-
 *      
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
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
 *   Patrick Beard
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

/*
Implementing the interface java.util.Enumeration passing the object
with JavaScript implementation directly to the constructor.
This is a shorthand for JavaAdapter constructor:

elements = new JavaAdapter(java.util.Enumeration, {
        index: 0, 
        elements: array,
	hasMoreElements: function ...
        nextElement: function ...
		});
 */

// an array to enumerate.
var array = [0, 1, 2];

// create an array enumeration.
var elements = new java.util.Enumeration({
        index: 0,
        elements: array,
        hasMoreElements: function() {
                return (this.index < this.elements.length);
	},      
        nextElement: function() {
                return this.elements[this.index++];
	}
    });

// now print out the array by enumerating through the Enumeration
while (elements.hasMoreElements())
	print(elements.nextElement());
