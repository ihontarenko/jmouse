package org.jmouse.core.cache;

/**
 * 🎛️ Adapter for {@link CountMinSketch} implementing {@link FrequencyEstimator}.
 *
 * <p>Acts as a thin wrapper delegating all operations to the underlying
 * Count-Min Sketch instance.</p>
 *
 * <ul>
 *   <li>📈 {@link #record(KeyWrapper)} – increments frequency counters.</li>
 *   <li>🔍 {@link #estimate(KeyWrapper)} – returns estimated frequency.</li>
 *   <li>⏳ {@link #age()} – applies decay/aging to counters.</li>
 * </ul>
 *
 * @param <K> key type
 */
public final class CountMinSketchFrequencyEstimator<K> implements FrequencyEstimator<K> {

    private final CountMinSketch<K> sketch;

    /**
     * 🏗️ Create a new estimator backed by a given {@link CountMinSketch}.
     *
     * @param sketch the underlying sketch to use
     */
    public CountMinSketchFrequencyEstimator(CountMinSketch<K> sketch) {
        this.sketch = sketch;
    }

    /**
     * 📈 Record an occurrence of the given key.
     *
     * @param key wrapped key to be recorded
     */
    @Override
    public void record(KeyWrapper<K> key) {
        sketch.offer(key.value());
    }

    /**
     * 🔍 Estimate the frequency of a key.
     *
     * @param key wrapped key to query
     * @return approximate count of occurrences
     */
    @Override
    public int estimate(KeyWrapper<K> key) {
        return sketch.estimate(key.value());
    }

    /**
     * ⏳ Apply counter aging (decay) to avoid overgrowth.
     */
    @Override
    public void age() {
        sketch.aging();
    }
}
