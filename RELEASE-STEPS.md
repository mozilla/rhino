# Rhino Release Process

## Prepare the repository

Update the version in gradle.properties to the new release version.

Update RELEASE_NOTES.md to include at least a summary of the major features
of the release. (This is historical, but many find it helpful to have a single
file in the repo that lists the major changes.)

Update README.md to add a row to the table of releases that points to the
GitHub release that we'll create in the next step (just follow the pattern
that's there).

Now might be a good time to run "./gradlew publishToMavenLocal" and use the
published JARs as a sanity check.

## Update Compatibility Table

The offial Kangax "compat table" now supports Rhino, but it's convenient
to have our own that shows progress across all releases. Here's how to
update it:

    git clone -b gh-pages https://github.com/gbrail/node-compat-table.git
    cd node-compat-table
    
Now, edit "rhinoall.sh" to include the new release -- it includes a series
of lines that fetch old releases, and use your local build of the new one.
Then, update the table:

    ./rhinoall.sh

The resulting "index.html" can be copied into "docs/compat/engines.html" in 
this repo.

## Push the Release to GitHub

At this point, the current contents of your directory correspond to the 
new release. Prepare a pull request containing the changes, submit it,
and merge it -- the result will be that the head of the "master" branch
will build your new release.

Update to that branch and create a tag for the release, where XX is a number
like "1_7_14":

    git pull origin master
    git tag Rhino_XX_Release
    git push origin Rhino_XX_Release

Now, on the Rhino "Releases" tab in GitHub, create a release that corresponds
to the new tag. Include the following:

* A cut and paste of the part of RELEASE_NOTES.md added for the release
* The three JARs created by "./gradlew.jar"
* The ZIP file created by "./gradlew distZip"
* A ZIP of the source will be included automatically by GitHub

## Push the release to Maven Central

The "Publish to Maven Central" action on GitHub Actions will automatically
build the release, sign the JARs, and push it to oss.sonatype.org in the
"org.mozilla" area. Log in to oss.sonatype.org, verify that all the checks
that happen there were successful, and "close" the release. It will appear
on Maven Central a few hours later.

## Update Homebrew

The Homebrew team for Mac does not necessarily pick up Rhino releases 
automatically. It may be necessary to submit a PR to the "homebrew/homebrew"
repo in GitHub for a change to the file "Library/Formula/rhino.rb".

## Prepare for Next Release

Now it's time to move to the next "SNAPSHOT" release. Update gradle.properties,
create a PR, and push the new PR. Now development can proceeed anew!

