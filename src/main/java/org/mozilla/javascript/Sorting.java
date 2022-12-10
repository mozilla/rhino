/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Comparator;

public final class Sorting {
    private static final int SMALLSORT = 16;

    private static final Sorting sorting = new Sorting();

    private Sorting() {}

    public static Sorting get() {
        return sorting;
    }

    public void insertionSort(Object[] a, Comparator<Object> cmp) {
        insertionSort(a, 0, a.length - 1, cmp);
    }

    private static void insertionSort(Object[] a, int start, int end, Comparator<Object> cmp) {
        int i = start;
        while (i <= end) {
            Object x = a[i];
            int j = i - 1;
            while ((j >= start) && (cmp.compare(a[j], x) > 0)) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
            i++;
        }
    }

    /*
    Hybrid sorting mechanism similar to Introsort by David Musser. Uses quicksort's
    partitioning mechanism recursively until the resulting array is small or the
    recursion is too deep, and then use insertion sort for the rest.
    This is the same basic algorithm used by the GNU Standard C++ library.
    */
    public void hybridSort(Object[] a, Comparator<Object> cmp) {
        hybridSort(a, 0, a.length - 1, cmp, log2(a.length) * 2);
    }

    private void hybridSort(Object[] a, int start, int end, Comparator<Object> cmp, int maxdepth) {
        if (start < end) {
            if ((maxdepth == 0) || ((end - start) <= SMALLSORT)) {
                insertionSort(a, start, end, cmp);
            } else {
                int p = partition(a, start, end, cmp);
                hybridSort(a, start, p, cmp, maxdepth - 1);
                hybridSort(a, p + 1, end, cmp, maxdepth - 1);
            }
        }
    }

    /*
    Quicksort-style partitioning, using the Hoare partition scheme as coded by
    Sedgewick at https://algs4.cs.princeton.edu/23quicksort/Quick.java.html.
    Use the "median of three" method to determine which index to pivot on, and then
    separate the array into two halves based on the pivot.
    */
    private int partition(Object[] a, int start, int end, Comparator<Object> cmp) {
        final int p = median(a, start, end, cmp);
        final Object pivot = a[p];
        a[p] = a[start];
        a[start] = pivot;

        int i = start;
        int j = end + 1;

        while (true) {
            while (cmp.compare(a[++i], pivot) < 0) {
                if (i == end) {
                    break;
                }
            }
            while (cmp.compare(a[--j], pivot) >= 0) {
                if (j == start) {
                    break;
                }
            }
            if (i >= j) {
                break;
            }
            swap(a, i, j);
        }

        swap(a, start, j);
        return j;
    }

    private static void swap(Object[] a, int l, int h) {
        final Object tmp = a[l];
        a[l] = a[h];
        a[h] = tmp;
    }

    private static int log2(int n) {
        return (int) (Math.log10(n) / Math.log10(2.0));
    }

    /*
    Return the index of the median of three elements in the specified array range -- the
    first, the last, and the one in the middle.
    */
    public int median(final Object[] a, int start, int end, Comparator<Object> cmp) {
        final int m = start + ((end - start) / 2);
        int smallest = start;

        if (cmp.compare(a[smallest], a[m]) > 0) {
            smallest = m;
        }
        if (cmp.compare(a[smallest], a[end]) > 0) {
            smallest = end;
        }

        if (smallest == start) {
            return (cmp.compare(a[m], a[end]) < 0) ? m : end;
        }
        if (smallest == m) {
            return (cmp.compare(a[start], a[end]) < 0) ? start : end;
        }
        return (cmp.compare(a[start], a[m]) < 0) ? start : m;
    }
}
