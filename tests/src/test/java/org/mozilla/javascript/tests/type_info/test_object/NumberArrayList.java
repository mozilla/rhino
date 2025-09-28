package org.mozilla.javascript.tests.type_info.test_object;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZZZank
 */
public class NumberArrayList extends ArrayList<Number> {
    private static final long serialVersionUID = 1L;

    public static List<Number> createTestObject() {
        List<Number> list = new NumberArrayList();

        list.add(42);
        list.add(7.5);
        return list;
    }
}
