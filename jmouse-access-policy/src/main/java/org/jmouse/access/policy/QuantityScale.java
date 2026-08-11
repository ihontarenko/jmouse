package org.jmouse.access.policy;

/**
 * Turns an amount as written in a document into a number — the seam a unit system plugs into.
 *
 * <h2>⚠️ Why the engine does not own this</h2>
 *
 * <p>Two reasons, and the second is the one that matters.
 *
 * <p><strong>Units are product knowledge.</strong> {@code 100GB} means something for a byte quota and
 * nothing at all for automation runs or story points. A library that resolved suffixes would be
 * asserting what its adopters count in, which is precisely the assertion this whole axis exists not to
 * make — and the next product's layers are boards and sprints.
 *
 * <p><strong>And the implementation already exists, one dependency away.</strong> {@code jmouse-core}
 * has {@code Bytes.parse}, but {@code jmouse-access-policy}'s pom is deliberately almost
 * dependency-free — <em>"not even jmouse-core"</em> — because core brings byte-buddy and objenesis
 * with it, and every product adopting the policy module would inherit a proxy engine to read one
 * number. The bar stated there is *does the engine stop working without it*, and for a units table the
 * answer is no. So the product supplies it, from a classpath where core is already present.
 *
 * <p>The default handles whole numbers with optional {@code _} separators, which is everything a
 * capability needs unless it is measured in something.
 */
@FunctionalInterface
public interface QuantityScale {

    /** Whole numbers only — what a product registering nothing gets. */
    QuantityScale PLAIN = written -> {
        try {
            return Long.parseLong(written.replace("_", ""));
        } catch (NumberFormatException notANumber) {
            throw new PolicyException(
                    "'" + written + "' is not an amount. Write a whole number, or '"
                    + Allowances.UNLIMITED + "' for no ceiling at all — which is a different fact, and "
                    + "every screen renders it differently.");
        }
    };

    /**
     * ⚠️ <strong>An implementation must still read a plain number.</strong> One document mixes
     * capabilities that are measured with ones that are merely counted — {@code storage-byte 100GB}
     * sits three lines from {@code workspace 25} — so a scale that only understood units would refuse
     * every count in the file. Handle the suffix where there is one and fall back to a whole number
     * where there is not; {@link #PLAIN} is the fallback, and delegating to it is the intended shape:
     *
     * <pre>{@code
     * QuantityScale bytes = written -> Character.isDigit(written.charAt(written.length() - 1))
     *         ? QuantityScale.PLAIN.resolve(written)
     *         : Bytes.parse(written).getBytes();
     * }</pre>
     *
     * @param written the amount exactly as the file wrote it, trimmed
     * @return the number it denotes
     * @throws PolicyException naming what was written, where it does not denote one
     */
    long resolve(String written);
}
