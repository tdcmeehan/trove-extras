package com.timdmeehan.trove.extras;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class IntIntSwissMapTest {

    private static final int DEFAULT = 0XDEADBEEF;

    @Test
    public void testPut() {
        IntIntSwissMap map = new IntIntSwissMap(0.75, DEFAULT, DEFAULT);
        assertEquals(map.put(123, 456), DEFAULT);
        assertEquals(map.get(123), 456);
        assertEquals(map.put(123, 789), 456);
        assertEquals(map.get(123), 789);
    }

    @RepeatedTest(1000)
    public void testRandom() {
        IntIntSwissMap map = new IntIntSwissMap(0.75, DEFAULT, DEFAULT);
        assertTrue(map.isEmpty());

        TIntSet randomInts = new TIntHashSet();
        for (int i = 0; i < 20_000; i++) {
            while (!randomInts.add(ThreadLocalRandom.current().nextInt())) {}
        }
        TIntIterator intIterator = randomInts.iterator();

        while (intIterator.hasNext()) {
            int key = intIterator.next();
            int value = intIterator.next();

            assertFalse(map.containsKey(key));
            assertFalse(map.containsValue(value));

            assertEquals(map.get(key), DEFAULT);
            assertEquals(map.put(key, value), DEFAULT);
            assertFalse(map.isEmpty());
            assertTrue(map.containsKey(key));
            assertTrue(map.containsValue(value));
            assertEquals(map.get(key), value);
            assertEquals(map.put(key, value + 1), value);
            assertEquals(map.get(key), value + 1);
            assertEquals(map.put(key, value), value + 1);
            assertEquals(map.get(key), value);

            assertTrue(map.increment(key));
            assertEquals(map.get(key), value + 1);
            assertTrue(map.adjustValue(key, 2));
            assertEquals(map.get(key), value + 3);
            assertEquals(map.remove(key), value + 3);
            assertFalse(map.containsKey(key));
            assertEquals(map.adjustOrPutValue(key, 4, value), value);
            assertEquals(map.adjustOrPutValue(key, 4, value), value + 4);
            assertEquals(map.adjustOrPutValue(key, -4, value), value);
        }
    }
}
