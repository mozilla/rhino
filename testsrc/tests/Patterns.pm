# -*- Mode: Perl; tab-width: 4; indent-tabs-mode: nil; -*-
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

package Patterns;

sub getuniversekey
{
    my ($machinerecord, $excludeduniversefield) = @_;
    my $i;
    my $key = '';

    if ($DEBUG) {
        dbg("getuniversekey: \$machinerecord=" . recordtostring($machinerecord) . ", \$excludeduniversefield=$excludeduniversefield");
    }

    for ($i = 0; $i < @universefields; $i++)
    {
        if ($DEBUG) {
            dbg("getuniversekey: \$universefields[$i]=$universefields[$i]");
        }

        if ($universefields[$i] ne $excludeduniversefield)
        {
            $key .= $machinerecord->{$universefields[$i]}
        }
    }

    if ($DEBUG) {
        dbg("getuniversekey=$key");
    }

    return $key;
}

sub getuniverse
{
    my ($universekey, $excludeduniversefield) = @_;
    my $i;
    my $value;
    my $testrun;
    my @universe = ();
    my %universehash = ();

    if ($DEBUG) {
        dbg("getuniverse: \$universekey=$universekey, \$excludeduniversefield=$excludeduniversefield");
    }

    for ($i = 0; $i < @testruns; $i++)
    {
        $testrun = $testruns[$i];
        if ($DEBUG) {
            dbg("getuniverse: \$testruns[$i]=" . recordtostring($testrun));
        }
        $testrununiversekey = getuniversekey($testrun, $excludeduniversefield);
        if ($DEBUG) {
            dbg("getuniverse: \$testrununiversekey=$testrununiversekey");
        }
        if ($testrununiversekey =~ /$universekey/)
        {
            if ($DEBUG) {
                dbg("getuniverse: matched \$testrununiversekey=$testrununiversekey to \$universekey=$universekey");
            }
            $value = $testrun->{$excludeduniversefield};
            
            if ($DEBUG) {
                dbg("getuniverse: \$testrun->{$excludeduniversefield}=$value");
            }

            if (! $universehash{$value} )
            {
                if ($DEBUG) {
                    dbg("getuniverse: pushing $value");
                }
                push @universe, ($value);
                $universehash{$value} = 1;
            }
        }
    }
    @universe = sort @universe;
    if ($DEBUG) {
        dbg("getuniverse=" . join(',', @universe));
    }
    return @universe;
}

sub recordtostring
{
    my ($record) = @_;
    my $j;
    my $line   = '';
    my $field;

    for ($j = 0; $j < @recordfields - 1; $j++)
    {
        $field = $recordfields[$j];
        if ($DEBUG) {
            dbg("recordtostring: \$field=$field, \$record->{$field}=$record->{$field}");
        }
        $line .= "$field=$record->{$field}, ";
    }
    $field = $recordfields[$#recordfields];
    if ($DEBUG) {
        dbg("recordtodtring: \$field=$field, \$record->{$field}= $record->{$field}");
    }
    $line .= "$field=$record->{$field}";

    return $line;
}

sub dumprecords
{
    my $record;
    my $line;
    my $prevline = '';
    my $i;

    if ($DEBUG) {
        dbg("dumping records");
    }

#    @records = sort sortrecords @records;

    for ($i = 0; $i < @records; $i++)
    {
        $record = $records[$i];
        $line   = recordtostring($record);
        if ($line eq $prevline)
        {
            dbg("DUPLICATE $line") if ($DEBUG);
        }
        else
        {
            print "$line\n";
            $prevline = $line;
        }
    }
}

sub sortrecords
{
    return recordtostring($a) cmp recordtostring($b);
}

sub dbg 
{
    if ($DEBUG)
    {
        print STDERR "DEBUG: " . join(" ", @_) . "\n";
    }
}

sub copyreference
{
    my ($fromreference) = @_;
    my $toreference = {};
    my $key;

    foreach $key (keys %{$fromreference})
    {
        $toreference->{$key} = $fromreference->{$key};
    }
    return $toreference;
}

BEGIN 
{
    dbg("begin");

    my $test_dir = $ENV{TEST_DIR} || "/work/mozilla/mozilla.com/test.mozilla.com/www";

    $DEBUG = $ENV{DEBUG};

    @recordfields   = ('TEST_ID', 'TEST_BRANCH', 'TEST_REPO', 'TEST_BUILDTYPE', 'TEST_TYPE', 'TEST_OS', 'TEST_KERNEL', 'TEST_PROCESSORTYPE', 'TEST_MEMORY', 'TEST_TIMEZONE', 'TEST_OPTIONS', 'TEST_RESULT', 'TEST_EXITSTATUS', 'TEST_DESCRIPTION');
    @sortkeyfields  = ('TEST_ID', 'TEST_RESULT', 'TEST_EXITSTATUS', 'TEST_DESCRIPTION', 'TEST_BRANCH', 'TEST_REPO', 'TEST_BUILDTYPE', 'TEST_TYPE', 'TEST_OS', 'TEST_KERNEL', 'TEST_PROCESSORTYPE', 'TEST_MEMORY', 'TEST_TIMEZONE', 'TEST_OPTIONS');
    @universefields = ('TEST_BRANCH', 'TEST_REPO', 'TEST_BUILDTYPE', 'TEST_TYPE', 'TEST_OS', 'TEST_KERNEL', 'TEST_PROCESSORTYPE', 'TEST_MEMORY', 'TEST_TIMEZONE', 'TEST_OPTIONS');

    @records = ();

    @testruns = ();

    my $UNIVERSE = $ENV{TEST_UNIVERSE} || "$test_dir/tests/mozilla.org/js/universe.data";

    dbg "UNIVERSE=$UNIVERSE";

    open TESTRUNS, "<$UNIVERSE" or die "$?";

    while (<TESTRUNS>) {

        chomp;

        dbg("BEGIN: \$_=$_\n");

        my $record = {};

        my ($test_os, $test_kernel, $test_processortype, $test_memory, $test_timezone, $test_jsoptions, $test_branch, $test_repo, $test_buildtype, $test_type) = $_ =~ 
            /^TEST_OS=([^,]*), TEST_KERNEL=([^,]*), TEST_PROCESSORTYPE=([^,]*), TEST_MEMORY=([^,]*), TEST_TIMEZONE=([^,]*), TEST_OPTIONS=([^,]*), TEST_BRANCH=([^,]*), TEST_REPO=([^,]*), TEST_BUILDTYPE=([^,]*), TEST_TYPE=([^,]*)/;

        $record->{TEST_ID}            = 'dummy';
        $record->{TEST_RESULT}        = 'dummy';
        $record->{TEST_EXITSTATUS}    = 'dummy';
        $record->{TEST_DESCRIPTION}   = 'dummy';

        $record->{TEST_BRANCH}        = $test_branch;
        $record->{TEST_REPO}          = $test_repo;
        $record->{TEST_BUILDTYPE}     = $test_buildtype;
        $record->{TEST_TYPE}          = $test_type;
        $record->{TEST_OS}            = $test_os;
        $record->{TEST_KERNEL}        = $test_kernel;
        $record->{TEST_PROCESSORTYPE} = $test_processortype;
        $record->{TEST_MEMORY}        = $test_memory;
        $record->{TEST_TIMEZONE}      = $test_timezone;
        $record->{TEST_OPTIONS}       = $test_jsoptions;

        dbg("BEGIN: testrun: " . recordtostring($record));

        push @testruns, ($record);
    }

    close TESTRUNS;

}

1;
