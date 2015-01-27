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

#
# options processing
#
usage()
{
    cat <<EOF
usage: detect-universe.sh -p products -b branches -R repositories -T buildtypes

Outputs to stdout the universe data for this machine.

variable            description
===============     ============================================================
-p products         required. one or more of firefox, thunderbird, fennec, js
-b branches         required. one or more of supported branches. set library.sh
-R repositories     required. one or more of CVS, mozilla-central, ...
-T buildtype        required. one or more of opt debug

if an argument contains more than one value, it must be quoted.
EOF
    exit 2
}

while getopts "p:b:R:T:" optname
do
    case $optname in
        p)
            products=$OPTARG;;
        b)
            branches=$OPTARG;;
        R)
            repos=$OPTARG;;
        T)
            buildtypes=$OPTARG;;
    esac
done

if [[ -z "$products" || -z "$branches" || -z "$buildtypes" ]]; then
    usage
fi

source $TEST_DIR/bin/library.sh

(for product in $products; do
    for branch in $branches; do
        for repo in $repos; do

            if [[ ("$branch" != "1.8.0" && "$branch" != "1.8.1" && "$branch" != "1.9.0") && $repo == "CVS" ]]; then
                continue;
            fi

            if [[ ("$branch" == "1.8.0" || "$branch" == "1.8.1" || "$branch" == "1.9.0") && $repo != "CVS" ]]; then
                continue
            fi

            for buildtype in $buildtypes; do
                if [[ $product == "js" ]]; then
                    testtype=shell
                else
                    testtype=browser
                fi
                echo "TEST_OS=$OSID, TEST_KERNEL=$TEST_KERNEL, TEST_PROCESSORTYPE=$TEST_PROCESSORTYPE, TEST_MEMORY=$TEST_MEMORY, TEST_TIMEZONE=$TEST_TIMEZONE, TEST_BRANCH=$branch, TEST_REPO=$repo, TEST_BUILDTYPE=$buildtype, TEST_TYPE=$testtype"
            done
        done
    done
done) | sort -u
