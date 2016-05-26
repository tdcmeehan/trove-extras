package com.timdmeehan.trove.extras;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by tmeehan on 5/26/16.
 */
public class LongBitSetTest {

    @Test
    public void testGetNoEntryValue() throws Exception {
        assertEquals(0, new LongBitSet().getNoEntryValue());
    }

    @Test
    public void testSize() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(4);
        longBitSet.add(5);
        longBitSet.add(Integer.MAX_VALUE);

        assertEquals(5, longBitSet.size());

        longBitSet.remove(Integer.MAX_VALUE);
        assertEquals(4, longBitSet.size());

        longBitSet.remove(3);
        assertEquals(4, longBitSet.size());
        longBitSet.remove(4);
        assertEquals(3, longBitSet.size());
    }

    @Test
    public void testIsEmpty() throws Exception {
        LongBitSet longBitSet = new LongBitSet();

        assertTrue(longBitSet.isEmpty());

        longBitSet.add(1);
        longBitSet.add(2);
        assertFalse(longBitSet.isEmpty());
        longBitSet.remove(2);
        assertFalse(longBitSet.isEmpty());
        longBitSet.remove(1);
        assertTrue(longBitSet.isEmpty());
    }

    @Test
    public void testContains() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        assertFalse(longBitSet.contains(1));

        longBitSet.add(1);
        assertTrue(longBitSet.contains(1));

        longBitSet.remove(1);
        assertFalse(longBitSet.contains(1));
    }

    @Test
    public void testIterator() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(1000);
        longBitSet.add(100000);
        longBitSet.add(10000000);
        TLongIterator iterator = longBitSet.iterator();
        Set<Long> resultSet = Sets.newHashSet();
        while(iterator.hasNext()) {
            resultSet.add(iterator.next());
        }
        assertEquals(Sets.newHashSet(1l, 1000l, 100000l, 10000000l), resultSet);
    }

    @Test
    public void testToArray() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(1000);
        longBitSet.add(100000);
        longBitSet.add(10000000);

        long[] arr = longBitSet.toArray();
        Arrays.sort(arr);
        assertArrayEquals(new long[]{1, 1000, 100000, 10000000}, arr);
    }

    @Test
    public void testToArray1() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(1000);

        long[] arr = new long[4];
        arr = longBitSet.toArray(arr);
        Arrays.sort(arr);
        assertArrayEquals(new long[]{0, 0, 1, 1000}, arr);
    }

    @Test
    public void testAdd() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        assertTrue(longBitSet.add(2));

        assertEquals(1, longBitSet.size());
        assertTrue(longBitSet.contains(2));
        assertFalse(longBitSet.contains(1));
    }

    @Test
    public void testAddNegativeValue() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        assertTrue(longBitSet.add(2));
        assertTrue(longBitSet.add(-2));

        assertEquals(2, longBitSet.size());
        assertTrue(longBitSet.contains(2));
        assertTrue(longBitSet.contains(-2));
        assertFalse(longBitSet.contains(1));
        assertFalse(longBitSet.contains(-1));
    }

    @Test
    public void testAddDupe() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        assertTrue(longBitSet.add(2));
        assertFalse(longBitSet.add(2));

        assertEquals(1, longBitSet.size());
        assertTrue(longBitSet.contains(2));
        assertFalse(longBitSet.contains(1));
    }

    @Test
    public void testAddCornerCases() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(Integer.MIN_VALUE);
        longBitSet.add(Integer.MAX_VALUE);
        longBitSet.add(0);

        assertEquals(3, longBitSet.size());
        assertTrue(longBitSet.contains(Integer.MIN_VALUE));
        assertTrue(longBitSet.contains(Integer.MAX_VALUE));
        assertTrue(longBitSet.contains(0));
        assertFalse(longBitSet.contains(1));
    }

    @Test
    public void testRemove() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(2);
        longBitSet.add(3);

        assertEquals(2, longBitSet.size());
        assertTrue(longBitSet.remove(2));
        assertEquals(1, longBitSet.size());
        assertFalse(longBitSet.remove(2));
    }

    @Test
    public void testContainsAll() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(4);
        longBitSet.add(5);
        longBitSet.add(Integer.MAX_VALUE);

        assertTrue(longBitSet.containsAll(Lists.newArrayList(1, 2, 4, 5)));
        assertTrue(longBitSet.containsAll(Lists.newArrayList(1, 2, 5)));
        assertFalse(longBitSet.containsAll(Lists.newArrayList(1, 2, 3, 5)));
    }

    @Test
    public void testContainsAllTCollection() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(4);
        longBitSet.add(5);
        longBitSet.add(Integer.MAX_VALUE);

        assertTrue(longBitSet.containsAll(new TLongArrayList(new long[]{1, 2, 4, 5})));
        assertTrue(longBitSet.containsAll(new TLongArrayList(new long[]{1, 2, 5})));
        assertFalse(longBitSet.containsAll(new TLongArrayList(new long[]{1, 2, 3, 5})));
    }

    @Test
    public void testContainsAllArray() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(4);
        longBitSet.add(5);
        longBitSet.add(Integer.MAX_VALUE);

        assertTrue(longBitSet.containsAll(new long[]{1, 2, 4, 5}));
        assertTrue(longBitSet.containsAll(new long[]{1, 2, 5}));
        assertFalse(longBitSet.containsAll(new long[]{1, 2, 3, 5}));
    }

    @Test
    public void testAddAll() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);

        assertTrue(longBitSet.addAll(Lists.newArrayList(1l, 2l, 3l)));
        assertTrue(longBitSet.contains(3));
        assertEquals(3, longBitSet.size());

        assertFalse(longBitSet.addAll(Lists.newArrayList(1l, 2l, 3l)));
        assertTrue(longBitSet.contains(3));
        assertEquals(3, longBitSet.size());
    }

    @Test
    public void testAddAllTCollection() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);

        assertTrue(longBitSet.addAll(new TLongArrayList(new long[]{1, 2, 3})));
        assertTrue(longBitSet.contains(3));
        assertEquals(3, longBitSet.size());

        assertFalse(longBitSet.addAll(new TLongArrayList(new long[]{1, 2, 3})));
        assertTrue(longBitSet.contains(3));
        assertEquals(3, longBitSet.size());
    }

    @Test
    public void testAddAllArray() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);

        assertTrue(longBitSet.addAll(new long[]{1, 2, 3}));
        assertTrue(longBitSet.contains(3));
        assertEquals(3, longBitSet.size());

        assertFalse(longBitSet.addAll(new long[]{1, 2, 3}));
        assertTrue(longBitSet.contains(3));
        assertEquals(3, longBitSet.size());
    }

    @Test
    public void testRetainAll() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);

        assertTrue(longBitSet.retainAll(Lists.newArrayList(1l, 2l)));
        assertTrue(longBitSet.contains(1));
        assertTrue(longBitSet.contains(2l));
        assertEquals(2, longBitSet.size());

        assertFalse(longBitSet.retainAll(Lists.newArrayList(1l, 2l)));
        assertEquals(2, longBitSet.size());
    }

    @Test
    public void testRetainAllArray() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);

        assertTrue(longBitSet.retainAll(new long[]{1, 2}));
        assertTrue(longBitSet.contains(1));
        assertTrue(longBitSet.contains(2));
        assertEquals(2, longBitSet.size());

        assertFalse(longBitSet.retainAll(new long[]{1, 2}));
        assertEquals(2, longBitSet.size());
    }

    @Test
    public void testRetainAllTCollection() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);

        assertTrue(longBitSet.retainAll(new TLongArrayList(new long[]{1, 2})));
        assertTrue(longBitSet.contains(1));
        assertTrue(longBitSet.contains(2));
        assertEquals(2, longBitSet.size());

        assertFalse(longBitSet.retainAll(new TLongArrayList(new long[]{1, 2})));
        assertEquals(2, longBitSet.size());
    }

    @Test
    public void testRemoveAll() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);
        longBitSet.add(4);

        assertTrue(longBitSet.removeAll(Lists.newArrayList(3l, 4l)));
        assertTrue(longBitSet.contains(1));
        assertTrue(longBitSet.contains(2));
        assertEquals(2, longBitSet.size());

        assertFalse(longBitSet.removeAll(Lists.newArrayList(3l, 4l)));
        assertEquals(2, longBitSet.size());
    }

    @Test
    public void testRemoveAllArray() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);
        longBitSet.add(4);

        assertTrue(longBitSet.removeAll(new long[]{3, 4}));
        assertTrue(longBitSet.contains(1));
        assertTrue(longBitSet.contains(2));
        assertEquals(2, longBitSet.size());

        assertFalse(longBitSet.removeAll(new long[]{3, 4}));
        assertEquals(2, longBitSet.size());
    }

    @Test
    public void testRemoveAllTCollection() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);
        longBitSet.add(4);

        assertTrue(longBitSet.removeAll(new TLongArrayList(new long[]{3, 4})));
        assertTrue(longBitSet.contains(1));
        assertTrue(longBitSet.contains(2));
        assertEquals(2, longBitSet.size());

        assertFalse(longBitSet.removeAll(new TLongArrayList(new long[]{3, 4})));
        assertEquals(2, longBitSet.size());
    }

    @Test
    public void testClear() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);
        longBitSet.add(4);

        assertEquals(4, longBitSet.size());
        longBitSet.clear();
        assertEquals(0, longBitSet.size());
        assertTrue(longBitSet.isEmpty());
    }

    @Test
    public void testForEachTrue() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);
        longBitSet.add(4);

        final Set<Long> seenInts = Sets.newHashSet();
        assertTrue(longBitSet.forEach(value -> {
            seenInts.add(value);
            return true;
        }));
        assertEquals(4, seenInts.size());
    }

    @Test
    public void testForEachSomeFalse() throws Exception {
        LongBitSet longBitSet = new LongBitSet();
        longBitSet.add(1);
        longBitSet.add(2);
        longBitSet.add(3);
        longBitSet.add(4);

        final Set<Long> seenInts = Sets.newHashSet();
        assertFalse(longBitSet.forEach(value -> {
            seenInts.add(value);
            return value % 2 == 0;
        }));
        assertEquals(4, seenInts.size());
    }

    @Test
    public void testCountLinesUpWithIteration() throws Exception {
        int setSize = 1000000;
        TLongSet intSet = new LongBitSet();
        for (int i = 0; i < setSize; i++) {
            intSet.add(i);
        }
        assertEquals(setSize, intSet.size());

        TLongIterator tIntIterator = intSet.iterator();
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