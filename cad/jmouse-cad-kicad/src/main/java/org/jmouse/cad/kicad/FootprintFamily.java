package org.jmouse.cad.kicad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every size of a KiCad footprint family written once, and the code of this library's footprint each size is: KiCad names a
 * package by its sizes — {@code Capacitor_THT:CP_Radial_D10.0mm_P5.00mm} —, this library codes it by the same sizes —
 * {@code FP.RADIAL:CP-D10-P5} —, so one entry carries a family both ways in.
 *
 * <p>In the KiCad id, {@code {name}} is a part read — no underscore in it — and {@code {*}} a part passed over. In the code,
 * {@code {name}} writes the part read as it is written, {@code {name|number}} a size as its plain value ({@code 10.00} is {@code 10},
 * {@code 03} is {@code 3}) — not a code, {@code 0207} stays {@code 0207} —, {@code {name|upper}} in capitals, {@code {name|initial}} its first
 * letter in capital ({@code Horizontal} is {@code H}).
 * An id whose sizes this library does not draw finds no footprint by the code, and is converted as KiCad draws it.</p>
 *
 * @param pattern the KiCad id, its parts as named groups
 * @param names   the parts read, in the order the id writes them
 * @param code    the code, its parts as {@code {name}} with an optional filter
 */
record FootprintFamily(Pattern pattern, List<String> names, String code) {

    /** A part of an id read: no underscore, as KiCad separates the sizes of a name. */
    private static final String PART = "([^_:]+?)";

    /** A part of an id passed over. */
    private static final String SKIPPED = ".*?";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z*]+)}");
    private static final Pattern WRITTEN = Pattern.compile("\\{([a-z]+)(?:\\|([a-z]+))?}");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    FootprintFamily {
        names = List.copyOf(names);
    }

    /** Whether a library id is written as a family: it holds a part to read or to pass over. */
    static boolean writes(String libraryId) {
        return PLACEHOLDER.matcher(libraryId).find();
    }

    /** The family a library id written with parts stands for, coded so. */
    static FootprintFamily of(String libraryId, String code) {
        StringBuilder pattern = new StringBuilder();
        List<String> names = new ArrayList<>();
        Matcher placeholder = PLACEHOLDER.matcher(libraryId);
        int written = 0;
        while (placeholder.find()) {
            pattern.append(Pattern.quote(libraryId.substring(written, placeholder.start())));
            String name = placeholder.group(1);
            if (name.equals("*")) {
                pattern.append(SKIPPED);
            } else {
                if (names.contains(name)) {
                    throw new IllegalArgumentException("'" + libraryId + "' reads '" + name + "' twice");
                }
                names.add(name);
                pattern.append(PART);
            }
            written = placeholder.end();
        }
        pattern.append(Pattern.quote(libraryId.substring(written)));
        Matcher used = WRITTEN.matcher(code);
        while (used.find()) {
            if (!names.contains(used.group(1))) {
                throw new IllegalArgumentException("The code '" + code + "' writes '" + used.group(1) + "', which '" + libraryId + "' does not read");
            }
        }
        return new FootprintFamily(Pattern.compile(pattern.toString()), names, code);
    }

    /**
     * The code a library id of this family is, its sizes written in; empty for an id of another family — one the pattern
     * does not match, or one whose size the code writes as a number is none: {@code SOIC-8W} is a wide package, not a count of
     * {@code 8W} leads.
     */
    Optional<String> codeOf(String libraryId) {
        Matcher matcher = pattern.matcher(libraryId);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Matcher used = WRITTEN.matcher(code);
        StringBuilder code = new StringBuilder();
        while (used.find()) {
            String part = matcher.group(names.indexOf(used.group(1)) + 1);
            Optional<String> written = filtered(part, used.group(2));
            if (written.isEmpty()) {
                return Optional.empty();
            }
            used.appendReplacement(code, Matcher.quoteReplacement(written.get()));
        }
        used.appendTail(code);
        return Optional.of(code.toString());
    }

    /** A size as its plain value; none for a part that is no number. */
    private static Optional<String> plain(String part) {
        return NUMBER.matcher(part).matches() ? Optional.of(new BigDecimal(part).stripTrailingZeros().toPlainString()) : Optional.empty();
    }

    private static Optional<String> filtered(String part, String filter) {
        if (filter == null) {
            return Optional.of(part);
        }
        return switch (filter) {
            case "number" -> plain(part);
            case "upper" -> Optional.of(part.toUpperCase(Locale.ROOT));
            case "initial" -> Optional.of(part.isEmpty() ? part : part.substring(0, 1).toUpperCase(Locale.ROOT));
            default -> throw new IllegalArgumentException("A code writes a part as 'number', 'upper' or 'initial', not '" + filter + "'");
        };
    }
}
