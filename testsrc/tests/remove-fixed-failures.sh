#!/bin/bash
# -*- Mode: Shell-script; tab-width: 4; indent-tabs-mode: nil; -*-

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

if [[ ! -e "$1" || ! -e "$2" ]]; then
    cat	<<EOF
Usage: remove-fixed-failures.sh possible-fixes.log failures.log

possible-fixes.log contains the possible fixes from the most recent
test run.

failures.log contains the current known failures.

remove-fixed-failures.sh removes each pattern in possible-fixes.log
from failures.log.

The original failures.log is saved as failures.log.orig for safe keeping.

EOF
    exit 1
fi

fixes="$1"
failures="$2"

# save the original failures file in case of an error
cp $failures $failures.orig

# create a temporary file to contain the current list 
# of failures.
workfailures=`mktemp working-failures.XXXXX`
workfixes=`mktemp working-fixes.XXXXX`

trap "rm -f ${workfailures} ${workfailures}.temp ${workfixes}*;" EXIT

# create working copy of the failures file
cp $failures $workfailures
cp $fixes $workfixes

sed -i.bak 's|:[^:]*\.log||' $workfixes;

grep -Fv -f $workfixes ${workfailures} > ${workfailures}.temp

mv $workfailures.temp $workfailures

mv $workfailures $failures

