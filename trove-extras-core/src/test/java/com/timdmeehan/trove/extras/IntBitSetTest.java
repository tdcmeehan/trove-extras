package com.timdmeehan.trove.extras;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by tmeehan on 5/26/16.
 */
public class IntBitSetTest {

    @Test
    public void testGetNoEntryValue() throws Exception {
        assertEquals(0, new IntBitSet().getNoEntryValue());
    }

    @Test
    public void testSize() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(4);
        intBitSet.add(5);
        intBitSet.add(Integer.MAX_VALUE);

        assertEquals(5, intBitSet.size());

        intBitSet.remove(Integer.MAX_VALUE);
        assertEquals(4, intBitSet.size());

        intBitSet.remove(3);
        assertEquals(4, intBitSet.size());
        intBitSet.remove(4);
        assertEquals(3, intBitSet.size());
    }

    @Test
    public void testIsEmpty() throws Exception {
        IntBitSet intBitSet = new IntBitSet();

        assertTrue(intBitSet.isEmpty());

        intBitSet.add(1);
        intBitSet.add(2);
        assertFalse(intBitSet.isEmpty());
        intBitSet.remove(2);
        assertFalse(intBitSet.isEmpty());
        intBitSet.remove(1);
        assertTrue(intBitSet.isEmpty());
    }

    @Test
    public void testContains() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        assertFalse(intBitSet.contains(1));

        intBitSet.add(1);
        assertTrue(intBitSet.contains(1));

        intBitSet.remove(1);
        assertFalse(intBitSet.contains(1));
    }

    @Test
    public void testIterator() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(1000);
        intBitSet.add(100000);
        intBitSet.add(10000000);
        TIntIterator iterator = intBitSet.iterator();
        Set<Integer> resultSet = Sets.newHashSet();
        while(iterator.hasNext()) {
            resultSet.add(iterator.next());
        }
        assertEquals(Sets.newHashSet(1, 1000, 100000, 10000000), resultSet);
    }

    @Test
    public void testToArray() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(1000);
        intBitSet.add(100000);
        intBitSet.add(10000000);

        int[] arr = intBitSet.toArray();
        Arrays.sort(arr);
        assertArrayEquals(new int[]{1, 1000, 100000, 10000000}, arr);
    }

    @Test
    public void testToArray1() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(1000);

        int[] arr = new int[4];
        arr = intBitSet.toArray(arr);
        Arrays.sort(arr);
        assertArrayEquals(new int[]{0, 0, 1, 1000}, arr);
    }

    @Test
    public void testAdd() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        assertTrue(intBitSet.add(2));

        assertEquals(1, intBitSet.size());
        assertTrue(intBitSet.contains(2));
        assertFalse(intBitSet.contains(1));
    }

    @Test
    public void testAddNegativeValue() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        assertTrue(intBitSet.add(2));
        assertTrue(intBitSet.add(-2));

        assertEquals(2, intBitSet.size());
        assertTrue(intBitSet.contains(2));
        assertTrue(intBitSet.contains(-2));
        assertFalse(intBitSet.contains(1));
        assertFalse(intBitSet.contains(-1));
    }

    @Test
    public void testAddDupe() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        assertTrue(intBitSet.add(2));
        assertFalse(intBitSet.add(2));

        assertEquals(1, intBitSet.size());
        assertTrue(intBitSet.contains(2));
        assertFalse(intBitSet.contains(1));
    }

    @Test
    public void testAddCornerCases() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(Integer.MIN_VALUE);
        intBitSet.add(Integer.MAX_VALUE);
        intBitSet.add(0);

        assertEquals(3, intBitSet.size());
        assertTrue(intBitSet.contains(Integer.MIN_VALUE));
        assertTrue(intBitSet.contains(Integer.MAX_VALUE));
        assertTrue(intBitSet.contains(0));
        assertFalse(intBitSet.contains(1));
    }

    @Test
    public void testRemove() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(2);
        intBitSet.add(3);

        assertEquals(2, intBitSet.size());
        assertTrue(intBitSet.remove(2));
        assertEquals(1, intBitSet.size());
        assertFalse(intBitSet.remove(2));
    }

    @Test
    public void testContainsAll() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(4);
        intBitSet.add(5);
        intBitSet.add(Integer.MAX_VALUE);

        assertTrue(intBitSet.containsAll(Lists.newArrayList(1, 2, 4, 5)));
        assertTrue(intBitSet.containsAll(Lists.newArrayList(1, 2, 5)));
        assertFalse(intBitSet.containsAll(Lists.newArrayList(1, 2, 3, 5)));
    }

    @Test
    public void testContainsAllTCollection() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(4);
        intBitSet.add(5);
        intBitSet.add(Integer.MAX_VALUE);

        assertTrue(intBitSet.containsAll(new TIntArrayList(new int[]{1, 2, 4, 5})));
        assertTrue(intBitSet.containsAll(new TIntArrayList(new int[]{1, 2, 5})));
        assertFalse(intBitSet.containsAll(new TIntArrayList(new int[]{1, 2, 3, 5})));
    }

    @Test
    public void testContainsAllArray() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(4);
        intBitSet.add(5);
        intBitSet.add(Integer.MAX_VALUE);

        assertTrue(intBitSet.containsAll(new int[]{1, 2, 4, 5}));
        assertTrue(intBitSet.containsAll(new int[]{1, 2, 5}));
        assertFalse(intBitSet.containsAll(new int[]{1, 2, 3, 5}));
    }

    @Test
    public void testAddAll() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);

        assertTrue(intBitSet.addAll(Lists.newArrayList(1, 2, 3)));
        assertTrue(intBitSet.contains(3));
        assertEquals(3, intBitSet.size());

        assertFalse(intBitSet.addAll(Lists.newArrayList(1, 2, 3)));
        assertTrue(intBitSet.contains(3));
        assertEquals(3, intBitSet.size());
    }

    @Test
    public void testAddAllTCollection() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);

        assertTrue(intBitSet.addAll(new TIntArrayList(new int[]{1, 2, 3})));
        assertTrue(intBitSet.contains(3));
        assertEquals(3, intBitSet.size());

        assertFalse(intBitSet.addAll(new TIntArrayList(new int[]{1, 2, 3})));
        assertTrue(intBitSet.contains(3));
        assertEquals(3, intBitSet.size());
    }

    @Test
    public void testAddAllArray() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);

        assertTrue(intBitSet.addAll(new int[]{1, 2, 3}));
        assertTrue(intBitSet.contains(3));
        assertEquals(3, intBitSet.size());

        assertFalse(intBitSet.addAll(new int[]{1, 2, 3}));
        assertTrue(intBitSet.contains(3));
        assertEquals(3, intBitSet.size());
    }

    @Test
    public void testRetainAll() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);

        assertTrue(intBitSet.retainAll(Lists.newArrayList(1, 2)));
        assertTrue(intBitSet.contains(1));
        assertTrue(intBitSet.contains(2));
        assertEquals(2, intBitSet.size());

        assertFalse(intBitSet.retainAll(Lists.newArrayList(1, 2)));
        assertEquals(2, intBitSet.size());
    }

    @Test
    public void testRetainAllArray() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);

        assertTrue(intBitSet.retainAll(new int[]{1, 2}));
        assertTrue(intBitSet.contains(1));
        assertTrue(intBitSet.contains(2));
        assertEquals(2, intBitSet.size());

        assertFalse(intBitSet.retainAll(new int[]{1, 2}));
        assertEquals(2, intBitSet.size());
    }

    @Test
    public void testRetainAllTCollection() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);

        assertTrue(intBitSet.retainAll(new TIntArrayList(new int[]{1, 2})));
        assertTrue(intBitSet.contains(1));
        assertTrue(intBitSet.contains(2));
        assertEquals(2, intBitSet.size());

        assertFalse(intBitSet.retainAll(new TIntArrayList(new int[]{1, 2})));
        assertEquals(2, intBitSet.size());
    }

    @Test
    public void testRemoveAll() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);
        intBitSet.add(4);

        assertTrue(intBitSet.removeAll(Lists.newArrayList(3, 4)));
        assertTrue(intBitSet.contains(1));
        assertTrue(intBitSet.contains(2));
        assertEquals(2, intBitSet.size());

        assertFalse(intBitSet.removeAll(Lists.newArrayList(3, 4)));
        assertEquals(2, intBitSet.size());
    }

    @Test
    public void testRemoveAllArray() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);
        intBitSet.add(4);

        assertTrue(intBitSet.removeAll(new int[]{3, 4}));
        assertTrue(intBitSet.contains(1));
        assertTrue(intBitSet.contains(2));
        assertEquals(2, intBitSet.size());

        assertFalse(intBitSet.removeAll(new int[]{3, 4}));
        assertEquals(2, intBitSet.size());
    }

    @Test
    public void testRemoveAllTCollection() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);
        intBitSet.add(4);

        assertTrue(intBitSet.removeAll(new TIntArrayList(new int[]{3, 4})));
        assertTrue(intBitSet.contains(1));
        assertTrue(intBitSet.contains(2));
        assertEquals(2, intBitSet.size());

        assertFalse(intBitSet.removeAll(new TIntArrayList(new int[]{3, 4})));
        assertEquals(2, intBitSet.size());
    }

    @Test
    public void testClear() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);
        intBitSet.add(4);

        assertEquals(4, intBitSet.size());
        intBitSet.clear();
        assertEquals(0, intBitSet.size());
        assertTrue(intBitSet.isEmpty());
    }

    @Test
    public void testForEachTrue() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);
        intBitSet.add(4);

        final Set<Integer> seenInts = Sets.newHashSet();
        assertTrue(intBitSet.forEach(value -> {
            seenInts.add(value);
            return true;
        }));
        assertEquals(4, seenInts.size());
    }

    @Test
    public void testForEachSomeFalse() throws Exception {
        IntBitSet intBitSet = new IntBitSet();
        intBitSet.add(1);
        intBitSet.add(2);
        intBitSet.add(3);
        intBitSet.add(4);

        final Set<Integer> seenInts = Sets.newHashSet();
        assertFalse(intBitSet.forEach(value -> {
            seenInts.add(value);
            return value % 2 == 0;
        }));
        assertEquals(4, seenInts.size());
    }

    @Test
    public void testCountLinesUpWithIteration() throws Exception {
        int setSize = 1000000;
        TIntSet intSet = new IntBitSet();
        for (int i = 0; i < setSize; i++) {
            intSet.add(i);
        }
        assertEquals(setSize, intSet.size());

        TIntIterator tIntIterator = intSet.iterator();
        int count = 0;
        while (tIntIterator.hasNext()) {
            count++;
            tIntIterator.next();
        }
        assertEquals(setSize, count);

        intSet.clear();
        assertEquals(0, intSet.size());
    }
}