package org.jmouse.storage.spring;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ⏱️ Translates Spring's shorthand durations into the form the settings record binds from.
 *
 * <p>Spring configuration is written {@code 15m}, {@code 1h}, {@code 365d}; a Java {@link Duration}
 * parses {@code PT15M}, {@code PT1H}, {@code P365D}. Both are perfectly ordinary — one is a
 * framework convenience, the other is the JDK — and the mismatch is exactly the kind of thing a
 * framework adapter exists to absorb. Making the library accept the shorthand would put a Spring
 * convention inside a Spring-free module; making products rewrite their values would break the
 * promise that adopting the library renames nothing.</p>
 *
 * <h3>Only where a duration is actually expected</h3>
 *
 * <p>The paths to convert are derived from the settings record itself rather than guessed from the
 * value, so a bucket unluckily named {@code 15m} stays a bucket. Deriving them also means a new
 * duration setting needs no change here: it is found because it is declared.</p>
 */
public final class SpringDurations {

    private static final Pattern SHORTHAND = Pattern.compile("^([+-]?\\d+)\\s*(ns|us|ms|s|m|h|d)$",
                                                             Pattern.CASE_INSENSITIVE);

    private static final String SEPARATOR = ".";

    private SpringDurations() {
    }

    /**
     * ⏱️ The value as a {@link Duration} understands it.
     *
     * <p>A value that is not shorthand — an ISO-8601 form, or anything else — is returned untouched,
     * so a product already writing {@code PT15M} keeps working.</p>
     *
     * @param value the configured value
     * @return the ISO-8601 form, or {@code value} unchanged
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        Matcher matcher = SHORTHAND.matcher(value.trim());

        if (!matcher.matches()) {
            return value;
        }

        long   amount = Long.parseLong(matcher.group(1));
        String unit   = matcher.group(2).toLowerCase(Locale.ROOT);

        return switch (unit) {
            case "ns" -> Duration.ofNanos(amount).toString();
            case "us" -> Duration.ofNanos(amount * 1_000L).toString();
            case "ms" -> Duration.ofMillis(amount).toString();
            case "s" -> Duration.ofSeconds(amount).toString();
            case "m" -> Duration.ofMinutes(amount).toString();
            case "h" -> Duration.ofHours(amount).toString();
            default -> Duration.ofDays(amount).toString();
        };
    }

}
