#!/bin/bash
# -*- Mode: Shell-script; tab-width: 4; indent-tabs-mode: nil; -*-

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# usage: changes.sh [prefix]
# 
# combines the {prefix}*possible-fixes.log files into {prefix}possible-fixes.log
# and {prefix}*possible-regressions.log files into 
# possible-regressions.log.
#
# This script is useful in cases where log files from different machines, branches
# and builds are being investigated.

export LC_ALL=C

if cat /dev/null | sed -r 'q' > /dev/null 2>&1; then
   SED="sed -r"
elif cat /dev/null | sed -E 'q' > /dev/null 2>&1; then
   SED="sed -E"
else
   echo "Neither sed -r or sed -E is supported"
   exit 2
fi

workfile=`mktemp work.XXXXXXXX`
if [ $? -ne 0 ]; then
    echo "Unable to create working temp file"
    exit 2
fi

for f in ${1}*results-possible-fixes.log*; do
    case $f in
	*.log)
	    CAT=cat
	    ;;
	*.log.bz2)
	    CAT=bzcat
	    ;;
	*.log.gz)
	    CAT=zcat
	    ;;
	*.log.zip)
	    CAT="unzip -c"
	    ;;
	*)
	    echo "unknown log type: $f"
	    exit 2
	    ;;
    esac

    $CAT $f | $SED "s|$|:$f|" >> $workfile

done

sort -u $workfile > ${1}possible-fixes.log

rm $workfile


for f in ${1}*results-possible-regressions.log*; do
    case $f in
	*.log)
	    CAT=cat
	    ;;
	*.log.bz2)
	    CAT=bzcat
	    ;;
	*.log.gz)
	    CAT=zcat
	    ;;
	*.log.zip)
	    CAT="unzip -c"
	    ;;
	*)
	    echo "unknown log type: $f"
	    exit 2
	    ;;
    esac
    $CAT $f >> $workfile
done

sort -u $workfile > ${1}possible-regressions.log

rm $workfile



