package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author ZZZank
 */
public class JavaObjectWrappingTest {
    private static final String PATH = "Packages." + Interf.class.getName();

    @Test
    public void list() {
        // methods exposed via static type should be accessible
        Utils.assertWithAllModes(1, PATH + ".listBased().size()");
        // values inside list should not be leaked, if static type is not a list
        Utils.assertEvaluatorExceptionES6(
            String.format("Java class \"%s\" has no public instance field or method named", ListBasedImpl.class.getName()),
            PATH + ".listBased()[0]"
        );
        Utils.assertWithAllModes(Undefined.instance, PATH + ".listBased().length");
    }

    @Test
    public void map() {
        // methods exposed via static type should be accessible
        Utils.assertWithAllModes(1, PATH + ".mapBased().size()");
        // values inside map should not be leaked, if static type is not a map
        Utils.assertWithAllModes(Undefined.instance, PATH + ".mapBased()['inner']");
        // even when related feature is enabled
        Utils.assertWithAllModes(
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS) {
                        return true;
                    }
                    return super.hasFeature(cx, featureIndex);
                }
            },
            -1,
            null,
            Undefined.instance,
            PATH + ".mapBased().inner"
        );
    }

    public interface Interf {
        static Interf listBased() {
            return new ListBasedImpl();
        }

        static Interf mapBased() {
            return new MapBasedImpl();
        }

        /**
         * the only method exposed by this interface, accessing any other name SHOULD return {@code undefined}
         */
        int size();
    }

    static class ListBasedImpl extends ArrayList<Double> implements Interf {

        public ListBasedImpl() {
            super();
            add(1.23);
        }
    }

    static class MapBasedImpl extends HashMap<String, Double> implements Interf {

        public MapBasedImpl() {
            super();
            this.put("inner", 3.0);
        }
    }
}
