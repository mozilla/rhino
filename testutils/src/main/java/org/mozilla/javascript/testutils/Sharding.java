package org.mozilla.javascript.testutils;

/** This class interprets Bazel test sharding parameters for us. */
public class Sharding {
    /**
     * If TEST_TOTAL_SHARDS is set in the environment, return the total number of shards and the
     * shard index. OTherwise return null.
     */
    public static Shards getSharding() {
        String total = System.getenv("NUM_TEST_SHARDS");
        String index = System.getenv("SHARD_INDEX");
        try {
            var ret = new Shards();
            ret.total = Integer.parseInt(total);
            ret.index = Integer.parseInt(index);
            return ret;
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    public static final class Shards {
        public int total;
        public int index;

        @Override
        public String toString() {
            return "{shard " + index + " of " + total + "}";
        }
    }
}
