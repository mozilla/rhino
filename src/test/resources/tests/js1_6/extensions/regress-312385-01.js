/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-312385-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 312385;
var summary = 'Generic methods with null or undefined |this|';
var actual = '';
var expect = true;
var voids = [null, undefined];

var generics = {
  String: [{ quote: [] },
{ substring: [] },
{ toLowerCase: [] },
{ toUpperCase: [] },
{ charAt: [] },
{ charCodeAt: [] },
{ indexOf: [] },
{ lastIndexOf: [] },
{ toLocaleLowerCase: [] },
{ toLocaleUpperCase: [] },
{ localeCompare: [] },
{ match: [/(?:)/] }, // match(regexp)
{ search: [] },
{ replace: [] },
{ split: [] },
{ substr: [] },
{ concat: [] },
{ slice: [] }],

  Array:  [{ join: [] },
{ reverse: [] },
{ sort: [] },
           // { push: [0] },  // push(item1, ...)
           // { pop: [] },
           // { shift: [] },
{ unshift: [] },
           // { splice: [0, 0, 1] }, // splice(start, deleteCount, item1, ...)
{ concat: [] },
{ indexOf: [] },
{ lastIndexOf: [] },
           // forEach is excluded since it does not return a value...
           /* { forEach: [noop] },  // forEach(callback, thisObj) */
{ map: [noop] },      // map(callback, thisObj)
{ filter: [noop] },   // filter(callback, thisObj)
{ some: [noop] },     // some(callback, thisObj)
{ every: [noop] }     // every(callback, thisObj)
    ]
};

printBugNumber(BUGNUMBER);
printStatus (summary);

for (var c in generics)
{
  var methods = generics[c];
  for (var i = 0; i < methods.length; i++)
  {
    var method = methods[i];

    for (var methodname in method)
    {
      for (var v = 0; v < voids.length; v++)
      {
	var lhs = c + '.' + methodname +
	  '(' + voids[v] + (method[methodname].length ?(', ' + method[methodname].toString()):'') + ')';

	var rhs = c + '.prototype.' + methodname +
	  '.apply(' + voids[v] + ', ' + method[methodname].toSource() + ')';

	var expr = lhs + ' == ' + rhs;
	printStatus('Testing ' + expr);

	try
	{
	  printStatus('lhs ' + lhs + ': ' + eval(lhs));
	}
	catch(ex)
	{
	  printStatus(ex + '');
	}

	try
	{
	  printStatus('rhs ' + rhs + ': ' + eval(rhs));
	}
	catch(ex)
	{
	  printStatus(ex + '');
	}

	try
	{
	  actual = comparelr(eval(lhs), eval(rhs));
	}
	catch(ex)
	{
	  actual = ex + '';
	}
	reportCompare(expect, actual, expr);
	printStatus('');
      }
    }
  }
}

function comparelr(lhs, rhs)
{
 
  if (lhs.constructor.name != 'Array')
  {
    return (lhs == rhs);
  }

  return (lhs.toSource() == rhs.toSource());
}

function noop()
{
}
