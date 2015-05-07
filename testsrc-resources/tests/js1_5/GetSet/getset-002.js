/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'getset-002.js';

var t = {   
  _y: "<initial y>",

  y getter: function get_y ()
  {
    var rv;
    if (typeof this._y == "string")
      rv = "got " + this._y;
    else
      rv = this._y;

    return rv;
  },

  y setter: function set_y (newVal)
  {
    this._y = newVal;
  }
}


  test(t);

function test(t)
{
  enterFunc ("test");
   
  printStatus ("Basic Getter/ Setter test (object literal notation)");

  reportCompare ("<initial y>", t._y, "y prototype check");

  reportCompare ("got <initial y>", t.y, "y getter, before set");

  t.y = "new y";
  reportCompare ("got new y", t.y, "y getter, after set");

  t.y = 2;
  reportCompare (2, t.y, "y getter, after numeric set");

  var d = new Date();
  t.y = d;
  reportCompare (d, t.y, "y getter, after date set");

}       
