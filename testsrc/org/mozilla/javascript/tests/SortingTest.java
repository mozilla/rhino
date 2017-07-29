package org.mozilla.javascript.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Sorting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static org.junit.Assert.*;

public class SortingTest {
    private static final int BIG_ARRAY = 100000;
    private static final int ITERATIONS = 4;

    private static final Random rand = new Random();

    private static Object[] bigRandom;

    @BeforeClass
    public static void init()
    {
        bigRandom = randomArray(BIG_ARRAY);
    }

    private void insertionSort(Object[] expected)
    {
        Object[] after = Arrays.copyOf(expected, expected.length);
        Sorting.insertionSort(after, new IntComparator());
        Arrays.sort(expected, new IntComparator());
        assertArrayEquals(expected, after);
    }

    @Test
    public void testInsertionSort()
    {
        insertionSort(forwardArray(100));
        insertionSort(reverseArray(100));
        insertionSort(randomArray(100));
        insertionSort(sameArray(100));
        insertionSort(new Object[] {});
        insertionSort(randomArray(10000));
    }

    private void hybridSort(Object[] expected)
    {
        Object[] after = Arrays.copyOf(expected, expected.length);
        Sorting.hybridSort(after, new IntComparator());
        Arrays.sort(expected, new IntComparator());
        assertArrayEquals(expected, after);
    }

    @Test
    public void testHybridSort()
    {
        hybridSort(randomArray(10));
        hybridSort(forwardArray(100));
        hybridSort(reverseArray(100));
        hybridSort(randomArray(100));
        hybridSort(sameArray(100));
        hybridSort(new Object[] {});
        hybridSort(randomArray(10000));
    }

    /*
    @Test
    public void testBenchInsertionRandom()
    {
        Object[] a = Arrays.copyOf(bigRandom, bigRandom.length);
        Sorting.insertionSort(a, new IntComparator());
    }
    */

    @Test
    public void testBenchRandomHybrid()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = Arrays.copyOf(bigRandom, bigRandom.length);
            Sorting.hybridSort(a, new IntComparator());
        }
    }

    @Test
    public void testBenchRandomJavaUtil()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = Arrays.copyOf(bigRandom, bigRandom.length);
            Arrays.sort(a, new IntComparator());
        }
    }

    /*
    @Test
    public void testBenchInsertionReverse()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = reverseArray(BIG_ARRAY);
            Sorting.insertionSort(a, new IntComparator());
        }
    }
    */

    @Test
    public void testBenchReverseHybrid()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = reverseArray(BIG_ARRAY);
            Sorting.hybridSort(a, new IntComparator());
        }
    }

    @Test
    public void testBenchReverseJavaUtil()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = reverseArray(BIG_ARRAY);
            Arrays.sort(a, new IntComparator());
        }
    }

    @Test
    public void testBenchSequentialInsertion()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = forwardArray(BIG_ARRAY);
            Sorting.insertionSort(a, new IntComparator());
        }
    }

    @Test
    public void testBenchSequentialHybrid()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = forwardArray(BIG_ARRAY);
            Sorting.hybridSort(a, new IntComparator());
        }
    }

    @Test
    public void testBenchSequentialJavaUtil()
    {
        for (int i = 0; i < ITERATIONS; i++) {
            Object[] a = forwardArray(BIG_ARRAY);
            Arrays.sort(a, new IntComparator());
        }
    }

    private static Integer[] forwardArray(int length)
    {
        Integer[] a = new Integer[length];
        for (int i = 0; i < length; i++) {
            a[i] = i;
        }
        return a;
    }

    private static Integer[] reverseArray(int length)
    {
        Integer[] a = new Integer[length];
        for (int i = 0; i < length; i++) {
            a[i] = length - i - 1;
        }
        return a;
    }

    private static Integer[] randomArray(int length)
    {
        Integer[] a = new Integer[length];
        for (int i = 0; i < length; i++) {
            a[i] = rand.nextInt();
        }
        return a;
    }

    private static Integer[] sameArray(int length)
    {
        Integer[] a = new Integer[length];
        Arrays.fill(a, 1);
        return a;
    }

    private final class IntComparator
        implements Comparator<Object>
    {
        @Override
        public int compare(Object a, Object b) {
            Integer ia = (Integer)a;
            Integer ib = (Integer)b;
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
