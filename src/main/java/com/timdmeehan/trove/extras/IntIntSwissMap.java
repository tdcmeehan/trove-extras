package com.timdmeehan.trove.extras;

import gnu.trove.TIntCollection;
import gnu.trove.function.TIntFunction;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;

import java.util.Map;

import static java.lang.Long.BYTES;
import static java.lang.Long.numberOfTrailingZeros;
import static java.lang.Math.toIntExact;

/**
 * An implementation of a Trove primitive integer to integer map which is inspired by SwissTable.
 */
public class IntIntSwissMap
        implements TIntIntMap
{
    // Copied from SmoothieMap implementation of SwissTable
    private static final long LEAST_SIGNIFICANT_BYTE_BITS = 0x0101010101010101L;
    private static final long MOST_SIGNIFICANT_BYTE_BITS = 0x8080808080808080L;
    // Copied from fastutil
    /** 2<sup>32</sup> &middot; &phi;, &phi; = (&#x221A;5 &minus; 1)/2. */
    private static final long LONG_PHI = 0x9E3779B97F4A7C15L;
    private static final int INITIAL_SIZE = 16;
    private static final long METADATA_MASK = 0x7;
    private static final long KEYS_MASK = ~METADATA_MASK;
    private static final long H2_MASK = 0x7F;
    private static final int KEY_BYTE_MASK = 0x80;

    private final double loadFactor;
    private final int noEntryKey;
    private final int noEntryValue;

    private long[] metadatas;
    private int[] keys;
    private int[] values;
    private long mask;
    private int filled;
    private int deleted;
    private int resizeThreshold;

    public IntIntSwissMap(double loadFactor, int noEntryKey, int noEntryValue)
    {
        this.loadFactor = loadFactor;
        this.noEntryKey = noEntryKey;
        this.noEntryValue = noEntryValue;
        clear();
    }

    @Override
    public int getNoEntryKey() {
        return noEntryKey;
    }

    @Override
    public int getNoEntryValue() {
        return noEntryValue;
    }

    @Override
    public int put(int key, int value)
    {
        long hash = mix(key);
        long keyByte = getKeyByte(hash);
        int location = find(key, keyByte, hash);

        int oldValue = location >= 0 ? values[location] : noEntryValue;
        location = location >= 0 ? location : -location - 1;
        keys[location] = key;
        values[location] = value;
        addKeyByteToMetadata(metadatas, keyIndexToMetadataIndex(location), keyIndexToMetadataWordIndex(location), keyByte);
        if (++filled == resizeThreshold) {
            rehash();
        }
        return oldValue;
    }

    @Override
    public int putIfAbsent(int key, int value) {
        long hash = mix(key);
        long keyByte = getKeyByte(hash);
        int location = find(key, keyByte, hash);

        if (location >= 0) {
            return getNoEntryValue();
        }

        location = -location - 1;
        keys[location] = key;
        values[location] = value;
        addKeyByteToMetadata(metadatas, keyIndexToMetadataIndex(location), keyIndexToMetadataWordIndex(location), keyByte);
        if (++filled == resizeThreshold) {
            rehash();
        }
        return getNoEntryValue();
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> map) {
        // TODO: add bulk rehash method #ensureCapacity
        // ensureCapacity( map.size() );
        // TODO: could optimize this for cases when map instanceof THashMap
        for ( Map.Entry<? extends Integer, ? extends Integer> entry : map.entrySet() ) {
            this.put( entry.getKey().intValue(), entry.getValue().intValue() );
        }
    }

    @Override
    public void putAll(TIntIntMap map) {
        // TODO: add bulk rehash method #ensureCapacity
        // ensureCapacity( map.size() );
        TIntIntIterator iter = map.iterator();
        while (iter.hasNext()) {
            iter.advance();
            put(iter.key(), iter.value());
        }
    }

    @Override
    public int get(int key)
    {
        long hash = mix(key);
        long keyByte = getKeyByte(hash);

        long keyWord = fillWordWithByte(keyByte);
        int location = getLocationFromHash(hash);
        while (true) {
            long metadata = metadatas[keyIndexToMetadataIndex(location)];
            int indexOfByte = getIndexOfByte(metadata, keyWord);

            while (indexOfByte <= 7){
                if (keys[location + indexOfByte] == key) {
                    return values[location + indexOfByte];
                }
                metadata = unsetBitAtIndex(metadata, indexOfByte);
                indexOfByte = getIndexOfByte(metadata, keyWord);
            }

            // Was not a match -- was there an empty entry or a tombstone?
            indexOfByte = getIndexOfZeroByte(metadata);
            if (indexOfByte <= 7) {
                return noEntryValue;
            }

            // it was a tombstone
            location = rangeReduction(location + 8);
        }
    }

    @Override
    public void clear() {
        this.filled = 0;
        metadatas = new long[INITIAL_SIZE / BYTES]; // metadata int represents 4 bytes of metadata
        keys = new int[INITIAL_SIZE];
        values = new int[INITIAL_SIZE];
        mask = keys.length - 1 & KEYS_MASK;
        resizeThreshold = (int) (loadFactor * keys.length);
    }

    @Override
    public boolean isEmpty() {
        return (filled - deleted) == 0;
    }

    @Override
    public int remove(int key) {
        int prev = noEntryValue;
        int index = find(key);
        if (index >= 0) {
            prev = values[index];
            removeAt(index);
        }
        return prev;
    }

    private void removeAt(int index) {
        int metadatasIndex = keyIndexToMetadataIndex(rangeReduction(index));
        int byteIndex = keyIndexToMetadataWordIndex(index);
        metadatas[metadatasIndex] = unsetBitAtIndex(metadatas[metadatasIndex], byteIndex);
        deleted++;
    }

    @Override
    public int size() {
        return filled;
    }

    @Override
    public TIntSet keySet() {
        // TODO
        return null;
    }

    @Override
    public int[] keys() {
        return keys(new int[]{});
    }

    @Override
    public int[] keys(int[] ints) {
        if (ints.length < (filled - deleted)) {
            ints = new int[filled - deleted];
        }
        int counter = 0;
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                ints[counter++] = keys[index];
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
        return ints;
    }

    @Override
    public TIntCollection valueCollection() {
        // TODO
        return null;
    }

    @Override
    public int[] values() {
        return values(new int[]{});
    }

    @Override
    public int[] values(int[] ints) {
        if (ints.length < (filled - deleted)) {
            ints = new int[filled - deleted];
        }
        int counter = 0;
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                ints[counter++] = values[index];
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
        return ints;
    }

    @Override
    public boolean containsValue(int value) {
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                if (values[index] == value) {
                    return true;
                }
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(int key) {
        return find(key) >= 0;
    }

    @Override
    public TIntIntIterator iterator() {
        // TODO
        return null;
    }

    @Override
    public boolean forEachKey(TIntProcedure procedure) {
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                if (!procedure.execute(keys[index])) {
                    return false;
                }
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
        return true;
    }

    @Override
    public boolean forEachValue(TIntProcedure procedure) {
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                if (!procedure.execute(values[index])) {
                    return false;
                }
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
        return true;
    }

    @Override
    public boolean forEachEntry(TIntIntProcedure procedure) {
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                if (!procedure.execute(keys[index], values[index])) {
                    return false;
                }
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
        return true;
    }

    @Override
    public void transformValues(TIntFunction function) {
        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                values[index] = function.execute(values[index]);
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }
    }

    @Override
    public boolean retainEntries(TIntIntProcedure procedure) {
        boolean modified = false;

        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int indexInByte = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int index = i * BYTES + indexInByte;
                if (!procedure.execute(keys[index], values[index])) {
                    removeAt(index);
                    modified = true;
                }
                metadata = unsetBitAtIndex(metadata, indexInByte);
            }
        }

        return modified;
    }

    @Override
    public boolean increment(int key) {
        int location = find(key);

        // Location present, adjust the value
        if (location >= 0) {
            values[location] += 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean adjustValue(int key, int adjustAmount) {
        int location = find(key);

        // Location present, adjust the value
        if (location >= 0) {
            values[location] += adjustAmount;
            return true;
        }
        return false;
    }

    @Override
    public int adjustOrPutValue(int key, int adjustAmount, int putValue) {
        long hash = mix(key);
        long keyByte = getKeyByte(hash);
        int location = find(key, keyByte, hash);
        int newValue;

        // Location present, adjust the value
        if (location >= 0) {
            newValue = (values[location] += adjustAmount);
        }
        else {
            location = -location - 1;
            keys[location] = key;
            newValue = (values[location] = putValue);
            addKeyByteToMetadata(metadatas, keyIndexToMetadataIndex(location), keyIndexToMetadataWordIndex(location), keyByte);
            if (++filled == resizeThreshold) {
                rehash();
            }
        }
        return newValue;
    }

    private int find(int key) {
        long hash = mix(key);
        return find(key, getKeyByte(hash), hash);
    }

    /**
     * Returns the first index of either a match or free location if there is a match.  Returns (-location + 1) of the
     * first free space if there was no match.  It is presumed that the table is never full.
     */
    private int find(int key, long keyByte, long hash) {
        long keyWord = fillWordWithByte(keyByte);
        int location = getLocationFromHash(hash);
        while (true) {
            long metadata = metadatas[keyIndexToMetadataIndex(location)];
            int indexOfByte = getIndexOfByte(metadata, keyWord);

            while (indexOfByte <= 7){
                if (keys[location + indexOfByte] == key) {
                    return location + indexOfByte;
                }
                metadata = unsetBitAtIndex(metadata, indexOfByte);
                indexOfByte = getIndexOfByte(metadata, keyWord);
            }

            // Was not a match -- was there an empty entry or a tombstone?
            indexOfByte = getIndexOfZeroByte(metadata);
            if (indexOfByte <= 7) {
                return -(location + indexOfByte + 1);
            }

            // it was a tombstone
            location = rangeReduction(location + 8);
        }
    }

    private void rehash() {
        int[] newKeys = new int[keys.length * 2];
        int[] newValues = new int[values.length * 2];
        long[] newMetadatas = new long[metadatas.length * 2];
        mask = newKeys.length - 1 & KEYS_MASK;
        resizeThreshold = (int) (loadFactor * newKeys.length);

        for (int i = 0; i < metadatas.length; i++) {
            long metadata = metadatas[i];
            // Returns the flipped 8th bits of key bytes which are present
            metadata = metadata & MOST_SIGNIFICANT_BYTE_BITS;
            while (metadata != 0) {
                int oldIndex = keyIndexToMetadataIndex(numberOfTrailingZeros(metadata)); // Returns the index of the first present key byte
                int key = keys[i * BYTES + oldIndex];
                int value = values[i * BYTES + oldIndex];

                long hash = mix(key);
                long keyByte = getKeyByte(hash);

                int location = getLocationFromHash(hash);
                int metadataIndex = keyIndexToMetadataIndex(location);
                long newMetadata = newMetadatas[metadataIndex];
                int indexOfZeroByte;
                while ((indexOfZeroByte = getIndexOfZeroByte(newMetadata)) > 7) {
                    location = rangeReduction(location + 8);
                    newMetadata = newMetadatas[(metadataIndex = keyIndexToMetadataIndex(location))];
                }
                addKeyByteToMetadata(newMetadatas, metadataIndex, indexOfZeroByte, keyByte);
                newKeys[location + indexOfZeroByte] = key;
                newValues[location + indexOfZeroByte] = value;
                metadata = unsetBitAtIndex(metadata, oldIndex);
            }
        }
        keys = newKeys;
        values = newValues;
        metadatas = newMetadatas;
    }

    /******************************************************************************************************************\
     *                                                                                                                  *
     * Bit functions                                                                                                    *
     *                                                                                                                  *
     \******************************************************************************************************************/

    private static int keyIndexToMetadataIndex(int location) {
        return location >>> 3; // Equivalent to location / Long.BYTES
    }

    private static int keyIndexToMetadataWordIndex(int location) {
        return (int) (location & METADATA_MASK); // // Equivalent to location % Long.BYTES
    }

    private static long unsetBitAtIndex(long metadata, int indexOfByte) {
        return metadata & ~(1L << (indexOfByte * 8 + 7));
    }

    // From fastutil
    private static long mix(final long x) {
        long h = x * LONG_PHI;
        h ^= h >>> 32;
        return h ^ (h >>> 16);
    }

    // From SmoothieMap
    private static long fillWordWithByte(long metadata) {
        // Equivalent to: (metadata << 24) | (metadata << 16) | (metadata << 8) | metadata;
        return LEAST_SIGNIFICANT_BYTE_BITS * metadata;
    }

    // Returns 8 if there was no 0
    static int getIndexOfZeroByte(long v) {
        return keyIndexToMetadataIndex(numberOfTrailingZeros(getZeroBytesMask(v)));
    }

    private static long getZeroBytesMask(long v) {
        return (v - LEAST_SIGNIFICANT_BYTE_BITS) & ~v & MOST_SIGNIFICANT_BYTE_BITS;
    }

    /**
     * Returns the index of the input byte into a word which represents 4 key bytes
     * @param x the word to check for existence
     * @param keyWord the word which represents 4 consecutive key bytes to check
     * @return the index of the key byte, or some number > 7 if not present
     */
    private static int getIndexOfByte(long x, long keyWord) {
        return getIndexOfZeroByte( x ^ keyWord);
    }

    private int getLocationFromHash(long hash) {
        return toIntExact(rangeReduction(hash >>> 7));
    }

    private int rangeReduction(long h1) {
        return (int) (h1 & mask); // Mask should always be less than integer max value
    }

    private static long getKeyByte(long hash) {
        long h2 = hash & H2_MASK;
        return h2 | KEY_BYTE_MASK;
    }

    /**
     * Adds the given keyByte into the metadata
     * @param metadatas the metadata array
     * @param metadataIndex the index into the metadata array
     * @param metadataWordIndex the index into a word in the metadata array
     * @param keyByte the key byte to insert
     */
    private static void addKeyByteToMetadata(long[] metadatas, int metadataIndex, int metadataWordIndex, long keyByte) {
        metadatas[metadataIndex] |= (keyByte << (metadataWordIndex << 3));
    }

}
