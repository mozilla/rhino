#!/bin/bash -e
# -*- Mode: Shell-script; tab-width: 4; indent-tabs-mode: nil; -*-

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

if [[ -z "$TEST_DIR" ]]; then
    cat <<EOF
`basename $0`: error

TEST_DIR, the location of the Sisyphus framework, 
is required to be set prior to calling this script.
EOF
    exit 2
fi

if [[ ! -e $TEST_DIR/bin/library.sh ]]; then
    echo "TEST_DIR=$TEST_DIR"
    echo ""
    echo "This script requires the Sisyphus testing framework. Please "
    echo "cvs check out the Sisyphys framework from mozilla/testing/sisyphus"
    echo "and set the environment variable TEST_DIR to the directory where it"
    echo "located."
    echo ""

    exit 2
fi

source $TEST_DIR/bin/library.sh

TEST_JSDIR=${TEST_JSDIR:-$TEST_DIR/tests/mozilla.org/js}

usage()
{
    cat <<EOF
usage: process-logs.sh.sh -l testlogfiles -A arch -K kernel

variable            description
===============     ============================================================
testlogfiles        The test log to be processed. If testlogfiles is a file 
                    pattern it must be single quoted to prevent the shell from
                    expanding it before it is passed to the script.
EOF
    exit 2
}

while getopts "l:" optname; 
do
    case $optname in
        l) testlogfiles=$OPTARG;;
    esac
done

if [[ -z "$testlogfiles" ]]; then
    usage
fi

for testlogfile in `ls $testlogfiles`; do

    debug "testlogfile=$testlogfile"

    case $testlogfile in
	    *.log)
            worktestlogfile=$testlogfile
	        ;;
	    *.log.bz2)
            worktestlogfile=`mktemp $testlogfile.XXXXXX`
            bunzip2 -c $testlogfile > $worktestlogfile
	        ;;
	    *.log.gz)
            worktestlogfile=`mktemp $testlogfile.XXXXXX`
	        gunzip -c $testlogfile > $worktestlogfile
	        ;;
	    *)
	        echo "unknown log type: $f"
	        exit 2
	        ;;
    esac

    case "$testlogfile" in
        *,js,*) testtype=shell;;
        *,firefox,*) testtype=browser;;
        *,thunderbird,*) testtype=browser;;
        *,fennec,*) testtype=browser;;
        *) error "unknown testtype in logfile $testlogfile" $LINENO;;
    esac

    debug "testtype=$testtype"

    case "$testlogfile" in
        *,nightly*) buildtype=nightly;;
        *,opt,*) buildtype=opt;;
        *,debug,*) buildtype=debug;;
        *) error "unknown buildtype in logfile $testlogfile" $LINENO;
    esac

    debug "buildtype=$buildtype"

    branch=`echo $testlogfile | sed 's|.*,\([0-9]\.[0-9]*\.[0-9]*\).*|\1|'`

    debug "branch=$branch"

    repo=`grep -m 1 '^environment: TEST_MOZILLA_HG=' $worktestlogfile | sed 's|.*TEST_MOZILLA_HG=http://hg.mozilla.org.*/\([^\/]*\)|\1|'`
    if [[ -z "$repo" ]]; then
        repo=CVS
    fi
    debug "repo=$repo"

    case "$testlogfile" in 
        *,nt,*) OSID=nt;;
        *,linux,*) OSID=linux;;
        *,darwin,*) OSID=darwin;;
        *) 
            OSID=`grep -m 1 '^environment: OSID=' $worktestlogfile | sed 's|.*OSID=\(.*\)|\1|'`
            if [[ -z "$OSID" ]]; then
                error "unknown OS in logfile $testlogfile" $LINENO
            fi
            ;;
    esac

    debug "OSID=$OSID"

    kernel=`grep -m 1 '^environment: TEST_KERNEL=' $worktestlogfile | sed 's|.*TEST_KERNEL=\(.*\)|\1|'`
    if [[ "$OSID" == "linux" ]]; then
        kernel=`echo $kernel | sed 's|\([0-9]*\)\.\([0-9]*\)\.\([0-9]*\).*|\1.\2.\3|'`
    fi
    debug "kernel=$kernel"

    arch=`grep -m 1 '^environment: TEST_PROCESSORTYPE=' $worktestlogfile | sed 's|.*TEST_PROCESSORTYPE=\(.*\)|\1|'`
    debug "arch=$arch"

    memory=`grep -m 1 '^environment: TEST_MEMORY=' $worktestlogfile | sed 's|.*TEST_MEMORY=\(.*\)|\1|'`

    timezone=`basename $testlogfile | sed 's|^[-0-9]*\([-+]\)\([0-9]\{4,4\}\),.*|\1\2|'`
    debug "timezone=$timezone"

    jsoptions=`grep -m 1 '^arguments: javascriptoptions=' $worktestlogfile | sed 's|.*javascriptoptions=\(.*\)|\1|'`
    if [[ -z "$jsoptions" ]]; then
        jsoptions=none
    fi
    debug "jsoptions=$jsoptions"

    outputprefix=$testlogfile

    includetests="included-$branch-$testtype-$buildtype.tests"
    excludetests="excluded-$branch-$testtype-$buildtype.tests"

    grep '^include: ' $worktestlogfile | sed 's|include: ||' > $TEST_DIR/tests/mozilla.org/js/$includetests
    grep '^exclude: ' $worktestlogfile | sed 's|exclude: ||' > $TEST_DIR/tests/mozilla.org/js/$excludetests

    $TEST_DIR/tests/mozilla.org/js/known-failures.pl \
        -b "$branch" \
        -T "$buildtype" \
        -R "$repo" \
        -t "$testtype" \
        -o "$OSID" \
        -K "$kernel" \
        -A "$arch" \
        -M "$memory" \
        -z "$timezone" \
        -J "$jsoptions" \
        -r "$TEST_JSDIR/failures.txt" \
        -l "$worktestlogfile" \
        -O "$outputprefix"

    if [[ "$testlogfile" != "$worktestlogfile" ]]; then
        rm $worktestlogfile
        unset worktestlogfile
    fi
done
