package org.mozilla.javascript.tests.type_info.test_object;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZZZank
 */
public class DoubleArrayList extends ArrayList<Double> {
    private static final long serialVersionUID = 1L;

    public static List<Double> createTestObject() {
        List<Double> list = new DoubleArrayList();

        list.add(42.5);
        list.add(7.5);
        return list;
    }
}
