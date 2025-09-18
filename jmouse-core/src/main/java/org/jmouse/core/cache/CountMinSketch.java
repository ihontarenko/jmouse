package org.jmouse.core.cache;

import java.util.Random;

import static java.lang.Integer.highestOneBit;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * 📊 <b>Count–Min Sketch (CMS)</b> with a power-of-two width and a practical working depth of 4.
 *
 * <p>Provides an <i>approximate</i> frequency estimate for keys in event streams using sublinear
 * memory. Typical use: TinyLFU admission policies where only <b>relative</b> frequency comparisons
 * (candidate vs. victim) are required rather than exact counts.</p>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Maintain {@code depth} rows × {@code width} columns of 16-bit counters (stored in {@code short}).</li>
 *   <li>On each observation of a key, hash it once per row and increment the addressed counters
 *       (saturating at {@link Short#MAX_VALUE}).</li>
 *   <li>The estimate {@code f̂(k)} is the <b>minimum</b> across the row counters (hence “Count–Min”).</li>
 * </ol>
 *
 * <h3>Error guarantees (classic CMS)</h3>
 * For total updates {@code N}, with {@code width ≈ ⌈e/ε⌉} and {@code depth ≈ ⌈ln(1/δ)⌉}:
 * <pre>f(k) ≤ f̂(k) ≤ f(k) + ε·N  with probability ≥ 1−δ</pre>
 * In this implementation, width controls ε while depth controls δ. Theoretical failure probability:
 * {@code δ ≈ e^{-depth}}. Examples:
 * <ul>
 *   <li><b>d=4 → δ≈1.83%</b> ← common sweet spot</li>
 *   <li>d=5 → δ≈0.67%</li>
 *   <li>d=6 → δ≈0.25%</li>
 *   <li>d=7 → δ≈0.09%</li>
 *   <li>d=8 → δ≈0.03%</li>
 * </ul>
 * ⚠️ CMS overestimates (never underestimates); taking the min across rows limits collision bias.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>🔢 Power-of-two width → fast masking with {@code & (width-1)} instead of modulo.</li>
 *   <li>🎯 Depth 4 → strong accuracy/CPU trade-off for TinyLFU.</li>
 *   <li>🧮 Counters stored in {@code short}, read as unsigned via {@code & 0xFFFF}; saturating increments.</li>
 *   <li>⏳ {@link #aging()} performs aging (halves all counters) so recent history dominates.</li>
 *   <li>🎲 Per-instance seed + fixed mix constant improve independence across hash slices.</li>
 * </ul>
 *
 * <h3>Complexity</h3>
 * <ul>
 *   <li>{@link #offer(Object)}: O(1) (4 increments), no allocations.</li>
 *   <li>{@link #estimate(Object)}: O(1) (4 reads + 3 comparisons).</li>
 *   <li>{@link #aging()}: O(depth × width); call periodically, not per operation.</li>
 * </ul>
 *
 * <h3>Depth note</h3>
 * Although a {@code depth} parameter is accepted and clamped, the current implementation iterates
 * over the first 4 rows (d=4). The table above shows the theoretical δ for other depths.
 *
 * @param <K> key type; should provide a stable, well-distributed {@link Object#hashCode()}
 * @see <a href="https://dl.acm.org/doi/10.1145/509907.509965">Cormode &amp; Muthukrishnan (2003)</a>
 */
public final class CountMinSketch<K> {

    /**
     * 🔢 Maximum supported CMS depth (number of rows).
     * <p>Higher depth reduces the failure probability {@code δ ≈ e^{-depth}} but
     * increases CPU and memory linearly. At {@code d=8}, {@code δ≈0.03%} (theoretical).</p>
     */
    public static final int MAX_DEPTH = 8;

    /**
     * 🎯 Practical minimum (and default) CMS depth.
     * <p>{@code d=4} is a common sweet spot for TinyLFU: strong accuracy at low cost.
     * Theoretical failure probability: {@code δ≈1.83%}.</p>
     */
    public static final int MIN_DEPTH = 4;

    /**
     * 📐 Minimum width hint (columns per row) before power-of-two rounding.
     * <p>Width controls the additive error term {@code ε} (roughly {@code ε ≈ e / width}).
     * Note: with the current rounding formula
     * {@code Integer.highestOneBit(Math.max(16, width - 1)) << 1},
     * the <b>practical</b> minimum becomes {@code 32}, not {@code 16}.</p>
     */
    public static final int MIN_WIDTH = 16;

    /**
     * 🔢 Mix constant (~golden ratio for 64-bit) used to derive independent-ish hash slices:
     * {@code 0x9e3779b97f4a7c15L}. Helps to de-correlate hash streams across rows.
     */
    public static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;

    /**
     * Width (number of counters per row). Always a power of two (≥ 16) for fast masking.
     */
    private final int width;

    /**
     * Depth of table. Higher = lower probability of collision
     */
    private final int depth;

    /**
     * The counter table: {@code depth = 4} rows × {@code width} columns.
     * Each cell is a <b>non-negative</b> counter stored in a {@code short}.
     * Read as unsigned via {@code & 0xFFFF}; saturates at {@link Short#MAX_VALUE}.
     */
    private final short[][] table;

    /**
     * Per-instance random seed to diversify hash slices and reduce systematic collisions.
     */
    private final long seed;

    /**
     * ✨ Constructs a CMS with a width hint and a desired depth.
     *
     * <p><b>Width:</b> rounded up to the next power of two using
     * {@code Integer.highestOneBit(max(16, width-1)) << 1}. With this formula the practical
     * minimum becomes <b>32</b> (not 16). If you truly need a 16 minimum, adjust the rounding.</p>
     *
     * <p><b>Depth:</b> clamped to [{@link #MIN_DEPTH}..{@link #MAX_DEPTH}] (4..8), but the current
     * implementation still iterates only the first 4 rows (d=4).
     * Theoretical failure probabilities: {@code δ≈e^{-d}} e.g. d=4→~1.83%, d=8→~0.03%.</p>
     *
     * @param width  width hint, rounded up to a power of two; with the current rounding the minimum is 32
     * @param depth  desired depth; clamped to [4..8]; currently only the first 4 rows are used
     */
    public CountMinSketch(int width, int depth) {
        this.depth = max(MIN_DEPTH, min(MAX_DEPTH, depth));
        this.width = highestOneBit(max(MIN_WIDTH, width - 1)) << 1;
        this.table = new short[this.depth][this.width];
        this.seed = new Random().nextLong();
    }

    /**
     * ✨ Constructs a CMS with a width hint and the default depth {@link #MIN_DEPTH} (=4).
     *
     * <p><b>Width:</b> rounded up to the next power of two; with the current rounding the
     * practical minimum is 32. <b>Depth:</b> d=4 (theoretical {@code δ≈1.83%}).</p>
     *
     * @param width width hint, rounded up to a power of two; practical minimum is 32
     */
    public CountMinSketch(int width) {
        this(width, MIN_DEPTH);
    }

    /**
     * 🧩 Derives the column index for a given row (salt), using the key's hash
     * mixed with a per-instance seed and a fixed constant to emulate a small hash family.
     *
     * <p>We mask with {@code (width - 1)} instead of modulo, thanks to power-of-two width.</p>
     */
    private int hash(K key, int salt) {
        // Base hash: stable across JVM lifetime; quality depends on key's hashCode().
        int h = key.hashCode() ^ (int) (seed + GOLDEN_RATIO_64 * salt);

        // Lightweight finalization mix to spread high bits into low bits.
        h ^= (h >>> 16);

        // Map to [0, width) via bitmask (faster than % when width is power of two).
        return h & (width - 1);
    }

    /**
     * ➕ Records one observation of {@code key} by incrementing one counter in each row.
     * Saturates at {@link Short#MAX_VALUE} to avoid overflow.
     *
     * @param key the observed key
     */
    public void offer(K key) {
        for (int row = 0; row < 4; row++) {
            // 0 <= column < width
            int column = hash(key, row);
            // Saturating increment (unsigned view when read).
            if (table[row][column] != Short.MAX_VALUE) {
                table[row][column]++;
            }
        }
    }

    /**
     * 🔎 Returns the approximate frequency {@code f̂(key)} as the minimum across the 4 rows.
     * <p>Taking the min bounds the overcounting error introduced by collisions.</p>
     *
     * @param key the key to estimate
     * @return a non-negative integer estimate (unsigned short min across rows)
     */
    public int estimate(K key) {
        int min = Integer.MAX_VALUE;

        for (int row = 0; row < depth; row++) {
            int column = hash(key, row);
            // Read counter as unsigned 16-bit value.
            int unsigned = table[row][column] & 0xFFFF;
            if (unsigned < min) {
                min = unsigned;
            }
        }
        // If the table were empty, min would remain MAX_VALUE; here width≥16 and
        // offer() must have been called to change any cell, so min will be finite.
        // Returning min directly is safe; if no updates, caller should interpret 0 as "unseen".
        return (min == Integer.MAX_VALUE) ? 0 : min;
    }

    /**
     * 🧽 <b>Aging pass</b>: halves every counter (unsigned right shift by 1).
     *
     * <p>This gradually forgets the distant past so that recent activity dominates.
     * It also prevents counters from staying near saturation forever.</p>
     *
     * <p><b>Cost:</b> O(depth × width). Call sparingly (e.g., on a timer or every N requests).</p>
     */
    public void aging() {
        for (int r = 0; r < depth; r++) {
            for (int c = 0; c < width; c++) {
                // Read as unsigned, shift, then store back into short.
                table[r][c] = (short) ((table[r][c] & 0xFFFF) >>> 1);
            }
        }
    }
}
