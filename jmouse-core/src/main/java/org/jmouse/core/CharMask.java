package org.jmouse.core;

import java.util.*;

/**
 * 🔤 Compact character set built on top of {@link Bits} windows (64 chars each).
 *
 * <p>Unicode char space is partitioned into fixed 64-wide windows:
 * base = (ch / 64) * 64, index = ch - base. Each window stores its flags in a {@link Bits}.</p>
 *
 * <h2>Highlights</h2>
 * <ul>
 *   <li>⚡ O(1) membership test via {@link #contains(char)}</li>
 *   <li>🎯 Supports arbitrary char ranges and individual chars</li>
 *   <li>💾 Memory-efficient for sparse sets (only touched windows are allocated)</li>
 * </ul>
 */
public final class CharMask {

    /**
     * 📏 Window size in code points (64 = one {@code long}).
     */
    private static final int WINDOW_SIZE = 64;

    /**
     * 🗂️ Windows keyed by base boundary (multiple of 64).
     */
    private final Map<Integer, Window> windows;

    private CharMask(Map<Integer, Window> windows) {
        this.windows = new LinkedHashMap<>(windows);
    }

    /**
     * 🛠 Entry point for builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 🧱 Build a mask from explicit ranges.
     * <p>Pairs of (min,max) are expected; length must be even.</p>
     *
     * @param pairs alternating min and max characters
     * @return new {@link CharMask}
     * @throws IllegalArgumentException if odd length
     */
    public static CharMask ofRanges(char... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("PAIRS MUST BE (MIN,MAX) TUPLES");
        }

        Builder builder = builder();

        for (int i = 0; i < pairs.length; i += 2) {
            builder.range(pairs[i], pairs[i + 1]);
        }

