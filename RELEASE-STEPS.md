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

## Update Native Compatibility Table

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

## Update Kangax Compatibility Table

Check out `kangax/compat-table` and prepare a pull request to describe
support in the new version of Rhino. First, add a new environment for the
version to `environments.json` like so:

```json
"rhino1_7_14": {
  "full": "Rhino 1.7.14",
  "short": "Rhino 1.7.14",
  "family": "Rhino",
  "platformtype": "engine",
  "release": "2022-01-06",
  "obsolete": true
},
```

Copy the most recent rhino JAR into the directory as `rhino.jar`. Next, run
`rhino.js`. This will produce a number of messages indicating that failing
tests now pass, or that new tests now have recorded results:

    **** data-es2016plus ****

    data-es2016plus -> Error.cause property -> AggregateError.prototype lacks cause: test result out of date, res: true, actual: false

Manually update each indicated `data-*.js` file with updated test data as
indicated. For example, a test that failed in 1.7.13 and passed in 1.7.14
would have a `res` section with the following diff:

```javascript
   res: {
     babel6corejs2: false,
     babel7corejs3: babel.corejs,
     /* ... */
     rhino1_7_13: false,
+    rhino1_7_14: true,
   }
```

Rerun `rhino.js` and verify it produces no output. Then `npm run build` and
submit your pull request.

## Update Babel

Once the `compat-table` changes are merged, check out `babel/babel` and prepare
a pull request to enable support for `babel-preset-env` in the new release.

* Update `COMPAT_TABLE_COMMIT` in `packages/babel-compat-data/scripts/download-compat-table.sh`
to correspond to the merge commit in `compat-table`.
* Run `make build-compat-data && make bootstrap && OVERWRITE=true yarn jest`.

Then submit the resulting patch as a pull request to Babel.

## Update core-js-compat

Compatibility data for `core-js`, the `babel` polyfill engine, also needs to
be updated.

* Check out `zloirock/core-js` and run `npm install && npm run build-compat`.
* Define the actual Rhino version in the `compat-rhino-prepare` task in `package.json`.
* Run `npm run compat-rhino` and you will see the results of tests in the console.
* Much like in `compat-table`, edit `packages/core-js-compat/src/data.mjs` to add a line
`rhino: 1.7.[XX]` for any newly-passing test labeled as "not required".
* Submit a pull request with changes.

## Prepare for Next Release

Now it's time to move to the next "SNAPSHOT" release. Update gradle.properties,
create a PR, and push the new PR. Now development can proceeed anew!

