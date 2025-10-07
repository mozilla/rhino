package org.mozilla.javascript.tests.type_info.test_object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * members are accessed via field access
 *
 * @author ZZZank
 */
@SuppressWarnings("unused")
public class FieldBasedBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Integer> integers = new ArrayList<>();
    private List<Double> doubles = new ArrayList<>();

    public List<Double> getDoubles() {
        return doubles;
    }

    public List<Number> numbers = new ArrayList<>();

    public Map<String, String> stringStringMap = new HashMap<>();
    public Map<Integer, String> intStringMap = new HashMap<>();
    public Map<Integer, Integer> intIntMap = new HashMap<>();
    public Map<Integer, Long> intLongMap = new HashMap<>();
    // beans with typeInfo in the static type
    public GenericBean<Integer> intBean1 = new GenericBean<>();
    public GenericBean<Double> dblBean1 = new GenericBean<>();
    // beans with typeInfo in the dynamic type
    public GenericBean<Integer> intBean2 = new IntegerGenericBean();
    public GenericBean<Double> dblBean2 = new DoubleGenericBean();
}
