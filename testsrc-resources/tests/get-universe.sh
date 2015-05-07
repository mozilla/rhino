#!/bin/bash -e
# -*- Mode: Shell-script; tab-width: 4; indent-tabs-mode: nil; -*-

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# usage: get-universe.sh logfile(s) > universe.data
#
# get-universe.sh reads the processed javascript logs and writes to
# stdout the unique set of fields to be used as the "universe" of test
# run data. These values are used by pattern-expander.pl and
# pattern-extracter.pl to encode the known failure files into regular
# expressions.

export LC_ALL=C # handle all character sets

(for f in $@; do
    grep -h -m 1 TEST_ID $f | tr -dc '[\040-\177\n]' | sed 's|^TEST_ID=[^,]*, \(TEST_BRANCH=[^,]*, TEST_REPO=[^,]*, TEST_BUILDTYPE=[^,]*, TEST_TYPE=[^,]*\), \(TEST_OS=[^,]*, TEST_KERNEL=[^,]*, TEST_PROCESSORTYPE=[^,]*, TEST_MEMORY=[^,]*, TEST_TIMEZONE=[^,]*, TEST_OPTIONS=[^,]*\),.*|\2, \1|' 
done) | sort -u
