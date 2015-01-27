#!/usr/bin/perl -w
# -*- Mode: Perl; tab-width: 4; indent-tabs-mode: nil; -*-
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# usage: pattern-expander.pl knownfailures > knownfailures.expanded
#
# pattern-expander.pl reads the specified knownfailures file and
# writes to stdout an expanded set of failures where the wildcards
# ".*" are replaced with the set of possible values specified in the
# universe.data file.

use lib $ENV{TEST_DIR} . "/tests/mozilla.org/js";

use Patterns;

package Patterns;

processfile();

sub processfile
{
    my ($i, $j);

    while (<ARGV>) {

        chomp;

        $record = {};

        my ($test_id, $test_branch, $test_repo, $test_buildtype, $test_type, $test_os, $test_kernel, $test_processortype, $test_memory, $test_timezone, $test_options, $test_result, $test_exitstatus, $test_description) = $_ =~ 
            /TEST_ID=([^,]*), TEST_BRANCH=([^,]*), TEST_REPO=([^,]*), TEST_BUILDTYPE=([^,]*), TEST_TYPE=([^,]*), TEST_OS=([^,]*), TEST_KERNEL=([^,]*), TEST_PROCESSORTYPE=([^,]*), TEST_MEMORY=([^,]*), TEST_TIMEZONE=([^,]*), TEST_OPTIONS=([^,]*), TEST_RESULT=([^,]*), TEST_EXITSTATUS=([^,]*), TEST_DESCRIPTION=(.*)/;

        $record->{TEST_ID}            = $test_id;
        $record->{TEST_BRANCH}        = $test_branch;
        $record->{TEST_REPO}          = $test_repo;
        $record->{TEST_BUILDTYPE}     = $test_buildtype;
        $record->{TEST_TYPE}          = $test_type;
        $record->{TEST_OS}            = $test_os;
        $record->{TEST_KERNEL}        = $test_kernel;
        $record->{TEST_PROCESSORTYPE} = $test_processortype;
        $record->{TEST_MEMORY}        = $test_memory;
        $record->{TEST_TIMEZONE}      = $test_timezone;
        $record->{TEST_OPTIONS}       = $test_options;
        $record->{TEST_RESULT}        = $test_result;
        $record->{TEST_EXITSTATUS}    = $test_exitstatus;
        $record->{TEST_DESCRIPTION}   = $test_description;

        if ($DEBUG) {
            dbg("processfile: \$_=$_");
        }

        my @list1 = ();
        my @list2 = ();

        my $iuniversefield;
        my $universefield;

        $item1 = copyreference($record);
        if ($DEBUG) {
            dbg("processfile: check copyreference");
            dbg("processfile: \$record=" . recordtostring($record));
            dbg("processfile: \$item1=" . recordtostring($item1));
        }
        push @list1, ($item1);

        for ($iuniversefield = 0; $iuniversefield < @universefields; $iuniversefield++)
        {
            $universefield = $universefields[$iuniversefield];

            if ($DEBUG) {
                dbg("processfile: \$universefields[$iuniversefield]=$universefield, \$record->{$universefield}=$record->{$universefield}");
            }

            for ($j = 0; $j < @list1; $j++)
            {
                $item1 = $list1[$j];
                if ($DEBUG) {
                    dbg("processfile: item1 \$list1[$j]=" . recordtostring($item1));
                }
                # create a reference to a copy of the hash referenced by $item1
                if ($item1->{$universefield} ne '.*')
                {
                    if ($DEBUG) {
                        dbg("processfile: literal value");
                    }
                    $item2 = copyreference($item1);
                    if ($DEBUG) {
                        dbg("processfile: check copyreference");
                        dbg("processfile: \$item1=" . recordtostring($item1));
                        dbg("processfile: \$item2=" . recordtostring($item2));
                        dbg("processfile: pushing existing record to list 2: " . recordtostring($item2));
                    }
                    push @list2, ($item2);
                }
                else
                {
                    if ($DEBUG) {                    
                        dbg("processfile: wildcard value");
                    }
                    $keyfielduniversekey = getuniversekey($item1, $universefield);
                    @keyfielduniverse = getuniverse($keyfielduniversekey, $universefield);

                    if ($DEBUG) {
                        dbg("processfile: \$keyfielduniversekey=$keyfielduniversekey, \@keyfielduniverse=" . join(',', @keyfielduniverse));
                    }

                    for ($i = 0; $i < @keyfielduniverse; $i++)
                    {
                        $item2 = copyreference($item1);
                        if ($DEBUG) {
                            dbg("processfile: check copyreference");
                            dbg("processfile: \$item1=" . recordtostring($item1));
                            dbg("processfile: \$item2=" . recordtostring($item2));
                        }
                        $item2->{$universefield} = $keyfielduniverse[$i];
                        if ($DEBUG) {
                            dbg("processfile: pushing new record to list 2 " . recordtostring($item2));
                        }
                        push @list2, ($item2);
                    }
                }
                if ($DEBUG) {
                    for ($i = 0; $i < @list1; $i++)
                    {
                        dbg("processfile: \$list1[$i]=" . recordtostring($list1[$i]));
                    }
                    for ($i = 0; $i < @list2; $i++)
                    {
                        dbg("processfile: \$list2[$i]=" . recordtostring($list2[$i]));
                    }
                }
            }

            @list1 = @list2;
            @list2 = ();
        }
        for ($j = 0; $j < @list1; $j++)
        {
            $item1 = $list1[$j];
            push @records, ($item1);
        }
    }
    @records = sort sortrecords @records;

    dumprecords();
}