        return builder.build();
    }

    private static int baseOf(char character) {
        return (character / WINDOW_SIZE) * WINDOW_SIZE;
    }

    /**
     * ✅ Check membership of a character in this mask.
     *
     * @param character character to test
     * @return {@code true} if present in the mask
     */
    public boolean contains(char character) {
        int    base    = baseOf(character);
        Window current = windows.get(base);

        if (current == null) {
            return false;
        }

        return current.bits.hasAt(character - base);
    }

    /**
     * 🔢 Count the total number of enabled characters in this mask.
     *
     * @return size of the set
     */
    public int size() {
        int size = 0;

        for (Window window : windows.values()) {
            size += window.bits.count();
        }

        return size;
    }

    /**
     * 🧮 Generic window-wise operator over two masks producing a new mask.
     * <p>
     * For each 64-char window <code>base</code>, calls {@link Applier#apply(Window, Window, Window)}
     * with inputs A (this), B (that) and output window. Missing windows are treated as empty.
     * </p>
     *
     * @param that    the other mask (may be {@code null} = empty set)
     * @param applier window-level operator (pure function A,B -> OUT)
     * @return new {@link CharMask} with only non-empty windows
     */
    public CharMask apply(CharMask that, Applier applier) {
        Map<Integer, Window> windows = new HashMap<>();

        // union of bases from both masks
        Set<Integer> bases = new HashSet<>(this.windows.keySet());
        if (that != null) {
            bases.addAll(that.windows.keySet());
        }

        for (Integer base : bases) {
            Window wa = this.windows.get(base);
            Window wb = (that == null ? null : that.windows.get(base));
            Window nw = new Window(base);

            if (wa == null) {
                wa = new Window(base); // ∅
            }

            if (wb == null) {
                wb = new Window(base); // ∅
            }

            applier.apply(wa, wb, nw);

            // keep only non-empty windows
            if (nw.bits.count() > 0) {
                windows.put(base, nw);
            }
        }

        return new CharMask(Map.copyOf(windows));
    }

    /**
     * 🔀 Union (OR).
     * <pre>
     * Formula:  {@code A ∪ B}
     * </pre>
     *
     * @param that other mask
     * @return new mask that contains characters present in either A or B
     */
    public CharMask merge(CharMask that) {
        return apply(that, (wa, wb, nw) -> {
            // A ∪ B
            nw.bits.or(wa.bits);
            nw.bits.or(wb.bits);
        });
    }

    /**
     * ➖ Difference (A \ B).
     * <pre>
     * Formula:  {@code A \ B  =  A ∩ ¬B}
     * </pre>
     *
     * @param that other mask
     * @return new mask that contains characters present in A but not in B
     */
    public CharMask difference(CharMask that) {
        return apply(that, (wa, wb, nw) -> {
            // A \ B = A ∩ ¬B
            Bits nb = Bits.of(0L);
            nb.or(wb.bits);  // copy B
            nb.not();        // ¬B

            nw.bits.or(wa.bits); // copy A
            nw.bits.and(nb);   // A ∩ ¬B
        });
    }

    /**
     * 🤝 Intersection (AND).
     * <pre>
     * Formula:  {@code A ∩ B}
     * </pre>
     *
     * @param that other mask
     * @return new mask that contains characters present in both A and B
     */
    public CharMask intersect(CharMask that) {
        return apply(that, (wa, wb, nw) -> {
            // A ∩ B
            nw.bits.or(wa.bits); // copy A
            nw.bits.and(wb.bits);
        });
    }

    /**
     * ✖️ Symmetric difference (XOR).
     * Formula: {@code A Δ B = (A | B) & ~(A & B)}
     */
    public CharMask xor(CharMask that) {
        return apply(that, (wa, wb, nw) -> {
            // Copy A and B to temporaries to avoid mutating inputs
            Bits ca  = Bits.of(0L);
            Bits cb  = Bits.of(0L);
            Bits aob = Bits.of(0L);
            Bits aab = Bits.of(0L);

            ca.or(wa.bits);
            cb.or(wb.bits);

            // A | B
            aob.or(ca);
            aob.or(cb);

            // A & B
            aab.or(ca);
            aab.and(cb);

            // ~(A & B)
            aab.not();

            // (A | B) & ~(A & B)
            nw.bits.or(aob);
            nw.bits.and(aab);
        });
    }

    /**
     * ✏️ Create a mutable builder from this mask.
     *
     * @return new {@link Builder} initialized with current windows
     */
    public Builder mutate() {
        return new Builder(windows);
    }

    /**
     * 🧩 Window-level operator for {@link #apply(CharMask, Applier)}.
     * <p>
     * Implementations should treat inputs as read-only and write into {@code out}.
     * </p>
     */
    @FunctionalInterface
    public interface Applier {
        /**
         * Apply window-level operation.
         *
         * @param wa   window from A (this), never {@code null}; may be empty
         * @param wb   window from B (that), never {@code null}; may be empty
         * @param nw output window to fill
         */
        void apply(Window wa, Window wb, Window nw);
    }

    /**
     * 🪟 One window = base boundary + Bits for 64 positions.
     */
    public static final class Window {

        final int  base;   // multiple of 64
        final Bits bits;

        Window(int base) {
            this.base = base;
            this.bits = Bits.of(); // empty
        }

        @Override
        public String toString() {
            return "Window[%d]: bits=%s".formatted(base, bits);
        }

    }

    /**
     * 👷 Builder for {@link CharMask}.
     *
     * <h2>Usage</h2>
     * <pre>{@code
     * CharMask.Builder builder = CharMask.builder()
     *     .range('A', 'Z')    // add uppercase letters
     *     .range('a', 'z')    // add lowercase letters
     *     .range('0', '9')    // add digits
     *     .add('-', '_', '.'); // add specific symbols
     *
     * builder.remove('Q'); // remove a single char
     *
     * CharMask mask = builder.build();
     *
     * boolean ok1 = mask.contains('A'); // true
     * boolean ok2 = mask.contains('Q'); // false
     * }</pre>
     *
     * <h2>How windows work</h2>
     * <pre>
     * Unicode space is partitioned into 64-char "windows":
     *
     *   Window base = (c / 64) * 64
     *   Index       = c - base
     *
     * Example: char 'C' (U+0043 = 67)
     *   base  = (67 / 64) * 64 = 64
     *   index = 67 - 64 = 3
     *
     *  Window[64] covers chars U+0040..U+007F
     *
     *  Bits:  0100...
     *         ^
     *         'C'
     * </pre>
     */
    public static final class Builder {
        private final Map<Integer, Window> windows;

        /**
         * 🏗️ Create a builder with existing windows (mutation).
         */
        public Builder(Map<Integer, Window> windows) {
            this.windows = windows;
        }

        /**
         * 🏗️ Create a new empty builder.
         */
        public Builder() {
            this(new HashMap<>());
        }

        /**
         * ➕ Add a single character.
         */
        public Builder add(char character) {
            int    base   = baseOf(character);
            Window window = windowOf(base);
            int    index  = character - base;

            window.bits.add(1L << index);

            return this;
        }

        /**
         * ➕ Add multiple characters at once.
         */
        public Builder set(char... characters) {
            for (char character : characters) {
                add(character);
            }
            return this;
        }

        /**
         * ➕ Add a continuous range [min..max] (inclusive).
         *
         * @param min lower bound
         * @param max upper bound
         * @throws IllegalArgumentException if min > max
         */
        public Builder range(char min, char max) {
            if (min > max) {
                throw new IllegalArgumentException("MIN > MAX: " + (int) min + " .. " + (int) max);
            }

            // iterate with int to avoid char overflow at '\uFFFF'
            for (int current = min; ; current++) {
                add((char) current);
                if (current == max) {
                    break;
                }
            }

            return this;
        }

        /**
         * ➕ Add multiple single characters.
         */
        public Builder addAll(char... characters) {
            for (char character : characters) {
                add(character);
            }
            return this;
        }

        /**
         * 🧯 Remove a single character if present.
         */
        public Builder remove(char character) {
            int    base    = baseOf(character);
            Window current = windows.get(base);

            if (current != null) {
                int index = character - base;
                current.bits.subtract(1L << index);
            }
            return this;
        }

        /**
         * 🏁 Finalize and build immutable {@link CharMask}.
         */
        public CharMask build() {
            return new CharMask(Map.copyOf(windows));
        }

        private Window windowOf(int base) {
            return windows.computeIfAbsent(base > WINDOW_SIZE ? baseOf((char) base) : base, Window::new);
        }
    }
}
