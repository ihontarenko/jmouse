package org.jmouse.core.cache;

import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 🚪 <b>Doorkeeper</b>: a tiny Bloom-like front filter for TinyLFU.
 *
 * <p>The doorkeeper suppresses one-hit noise before it reaches the frequency sketch (e.g., CMS).
 * On the <b>first</b> sighting of a key, it sets {@code numberOfHashes} bits and returns {@code false}
 * (meaning: “do not count yet”). On the <b>second and subsequent</b> sightings, those bits are already set
 * and it returns {@code true} (meaning: “consider this a repeated key; allow counting”).</p>
 *
 * <h3>Why 2 hash functions by default?</h3>
 * Using {@code k=2} bits is a strong practical sweet spot for false-positive rate (FP) vs. cost.
 * The FP probability for a Bloom-like filter is approximately:
 * <pre>
 * p_fp ≈ (1 - e^{-k·n/m})^k
 * </pre>
 * where {@code m} is the number of bits in the filter, {@code k} is the number of hash functions,
 * and {@code n} is the number of distinct keys observed since the last {@link #reset()}.
 * <ul>
 *   <li>{@code k=1}: cheaper but significantly higher FP.</li>
 *   <li><b>{@code k=2}</b>: large FP drop for very small extra cost (two bit writes on first sighting).</li>
 *   <li>{@code k>2}: sometimes marginal FP gains, but higher CPU and not always beneficial unless {@code m/n} is large.</li>
 * </ul>
 *
 * <h3>Epoch semantics</h3>
 * Calling {@link #reset()} clears all bits, starting a new “epoch”. This intentionally forgets earlier
 * first sightings so popularity is measured over recent history.
 *
 * <h3>Complexity</h3>
 * <ul>
 *   <li>{@link #admit(Object)}: O(k) bit lookups (and O(k) bit sets on first sight).</li>
 *   <li>{@link #reset()}: O(m) to clear, implemented via {@link BitSet#clear()} (efficient native path).</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * Not thread-safe. Guard with external synchronization or shard per thread/core.
 *
 * @param <K> key type
 */
public final class Doorkeeper<K> {

    /**
     * 🎯 Default number of hash functions (k) used by the doorkeeper.
     * <p>{@code k=2} is commonly used in TinyLFU to sharply reduce false positives
     * while keeping first-sighting cost minimal.</p>
     */
    public static final int DEFAULT_NUMBER_HASHES = 2;

    /**
     * Bit table backing the front filter. Its length in bits is {@link #size}.
     */
    private final BitSet bits;

    /**
     * Number of addressable bit positions in the filter.
     * <p>Computed as the next power of two with a minimum of 1024, enabling fast masking arithmetic.</p>
     */
    private final int size;

    /**
     * Per-instance random seed used to decorrelate hash slices.
     */
    private final long seed;

    /**
     * The number of hash functions (k). Typical value: {@link #DEFAULT_NUMBER_HASHES} (=2).
     */
    private final int numberOfHashes;

    /**
     * ✨ Constructs a doorkeeper with the given bit capacity hint and the default number of hashes (2).
     *
     * <p><b>Capacity:</b> the internal bitset size is rounded up to the next power of two
     * with a practical minimum of 1024. Larger sizes reduce the false-positive probability
     * for the same number of distinct keys per epoch.</p>
     *
     * @param bitCountHint desired bit capacity hint (will be rounded up; minimum practical size is 1024)
     */
    public Doorkeeper(int bitCountHint) {
        this(bitCountHint, DEFAULT_NUMBER_HASHES);
    }

    /**
     * ✨ Constructs a doorkeeper with an explicit number of hash functions.
     *
     * <p><b>Capacity:</b> the internal bitset size is rounded up to the next power of two
     * with a practical minimum of 1024, so that index arithmetic can use a fast mask
     * ({@code index & (bitsetSize - 1)}).</p>
     *
     * <p><b>Hash count (k):</b> controls the FP/cost trade-off; {@code k=2} is usually best for TinyLFU.</p>
     *
     * @param bitCountHint     desired bit capacity hint (rounded up; minimum practical size is 1024)
     * @param numberOfHashes   number of hash functions (k ≥ 1); typical value: 2
     */
    public Doorkeeper(int bitCountHint, int numberOfHashes) {
        this.size = Math.max(1024, Integer.highestOneBit(bitCountHint - 1) << 1);
        this.bits = new BitSet(this.size);
        this.seed = ThreadLocalRandom.current().nextLong();
        this.numberOfHashes = numberOfHashes;
    }

    /**
     * 💡 Admission check.
     *
     * <p>Returns {@code true} if all {@code numberOfHashes} bit positions for the key are already set
     * (i.e., the key is considered “seen before” in the current epoch). Returns {@code false} on the first
     * sighting and sets those bits to mark the key thereafter.</p>
     *
     * @param key the key being checked
     * @return {@code true} if the key is considered “seen before”; {@code false} if this is the first sighting
     */
    public boolean admit(K key) {
        boolean seenBefore = true;

        // Check all k bit positions. If any is not set, this is the first sighting.
        for (int index = 0; index < numberOfHashes; index++) {
            int bitIndex = indexFor(key, index);
            seenBefore &= bits.get(bitIndex);
        }

        // On first sighting, set all k bits so subsequent calls will pass.
        if (!seenBefore) {
            for (int index = 0; index < numberOfHashes; index++) {
                int bitIndex = indexFor(key, index);
                bits.set(bitIndex);
            }
        }

        return seenBefore;
    }

    /**
     * 🧽 Clears all bits, starting a new epoch of first/second sightings.
     * <p>Use this periodically to bound the effective window and keep the false-positive rate in check.</p>
     */
    public void reset() {
        bits.clear();
    }

    /**
     * 🧩 Computes the bit index for the given key and hash slice (salt).
     *
     * <p>We mix a 32-bit {@code hashCode()} with a 64-bit seed and a golden-ratio stride,
     * then apply a 64-bit finalization (similar to Stafford/Murmur mix) and fold to 32 bits.
     * The final index is mapped via a power-of-two mask: {@code index & (bitsetSize - 1)}.</p>
     *
     * @param key        key to address
     * @param saltIndex  which hash slice to use (0..k-1)
     * @return bit index in {@link #bits}
     */
    private int indexFor(K key, int saltIndex) {
        long mixed = (long) key.hashCode() ^ seed ^ (0x9e3779b97f4a7c15L * (saltIndex + 1));

        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= (mixed >>> 33);

        int h32 = (int) mixed;

        return h32 & (size - 1);
    }

}
