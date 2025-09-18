package org.jmouse.core;

import java.util.Objects;

/**
 * 🧪 Hash mixing utilities (32/64-bit).
 *
 * <p>Provides:
 * <ul>
 *   <li>Golden-ratio constants (Fibonacci hashing)</li>
 *   <li>Murmur3 32-bit finalizer</li>
 *   <li>Stafford mix13 64-bit finalizer</li>
 *   <li>Helpers for deriving multiple de-correlated hashes/indices</li>
 * </ul>
 *
 * <p>Notes:
 * <ul>
 *   <li>Index helpers assume power-of-two table sizes.</li>
 *   <li>Use {@code spread32/spread64} before masking for better low-bit quality.</li>
 * </ul>
 */
public final class HashMix {

    /**
     * φ32 ≈ 2^32 / φ (Knuth multiplicative hashing constant).
     */
    public static final int  PHI32 = 0x9e3779b9;

    /**
     * φ64 ≈ 2^64 / φ (widely used golden ratio step).
     */
    public static final long PHI64 = 0x9e3779b97f4a7c15L;

    private HashMix() {
    }

    /**
     * 🌪️ MurmurHash3 32-bit finalizer (strong avalanche for 32-bit words).
     */
    public static int murmur3(int value) {
        int mixed = value;
        mixed ^= (mixed >>> 16);
        mixed *= 0x85ebca6b;
        mixed ^= (mixed >>> 13);
        mixed *= 0xc2b2ae35;
        mixed ^= (mixed >>> 16);
        return mixed;
    }

    /**
     * 🌪️ Stafford variant mix13 (64-bit), as used by SplitMix64.
     */
    public static long stafford13(long value) {
        long mixed = value;
        mixed ^= (mixed >>> 30);
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94d049bb133111ebL;
        mixed ^= (mixed >>> 31);
        return mixed;
    }

    // ------------------------------------------------------------
    // 🧮 Fibonacci hashing steps
    // ------------------------------------------------------------

    /**
     * Multiplicative step for 32-bit inputs (golden ratio).
     */
    public static int fibStep32(int value) {
        return value * PHI32;
    }

    /**
     * Multiplicative step for 64-bit inputs (golden ratio).
     */
    public static long fibStep64(long value) {
        return value * PHI64;
    }

    // ------------------------------------------------------------
    // 🎯 Spreading helpers (improve low-bit entropy before masking)
    // ------------------------------------------------------------

    /**
     * Spread a 32-bit hash to improve low-bit quality (then mask).
     */
    public static int spread32(int hash) {
        return murmur3(hash);
    }

    /**
     * Spread a 64-bit hash to improve low-bit quality (then cast/mask).
     */
    public static long spread64(long hash) {
        return stafford13(hash);
    }

    /**
     * Hash any object to 32-bit with Murmur3 finalization.
     */
    public static <T> int hash32(T object) {
        int baseHash = Objects.hashCode(object);
        return murmur3(baseHash);
    }

    /**
     * Hash any object to 64-bit by widening and mixing.
     */
    public static <T> long hash64(T object) {
        long widenedHash = Objects.hashCode(object);
        return stafford13(widenedHash);
    }

    // ------------------------------------------------------------
    // 📦 Indexing into power-of-two tables
    // ------------------------------------------------------------

    /**
     * Compute an index for a 32-bit hash into a power-of-two table size.
     *
     * @param hash      any 32-bit hash
     * @param tableSize must be power of two
     */
    public static int index32(int hash, int tableSize) {
        int spreadHash = spread32(hash);
        return spreadHash & (tableSize - 1);
    }

    /**
     * Compute an index for a 64-bit hash into a power-of-two table size.
     *
     * @param hash      any 64-bit hash
     * @param tableSize must be power of two
     */
    public static int index64(long hash, int tableSize) {
        long spreadHash = spread64(hash);
        return (int) (spreadHash & (tableSize - 1));
    }

    /**
     * Derive a de-correlated 32-bit hash from a base using a salt via φ32.
     * <p>Cheap & decent for generating multiple stream hashes.</p>
     */
    public static int derive32(int baseHash, int salt) {
        int stepped = baseHash + PHI32 * salt;
        return murmur3(stepped);
    }

    /**
     * Derive a de-correlated 64-bit hash from a base using a salt via φ64.
     */
    public static long derive64(long baseHash, int salt) {
        long stepped = baseHash + PHI64 * (long) salt;
        return stafford13(stepped);
    }

    /**
     * Compute {@code count} indices for a power-of-two table from an object key (32-bit path).
     */
    public static int[] indices32(Object key, int count, int tableSize, int seed) {
        int[] indices    = new int[count];
        int   baseHash   = Objects.hashCode(key) ^ seed;
        int   spreadHash = murmur3(baseHash);

        for (int index = 0; index < count; index++) {
            indices[index] = index32(spreadHash, tableSize);
            spreadHash += PHI32; // golden stride
        }
        return indices;
    }

    /**
     * Compute {@code count} indices for a power-of-two table from an object key (64-bit path).
     */
    public static int[] indices64(Object key, int count, int tableSize, long seed) {
        int[] indices = new int[count];
        long  base64  = hash64(key) ^ seed;
        long  mixed64 = stafford13(base64);

        for (int index = 0; index < count; index++) {
            indices[index] = index64(mixed64, tableSize);
            mixed64 += PHI64; // golden stride
        }
        return indices;
    }

    // ------------------------------------------------------------
    // 🧱 Seeds & salts
    // ------------------------------------------------------------

    /**
     * Mix a 64-bit seed with a salt using φ64 stride + strong avalanche.
     */
    public static long mixSeed64(long seed, int salt) {
        long stepped = seed + PHI64 * (long) salt;
        return stafford13(stepped);
    }

    /**
     * Mix a 32-bit seed with a salt using φ32 stride + 32-bit avalanche.
     */
    public static int mixSeed32(int seed, int salt) {
        int stepped = seed + PHI32 * salt;
        return murmur3(stepped);
    }
}