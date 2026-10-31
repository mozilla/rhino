load("@contrib_rules_jvm//java:defs.bzl", "java_junit5_test")

# This rule creates a test that runs in multiple shards, with
# two environment variables that the test runner can use to
# determine how sharding should work. It returns the names of the
# generated shard targets, which can be used to build a test_suite
# (with the base name) that runs all of the shards at once.
def sharded_parameterized_test(name, shard_count, **kwargs):
    """Generates multiple test targets to simulate manual sharding."""
    tags = kwargs.pop("tags", [])
    shards = []
    for i in range(shard_count):
        java_junit5_test(
            name = "%s_shard_%d" % (name, i),
            # Set environment variables. These should NOT have the same names
            # as Bazel's default sharding variables or the tests will not run.
            env = {
                "NUM_TEST_SHARDS": "%d" % shard_count,
                "SHARD_INDEX": "%d" % i,
            },
            tags = tags,
            **kwargs
        )
        shards.append("%s_shard_%d" % (name, i))
    return shards
