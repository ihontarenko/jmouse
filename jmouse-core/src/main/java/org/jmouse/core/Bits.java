package org.jmouse.core;

import java.util.Objects;

/**
 * 🔢 Lightweight 64-bit flag set based on a {@code long} bitmask.
 *
 * <p>Use {@link #of(long...)} to build a mask from flag constants, then
 * toggle, add, subtract, combine, and query flags efficiently.</p>
 *
 * <h2>Highlights</h2>
 * <ul>
 *   <li>Up to 64 flags (bits 0..63) ✅</li>
 *   <li>Fast bitwise operations (OR/AND/XOR/NOT) ⚡</li>
 *   <li>Ergonomic checks: {@link #has(Number)}, {@link #any(Number...)} ❓</li>
 *   <li>Convenient index helpers: {@link #toggle(int)}, {@link #hasAt(int)} 🎯</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // 1) Define flag constants (one-hot bits)
 * public static final long HANDLER_MAPPING        = 1L << 0;
 * public static final long HANDLER_ADAPTER        = 1L << 1;
 * public static final long ARGUMENT_RESOLVER      = 1L << 2;
 * public static final long RETURN_VALUE_HANDLER   = 1L << 3;
 * public static final long RETURN_VALUE_PROCESSOR = 1L << 4;
 *
 * // 2) Create a bitmask
 * Bits mask = Bits.of(HANDLER_MAPPING, ARGUMENT_RESOLVER);
 *
 * // 3) Enable/disable flags
 * mask.add(HANDLER_ADAPTER);              // turn on
 * mask.subtract(HANDLER_MAPPING);         // turn off
 *
 * // 4) Queries
 * boolean hasAdapter = mask.has(HANDLER_ADAPTER);               // true
 * boolean anyCore    = mask.any(HANDLER_MAPPING, RETURN_VALUE_HANDLER); // false
 * boolean empty      = mask.isEmpty();                           // false
 * int     enabled    = mask.count();                             // number of set bits
 *
 * // 5) Bit by index (0..63)
 * mask.toggle(5);                 // flip bit #5
 * boolean on = mask.hasAt(5);     // check bit #5
 *
 * // 6) Combine with other masks
 * Bits other = Bits.of(RETURN_VALUE_PROCESSOR);
 * mask.or(other);  // mask = mask OR other
 * mask.and(other); // mask = mask AND other
 * mask.xor(other); // mask = mask XOR other
 * mask.not();      // invert all 64 bits
 *
 * // 7) Reset
 * mask.clear(); // -> 0
 * }</pre>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Bit index range is {@code [0..63]}.</li>
 *   <li>Instances are <b>not</b> thread-safe; add synchronization if shared.</li>
 * </ul>
 *
 * @author Ivan
 */
public final class Bits {

    private long mask;

    private Bits(long initial) {
        this.mask = initial;
    }

    /**
     * 🧮 Build a {@code Bits} covering a continuous range of bits.
     * <p>
     * Example: {@code ofRange(2, 5)} produces a mask covering
     * bit positions 2, 3, 4, 5.
     * </p>
     *
     * @param min lowest bit index (inclusive, 0-based)
     * @param max highest bit index (inclusive, 0-based)
     * @return new {@code Bits} containing all bits from {@code min} to {@code max}
     * @throws IllegalArgumentException if {@code min > max} or indices are negative
     */
    public static Bits ofRange(int min, int max) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("Bit indices must be non-negative");
        }
        if (min > max) {
            throw new IllegalArgumentException("min cannot be greater than max");
        }

        long mask = 0L;

        for (int i = min; i <= max; i++) {
            mask |= (1L << i);
        }

        return new Bits(mask);
    }

    /**
     * Build a {@code Bits} from the OR of provided flag values.
     *
     * @param flags flag constants (each should be one-hot, e.g. {@code 1L << n})
     * @return new {@code Bits} containing the union of {@code flags}
     */
    public static Bits of(long... flags) {
        long mask = 0L;

        if (flags != null) {
            for (long flag : flags) {
                mask |= flag;
            }
        }

        return new Bits(mask);
    }

    /**
     * OR current mask with the given flag value.
     */
    public void add(Number value) {
        mask |= value.longValue();
    }

    /**
     * AND current mask with the negation of the given flag value (clear bits).
     */
    public void subtract(Number value) {
        mask &= ~value.longValue();
    }

    /**
     * @return true if all bits of {@code value} are set in this mask.
     */
    public boolean has(Number value) {
        return (mask & value.longValue()) != 0;
    }

    /**
     * @return true if at least one of the provided values is present.
     */
    public boolean any(Number... values) {
        for (Number value : values) {
            if (has(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if mask is zero.
     */
    public boolean isEmpty() {
        return this.mask == 0L;
    }

    /**
     * Reset mask to zero.
     */
    public void clear() {
        this.mask = 0L;
    }

    /**
     * @return number of set bits (population count).
     */
    public int count() {
        return Long.bitCount(this.mask);
    }

    /**
     * Flip bit at {@code index} (0..63).
     */
    public void toggle(int index) {
        this.mask ^= (1L << index);
    }

    /**
     * @return true if bit at {@code index} (0..63) is set.
     */
    public boolean hasAt(int index) {
        return (this.mask & (1L << index)) != 0;
    }

    /**
     * Bitwise OR with another {@code Bits}. Returns {@code this} for chaining.
     */
    public Bits or(Bits other) {
        if (other != null) this.mask |= other.mask;
        return this;
    }

    /**
     * Bitwise AND with another {@code Bits}. Mutates this mask.
     */
    public void and(Bits other) {
        this.mask &= other.mask;
    }

    /**
     * Bitwise XOR with another {@code Bits}. Mutates this mask.
     */
    public void xor(Bits other) {
        this.mask ^= other.mask;
    }

    /**
     * Bitwise NOT (invert all 64 bits). Mutates this mask.
     */
    public void not() {
        this.mask = ~this.mask;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Bits bits)) return false;

        return mask == bits.mask;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mask);
    }

    @Override
    public String toString() {
        return "Bits[%s]".formatted(Long.toBinaryString(mask));
    }
}
