package org.mozilla.javascript.tests.type_info.test_object;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ZZZank
 */
public class IntegerArrayList extends ArrayList<Integer> {
    @Serial private static final long serialVersionUID = 1L;

    public static List<Integer> createTestObject() {
        List<Integer> list = new IntegerArrayList();

        list.add(42);
        list.add(7);
        return list;
    }
}
