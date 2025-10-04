package org.mozilla.javascript.tests.type_info.test_object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * members are accessed via beaning
 *
 * @author ZZZank
 */
@SuppressWarnings("unused")
public class MethodBasedBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Integer> integers = new ArrayList<>();
    private List<Double> doubles = new ArrayList<>();
    private List<Number> numbers = new ArrayList<>();
    private Map<String, String> stringStringMap = new HashMap<>();
    private Map<Integer, String> intStringMap = new HashMap<>();
    private Map<Integer, Integer> intIntMap = new HashMap<>();
    private Map<Integer, Long> intLongMap = new HashMap<>();
    // beans with typeInfo in the static type
    private GenericBean<Integer> intBean1 = new GenericBean<>();
    private GenericBean<Double> dblBean1 = new GenericBean<>();
    // beans with typeInfo in the dynamic type
    private GenericBean<Integer> intBean2 = new IntegerGenericBean();
    private GenericBean<Double> dblBean2 = new DoubleGenericBean();

    public List<Double> getDoubles() {
        return doubles;
    }

    public void setDoubles(List<Double> doubles) {
        this.doubles = doubles;
    }

    public List<Integer> getIntegers() {
        return integers;
    }

    public void setIntegers(List<Integer> integers) {
        this.integers = integers;
    }

    public List<Number> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Number> numbers) {
        this.numbers = numbers;
    }

    public Map<String, String> getStringStringMap() {
        return stringStringMap;
    }

    public void setStringStringMap(Map<String, String> stringStringMap) {
        this.stringStringMap = stringStringMap;
    }

    public Map<Integer, String> getIntStringMap() {
        return intStringMap;
    }

    public void setIntStringMap(Map<Integer, String> intStringMap) {
        this.intStringMap = intStringMap;
    }

    public Map<Integer, Integer> getIntIntMap() {
        return intIntMap;
    }

    public void setIntIntMap(Map<Integer, Integer> intIntMap) {
        this.intIntMap = intIntMap;
    }

    public Map<Integer, Long> getIntLongMap() {
        return intLongMap;
    }

    public void setIntLongMap(Map<Integer, Long> intLongMap) {
        this.intLongMap = intLongMap;
    }

    public GenericBean<Integer> getIntBean1() {
        return intBean1;
    }

    public void setIntBean1(GenericBean<Integer> intBean1) {
        this.intBean1 = intBean1;
    }

    public GenericBean<Double> getDblBean1() {
        return dblBean1;
    }

    public void setDblBean1(GenericBean<Double> dblBean1) {
        this.dblBean1 = dblBean1;
    }

    public GenericBean<Integer> getIntBean2() {
        return intBean2;
    }

    public void setIntBean2(GenericBean<Integer> intBean2) {
        this.intBean2 = intBean2;
    }

    public GenericBean<Double> getDblBean2() {
        return dblBean2;
    }

    public void setDblBean2(GenericBean<Double> dblBean2) {
        this.dblBean2 = dblBean2;
    }
}
