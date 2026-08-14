package org.jmouse.ai.guard;

import org.jmouse.ai.ToolInvocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * What makes two calls "the same call".
 *
 * <p>Two guards ask two different questions and take the same answer from here: deduplication asks
 * <em>"has this already been done?"</em>, and confirmation asks <em>"is the call being confirmed the
 * call that was previewed?"</em>. One definition of sameness serving both is deliberate — two
 * definitions would eventually disagree, and the disagreement would be a confirmed operation that is
 * not the one that was shown.
 *
 * <p>The arguments are canonicalised before hashing, because a model does not emit its keys in a
 * stable order and two spellings of one intention must not read as two intentions.
 *
 * <p>The control arguments are dropped for the mirror-image reason: {@code allowDuplicate} exists
 * precisely to let an identical call through, so folding it into the hash would make it work by
 * accident rather than by design, and {@code confirm} carries the very token whose validity is being
 * checked.
 *
 * <p>⚠️ <strong>Only the top level is stripped.</strong> A nested object could legitimately hold a
 * field of a user's own called {@code confirm}, and dropping that would let two genuinely different
 * writes hash alike.
 *
 * <p>The caller is part of the hash, so one caller's activity can never suppress or confirm another's
 * — including another caller acting for the same subject.
 */
final class CallFingerprint {

    /** Arguments that steer the guards rather than describe the work, and so are not part of it. */
    private static final Set<String> CONTROL_ARGUMENTS = Set.of(
            ToolInvocation.CONFIRM_ARGUMENT,
            ToolInvocation.ALLOW_DUPLICATE_ARGUMENT);

    /** ASCII unit separator — a delimiter that cannot occur in an argument a client could send. */
    private static final char SEPARATOR = (char) 0x1F;

    private CallFingerprint() {
    }

    static String of(String callerId, String publishedName, Map<String, Object> arguments) {
        StringBuilder canonical = new StringBuilder()
                .append(callerId).append(SEPARATOR)
                .append(publishedName).append(SEPARATOR);

        appendCanonical(withoutControlArguments(arguments), canonical);

        return hash(canonical.toString());
    }

    private static Map<String, Object> withoutControlArguments(Map<String, Object> arguments) {
        Map<String, Object> retained = new TreeMap<>();

        arguments.forEach((name, value) -> {
            if (!CONTROL_ARGUMENTS.contains(name)) {
                retained.put(name, value);
            }
        });

        return retained;
    }

    /**
     * Writes a value in a form that depends only on its content.
     *
     * <p>Hand-written rather than handed to a JSON serialiser, for two reasons a serialiser cannot
     * give: a stable ordering of map keys, which JSON does not promise, and a delimiter that cannot
     * appear inside the data, which a JSON string would not either.
     */
    private static void appendCanonical(Object value, StringBuilder canonical) {
        switch (value) {
            case null -> canonical.append("null");

            case Map<?, ?> entries -> {
                canonical.append('{');
                sortedByKey(entries).forEach((key, entryValue) -> {
                    canonical.append(key).append('=');
                    appendCanonical(entryValue, canonical);
                    canonical.append(SEPARATOR);
                });
                canonical.append('}');
            }

            case Iterable<?> items -> {
                canonical.append('[');
                items.forEach(item -> {
                    appendCanonical(item, canonical);
                    canonical.append(SEPARATOR);
                });
                canonical.append(']');
            }

            default -> canonical.append(value);
        }
    }

    private static Map<String, Object> sortedByKey(Map<?, ?> entries) {
        Map<String, Object> sorted = new TreeMap<>();
        entries.forEach((key, value) -> sorted.put(String.valueOf(key), value));
        return sorted;
    }

    private static String hash(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required of every Java platform", impossible);
        }
    }
}
