package com.timdmeehan.trove.extras;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

/**
 * A version of {@link TIntSet} which is itself backed by a hashmap of key partition values to
 * {@link BitSet}, based off of the implementation outlined here:
 *
 * http://java-performance.info/bit-sets/
 *
 * This version of {@link TIntSet} is optimized for space, enabling huge sets with relatively small consumption of
 * memory.  Operations such as iteration and existence are quick, and size() is proportional to the size of the
 * underlying BitSets.
 *
 * This class is NOT thread safe.  Use {@link gnu.trove.TCollections#synchronizedSet(TIntSet)} if thread safety
 * is a requirement (and heed the advice on iterators).
 */
public class IntBitSet implements TIntSet {
    /** Number of bits allocated to a value in an index */
    private static final int VALUE_BITS = 10;
    private static final int VALUES_PER_BITSET = (int) Math.pow(2, VALUE_BITS); // 2^(32-22) values per bit set
    /** Mask for extracting values */
    private static final long VALUE_MASK = (1 << VALUE_BITS) - 1;

    private TIntObjectMap<BitSet> bitSets = new TIntObjectHashMap<>();

    /**
     * Get set index by int index (extract bits 10-31)
     * @param index int index
     * @return Index of a bit set in the inner map
     */
    private int getSetIndex(final int index) {
        return index >> VALUE_BITS;
    }

    /**
     * Get index of a value in a bit set (bits 0-9)
     * @param index Long index
     * @return Index of a value in a bit set
     */
    private int getPos(final int index) {
        return (int) (index & VALUE_MASK);
    }

    /**
     * Helper method to get (or create, if necessary) a bit set for a given long index
     * @param index Long index
     * @return A bit set for a given index (always not null)
     */
    private BitSet getOrCreateBitSet(final int index) {
        final int iIndex = getSetIndex(index);
        final BitSet bitSet;
        if (bitSets.containsKey(iIndex)) {
            bitSet = bitSets.get(iIndex);
        }
        else {
            bitSet = new BitSet(VALUE_BITS);
            bitSets.put(iIndex, bitSet);
        }
        return bitSet;
    }

    /**
     * Set a given value for a given index
     * @param index Long index
     * @param value Value to set
     */
    private void set(final int index, final boolean value) {
        if (value) {
            getOrCreateBitSet(index).set(getPos(index));
        }
        else
        {  //if value shall be cleared, check first if given partition exists
            final BitSet bitSet = bitSets.get(getSetIndex(index));
            if (bitSet != null) {
                bitSet.clear(getPos(index));
                // Remove it if empty
                if (bitSet.isEmpty()) {
                    bitSets.remove(getSetIndex(index));
                }
            }
        }
    }

    /**
     * Get a value for a given index
     * @param index Long index
     * @return Value associated with a given index
     */
    private boolean get(final int index) {
        final BitSet bitSet = bitSets.get(getSetIndex(index));
        return bitSet != null && bitSet.get(getPos(index));
    }

    /**
     * Returns the cardinality, or number of set bits, aggregated across each key.
     * @return the cardinality of this set
     */
    private int cardinality() {
        int cardinality = 0;
        for (final BitSet bitSet : bitSets.valueCollection()) {
            cardinality += bitSet.cardinality();
        }
        return cardinality;
    }

    // Only exists to fulfil the contract of TIntSet.  Not used.
    public int getNoEntryValue() {
        return 0;
    }

    /**
     * @return the size of this set
     */
    public int size() {
        return cardinality();
    }

    /**
     * Is the set empty
     * @return
     */
    public boolean isEmpty() {
        return bitSets.isEmpty();
    }

    /**
     * Tests to see if an entry is in this set
     * @param entry the entry to test
     * @return true if it exists in this set, false otherwise
     */
    public boolean contains(final int entry) {
        return get(entry);
    }

    /**
     * @return a {@link TIntIterator} representation of this set
     */
    public TIntIterator iterator() {
        return new IntBitSetIterator(bitSets);
    }

    /**
     * @return the array representing all the values in this set
     */
    public int[] toArray() {
        final int size = size();
        final int[] arr = new int[size];
        int index = 0;
        final TIntIterator iterator = iterator();
        while(iterator.hasNext()) {
            arr[index++] = iterator.next();
        }
        return arr;
    }

    /**
     * Copies the values of this set into the provided array
     * @param dest the array to copy to
     * @return the dest array
     */
    public int[] toArray(final int[] dest) {
        int index = 0;
        final TIntIterator iterator = iterator();
        while (index < dest.length && iterator.hasNext()) {
            dest[index++] = iterator.next();
        }
        while (index < dest.length && index < dest.length) {
            dest[index++] = getNoEntryValue();
        }
        return dest;
    }

    /**
     * Add an entry to this set
     * @param entry the entry to add
     * @return true if value was not already present, false otherwise
     */
    public boolean add(final int entry) {
        final boolean isSet = get(entry);
        set(entry, true);
        return !isSet;
    }

    /**
     * Remove the entry from this set
     * @param entry the entry to remove
     * @return true if element was in set and removed, false otherwise
     */
    public boolean remove(final int entry) {
        final boolean isSet = get(entry);
        set(entry, false);
        return isSet;
    }

