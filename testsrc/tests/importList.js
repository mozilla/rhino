/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var include = document.location.href.indexOf('?include') != -1;
document.write( '<title>Import ' + (include ? 'Include' : 'Exclude') + ' Test List<\/title>')

function doImport()
{
  var lines =
    document.forms["foo"].elements["testList"].value.split(/\r?\n/);
  var suites = window.opener.suites;
  var elems = window.opener.document.forms["testCases"].elements;

  if (include && document.forms["foo"].elements["clear_all"].checked)
    window.opener._selectNone();

  for (var l in lines)
  {
    if (!lines[l])
    {
      continue;
    }

    if (lines[l].search(/^\s$|\s*\#/) == -1)
    {
      var ary = lines[l].match (/(.*)\/(.*)\/(.*)/);

      if (!ary)
      {
        if (!confirm ("Line " + l + " '" +
                      lines[l] + "' is confusing, " +
                      "continue with import?"))
        {
          return;
        }
      }
                 
      if (suites[ary[1]] &&
          suites[ary[1]].testDirs[ary[2]] &&
          suites[ary[1]].testDirs[ary[2]].tests[ary[3]])
      {
        var radioname = suites[ary[1]].testDirs[ary[2]].tests[ary[3]].id;
        var radio = elems[radioname];
        if (include && !radio.checked)
        {
          radio.checked = true; 
          window.opener.onRadioClick(radio);
        }
        else if  (!include && radio.checked)
        {
          radio.checked = false; 
          window.opener.onRadioClick(radio);
        }
      }
    }
  }

  setTimeout('window.close();', 200);
                 
}
