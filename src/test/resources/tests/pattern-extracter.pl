#!/usr/bin/perl -w
# -*- Mode: Perl; tab-width: 4; indent-tabs-mode: nil; -*-
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# usage: pattern-extracter.pl knownfailures.expanded > knownfailures
#
# pattern-extracter.pl reads the specified expanded knownfailures file
# (see pattern-expander.pl) and writes to stdout a set of knownfailures
# where repetitions of values found in the universe.data file are
# replaced with wildcards ".*".

use lib $ENV{TEST_DIR} . "/tests/mozilla.org/js";

use Patterns;

package Patterns;


my $universefield;

processfile();

sub processfile
{
    my $recordcurr = {};
    my $recordprev;

    my @output;
    my $keycurr = '';
    my $keyprev = '';
    my @values = ();
    my $universefielduniversekey; # universekey for universefield
    my @universefielduniverse;
    my $i;
    my $j;
    my $v;

    while (<ARGV>) {

        chomp;

        $recordcurr = {};

        my ($test_id, $test_branch, $test_repo, $test_buildtype, $test_type, $test_os, $test_kernel, $test_processortype, $test_memory, $test_timezone, $test_options, $test_result, $test_exitstatus, $test_description) = $_ =~ 
            /TEST_ID=([^,]*), TEST_BRANCH=([^,]*), TEST_REPO=([^,]*), TEST_BUILDTYPE=([^,]*), TEST_TYPE=([^,]*), TEST_OS=([^,]*), TEST_KERNEL=([^,]*), TEST_PROCESSORTYPE=([^,]*), TEST_MEMORY=([^,]*), TEST_TIMEZONE=([^,]*), TEST_OPTIONS=([^,]*), TEST_RESULT=([^,]*), TEST_EXITSTATUS=([^,]*), TEST_DESCRIPTION=(.*)/;

        $recordcurr->{TEST_ID}            = $test_id;
        $recordcurr->{TEST_BRANCH}        = $test_branch;
        $recordcurr->{TEST_REPO}          = $test_repo;
        $recordcurr->{TEST_BUILDTYPE}     = $test_buildtype;
        $recordcurr->{TEST_TYPE}          = $test_type;
        $recordcurr->{TEST_OS}            = $test_os;
        $recordcurr->{TEST_KERNEL}        = $test_kernel;
        $recordcurr->{TEST_PROCESSORTYPE} = $test_processortype;
        $recordcurr->{TEST_MEMORY}        = $test_memory;
        $recordcurr->{TEST_TIMEZONE}      = $test_timezone;
        $recordcurr->{TEST_OPTIONS}       = $test_options;
        $recordcurr->{TEST_RESULT}        = $test_result;
        $recordcurr->{TEST_EXITSTATUS}    = $test_exitstatus;
        $recordcurr->{TEST_DESCRIPTION}   = $test_description;

        push @records, ($recordcurr);
    }

    for ($j = $#universefields; $j >= 0; $j--)
    {
        $universefield   = $universefields[$j];

        @records = sort {getkey($a, $universefield) cmp getkey($b, $universefield);} @records;

        $recordprev = $records[0];
        $keyprev    = getkey($recordprev, $universefield);
        @values     = ();

        my $recordtemp;
        my $keytemp;

        dbg("processfile: begin processing records for \$universefields[$j]=$universefield");

        for ($i = 0; $i < @records; $i++)
        {
            $recordcurr = $records[$i];
            $keycurr = getkey($recordcurr, $universefield);

            dbg("processfile: processing record[$i]");
            dbg("processfile: recordprev: " . recordtostring($recordprev));
            dbg("processfile: recordcurr: " . recordtostring($recordcurr));
            dbg("processfile: \$keyprev=$keyprev");
            dbg("processfile: \$keycurr=$keycurr");

            if ($keycurr ne $keyprev)
            {
                # key changed, must output previous record
                dbg("processfile: new key");
                $universefielduniversekey = getuniversekey($recordprev, $universefield);
                @universefielduniverse = getuniverse($universefielduniversekey, $universefield);
                dbg("processfile: \@values: ". join(',', @values));
                dbg("processfile: \$universefielduniversekey=$universefielduniversekey, \@universefielduniverse=" . join(',', @universefielduniverse));
                @values = ('.*') if (arraysequal($universefield, \@values, \@universefielduniverse));
                dbg("processfile: \@values=" . join(',', @values));

                for ($v = 0; $v < @values; $v++)
                {
                    dbg("processfile: stuffing $values[$v]");
                    $recordtemp = copyreference($recordprev);
                    $recordtemp->{$universefield} = $values[$v];
                    dbg("processfile: stuffed $recordtemp->{$universefield}");
                    dbg("processfile: recordprev: " . recordtostring($recordprev));
                    dbg("processfile: output: " . recordtostring($recordtemp));
                    push @output, ($recordtemp);
                }
                @values = ();
            }
            dbg("processfile: collecting \$recordcurr->{$universefield}=$recordcurr->{$universefield}");
            push @values, ($recordcurr->{$universefield});
            $keyprev = $keycurr;
            $recordprev = $recordcurr;
        }
        dbg("processfile: finish processing records for \$universefields[$j]=$universefield");
        if (@values)
        {
            dbg("processfile: last record for \$universefields[$j]=$universefield has pending values");
            $universefielduniversekey = getuniversekey($recordprev, $universefield);
            @universefielduniverse = getuniverse($universefielduniversekey, $universefield);
            dbg("processfile: \@values: ". join(',', @values));
            dbg("processfile: \$universefielduniversekey=$universefielduniversekey, \@universefielduniverse=" . join(',', @universefielduniverse));
            @values = ('.*') if (arraysequal($universefield, \@values, \@universefielduniverse));
            dbg("processfile: \@values=" . join(',', @values));

            for ($v = 0; $v < @values; $v++)
            {
                dbg("processfile: stuffing $values[$v]");
                $recordtemp = copyreference($recordprev);
                $recordtemp->{$universefield} = $values[$v];
                dbg("processfile: stuffed $recordprev->{$universefield}");
                dbg("processfile: recordprev: " . recordtostring($recordprev));
                dbg("processfile: output: " . recordtostring($recordtemp));
                push @output, ($recordtemp);
            }
            @values = ();
        }
        @records = @output;
        @output = ();
    }

    @records = sort sortrecords @records;
    dumprecords();
}


sub getkey 
{
    my ($record, $universefield) = @_;

    my $i;

    my $key = '';

    for ($i = 0; $i < @sortkeyfields; $i++)
    {
        if ($sortkeyfields[$i] ne $universefield)
        {
            $key .= $record->{$sortkeyfields[$i]}
        }
    }
    return $key;
}

sub arraysequal 
{
    my ($universefield, $larrayref, $rarrayref) = @_;
    my $i;

    dbg("arraysequal: checking $universefield if " . (join ',', @{$larrayref}) . " is equal to " . (join ',', @{$rarrayref}));

    # fail if lengths not equal
    return 0 if (@{$larrayref} != @{$rarrayref});

    # if the universe field is 'important', fail if lengths are 1, 
    # so that important field singletons are not replaced by wildcards.
    my @importantfields = ('TEST_BRANCH', 'TEST_REPO', 'TEST_BUILDTYPE', 'TEST_TYPE', 'TEST_OS');
    my @matches = grep /$universefield/, @importantfields;

    return 0 if ( @matches && @{$larrayref} == 1);

    for ($i = 0; $i < @{$larrayref}; $i++)
    {
        return 0 if ($rarrayref->[$i] ne $larrayref->[$i]);
    }
    dbg("arraysequal: equal");
    return 1;
}