    /**
     * Given a collection, will test each Integer element and check its presence in this set
     * @param collection the collection to test
     * @return true if every element was present from the collection, false otherwise
     */
    public boolean containsAll(final Collection<?> collection) {
        for (final Object o : collection) {
            if (!(o instanceof Number)) {
                return false;
            }
            if (!get(((Number)o).intValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Given a collection, will test each int element for existence in the set
     * @param collection the collection to test
     * @return true if each item in the collection was present, false otherwise
     */
    public boolean containsAll(final TIntCollection collection) {
        return collection.forEach(this::get);
    }

    /**
     * Given an array, will test each int element for existence in the set
     * @param array the array to test
     * @return true if each item in the array was present, false otherwise
     */
    public boolean containsAll(final int[] array) {
        return containsAll(new TIntArrayList(array));
    }

    /**
     * Adds each item in the collection to this set
     * @param collection the collection of items to add
     * @return true if each item was added and not present, false otherwise
     */
    public boolean addAll(final Collection<? extends Integer> collection) {
        boolean modified = false;
        for (final int integer : collection) {
            modified = add(integer) || modified;
        }
        return modified;
    }

    /**
     * Adds each item in the collection to this set
     * @param collection the collection of items to add
     * @return true if each item was added and not present, false otherwise
     */
    public boolean addAll(final TIntCollection collection) {
        final TIntIterator iterator = collection.iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            modified = add(iterator.next()) || modified;
        }
        return modified;
    }

    /**
     * Adds each item in the array to this set
     * @param array the array of items to add
     * @return true if each item was added and not present, false otherwise
     */
    public boolean addAll(int[] array) {
        boolean modified = false;
        for (Integer integer : array) {
            modified = add(integer) || modified;
        }
        return modified;
    }

    /**
     * Removes everything from this set except the values in the provided collection
     * @param collection the collection of items to retain
     * @return true if at least one item was removed, false otherwise
     */
    public boolean retainAll(final Collection<?> collection) {
        final TIntIterator iterator = iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            if (!collection.contains(iterator.next())) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Removes everything from this set except the values in the provided collection
     * @param collection the collection of items to retain
     * @return true if at least one item was removed, false otherwise
     */
    public boolean retainAll(final TIntCollection collection) {
        if (this == collection) {
            return false;
        }
        final TIntIterator iterator = iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            if (!collection.contains(iterator.next())) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Removes everything from this set except the values in the provided array
     * @param arrayInput the collection of items to retain
     * @return true if at least one item was removed, false otherwise
     */
    public boolean retainAll(final int[] arrayInput) {
        final int[] array = new int[arrayInput.length];
        System.arraycopy(arrayInput, 0, array, 0, arrayInput.length);
        Arrays.sort(array);
        final TIntIterator iterator = iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            if (Arrays.binarySearch(array, iterator.next()) < 0) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Removes all the elements from this set in the provided collection
     * @param collection the collection of items to remove
     * @return true if at least one item was removed, false otherwise
     */
    public boolean removeAll(final Collection<?> collection) {
        boolean modified = false;
        for (Object o : collection) {
            modified = remove((Integer) o) || modified;
        }
        return modified;
    }

    /**
     * Removes all the elements from this set in the provided collection
     * @param collection the collection of items to remove
     * @return true if at least one item was removed, false otherwise
     */
    public boolean removeAll(final TIntCollection collection) {
        TIntIterator iterator = collection.iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            modified = remove(iterator.next()) || modified;
        }
        return modified;
    }

    /**
     * Removes all the elements from this set in the provided array
     * @param array the collection of items to remove
     * @return true if at least one item was removed, false otherwise
     */
    public boolean removeAll(final int[] array) {
        boolean modified = false;
        for (final int val : array) {
            modified = remove(val) || modified;
        }
        return modified;
    }

    /**
     * Clear the entire set
     */
    public void clear() {
        bitSets.clear();
    }

    /**
     * Execute the provided procedure on each element in the set
     * @param procedure the subroutine to execute against each element in this set
     * @return false if one execution returned false, otherwise true
     */
    public boolean forEach(final TIntProcedure procedure) {
        final TIntIterator iterator = iterator();

        boolean procedureResult = true;
        while (iterator.hasNext()) {
            procedureResult = procedure.execute(iterator.next()) && procedureResult;
        }

        return procedureResult;
    }

    /**
     *
     */
    private static final class IntBitSetIterator implements TIntIterator {
        private final TIntObjectIterator<BitSet> bitSetIterator;
        private boolean hasNext;
        private int nextValue;
        private int currentPositionInKey = -1;
        private int previousPositionInKey = -1;

        public IntBitSetIterator(final TIntObjectMap<BitSet> bitSets) {
            this.bitSetIterator = bitSets.iterator();
            hasNext = advanceIterator();
        }

        private boolean advanceIterator() {
            while (true) {
                // The current bit position on the current bitset is a potential candidate for a value
                if (currentPositionInKey >= 0 && currentPositionInKey < VALUES_PER_BITSET) {
                    // Update it to the next set value
                    currentPositionInKey = bitSetIterator.value().nextSetBit(currentPositionInKey);
                    // If present
                    if (currentPositionInKey >= 0) {
                        // Decode the value
                        nextValue = (bitSetIterator.key() << VALUE_BITS) + currentPositionInKey;
                        previousPositionInKey = currentPositionInKey;
                        currentPositionInKey++;
                        return true;
                    }
                }
                // Move on to the next bit set
                else if (bitSetIterator.hasNext()) {
                    bitSetIterator.advance();
                    currentPositionInKey = 0;
                }
                // Exhausted this iterator
                else {
                    return false;
                }
            }
        }

        public int next() {
            if (!hasNext) {
                throw new IllegalStateException("No more iterations available in iterator");
            }
            final int nextValue = this.nextValue;
            hasNext = advanceIterator();
            return nextValue;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public void remove() {
            bitSetIterator.value().clear(previousPositionInKey);
            if (bitSetIterator.value().isEmpty()) {
                bitSetIterator.remove();
            }
        }
    }
}