package org.mozilla.javascript.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Sorting;

public class SortingTest {
    private static final int BIG_ARRAY = 100000;
    private static final int ITERATIONS = 1;

    private static final Random rand = new Random();

    private static Object[] bigRandom;
    private static Sorting sorter;

    @BeforeClass
    public static void init() {
        bigRandom = randomArray(BIG_ARRAY);
        sorter = Sorting.get();
    }

    private void insertionSort(Object[] expected) {
        Object[] after = Arrays.copyOf(expected, expected.length);
        sorter.insertionSort(after, new IntComparator());
        Arrays.sort(expected, new IntComparator());
        assertArrayEquals(expected, after);
    }

    @Test
    public void insertionSort() {
        insertionSort(forwardArray(100));
        insertionSort(reverseArray(100));
        insertionSort(randomArray(100));
        insertionSort(sameArray(100));
        insertionSort(new Object[] {});
        insertionSort(randomArray(10000));
    }

    private void hybridSort(Object[] expected) {
        Object[] after = Arrays.copyOf(expected, expected.length);
        sorter.hybridSort(after, new IntComparator());
        Arrays.sort(expected, new IntComparator());
        assertArrayEquals(expected, after);
    }

    @Test
    public void hybridSort() {
        hybridSort(randomArray(10));
        hybridSort(forwardArray(100));
        hybridSort(reverseArray(100));
        hybridSort(randomArray(100));
        hybridSort(sameArray(100));
        hybridSort(new Object[] {});
        hybridSort(randomArray(10000));
    }

    @Test
    public void median() {
        Object[] a = new Object[] {1, 2, 3, 4, 5};
        assertEquals(2, sorter.median(a, 0, 4, new IntComparator()));
        a = new Object[] {5, 4, 3, 2, 1};
        assertEquals(2, sorter.median(a, 0, 4, new IntComparator()));
        a = new Object[] {3, 4, 5, 2, 1};
        assertEquals(0, sorter.median(a, 0, 4, new IntComparator()));
        a = new Object[] {4, 5, 1, 2, 3};
        assertEquals(4, sorter.median(a, 0, 4, new IntComparator()));
    }

    /*
    @Test
    public void nenchInsertionRandom()
    {
        Object[] a = Arrays.copyOf(bigRandom, bigRandom.length);
        Sorting.insertionSort(a, new IntComparator());
    }
    */

    @Test
    public void benchRandomHybrid() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = Arrays.copyOf(bigRandom, bigRandom.length);
            sorter.hybridSort(a, new IntComparator());
        }
    }

    @Test
    public void benchRandomJavaUtil() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = Arrays.copyOf(bigRandom, bigRandom.length);
            Arrays.sort(a, new IntComparator());
        }
    }

    /*
    @Test
    public void benchInsertionReverse()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = reverseArray(BIG_ARRAY);
            Sorting.insertionSort(a, new IntComparator());
        }
    }
    */

    @Test
    public void benchReverseHybrid() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = reverseArray(BIG_ARRAY);
            sorter.hybridSort(a, new IntComparator());
        }
    }

    @Test
    public void benchReverseJavaUtil() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = reverseArray(BIG_ARRAY);
            Arrays.sort(a, new IntComparator());
        }
    }

    @Test
    public void benchSequentialInsertion() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = forwardArray(BIG_ARRAY);
            sorter.insertionSort(a, new IntComparator());
        }
    }

    @Test
    public void benchSequentialHybrid() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = forwardArray(BIG_ARRAY);
            sorter.hybridSort(a, new IntComparator());
        }
    }

    @Test
    public void benchSequentialJavaUtil() {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = forwardArray(BIG_ARRAY);
            Arrays.sort(a, new IntComparator());
        }
    }

    private static Integer[] forwardArray(int length) {
        Integer[] a = new Integer[length];
        for (int i = 0; i < length; i++) {
            a[i] = i;
        }
        return a;
    }

    private static Integer[] reverseArray(int length) {
        Integer[] a = new Integer[length];
        for (int i = 0; i < length; i++) {
            a[i] = length - i - 1;
        }
        return a;
    }

    private static Integer[] randomArray(int length) {
        Integer[] a = new Integer[length];
        for (int i = 0; i < length; i++) {
            a[i] = rand.nextInt();
        }
        return a;
    }

    private static Integer[] sameArray(int length) {
        Integer[] a = new Integer[length];
        Arrays.fill(a, 1);
        return a;
    }

    private final class IntComparator implements Comparator<Object> {
        @Override
        public int compare(Object a, Object b) {
            Integer ia = (Integer) a;
            Integer ib = (Integer) b;
            if (ia < ib) {
                return -1;
            }
            if (ia > ib) {
                return 1;
            }
            return 0;
        }
    }
}
