package org.jmouse.parallax.spice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A netlist with its includes written in, before it is parsed: {@code .include file} (or {@code .inc}) becomes the
 * file's text, and {@code .lib file section} the lines between {@code .lib section} and {@code .endl} in that file —
 * each written in again in the same way, a few levels deep. A file that was not given is left as its line, for the
 * analyzer to name in a note; so is a section the file does not have.
 */
final class IncludeExpansion {

    /** How deep includes may nest: a library that includes itself stops here instead of forever. */
    private static final int DEEPEST = 8;

    private final SpiceIncludes includes;

    IncludeExpansion(SpiceIncludes includes) {
        this.includes = includes;
    }

    String expand(String text) {
        return expand(text, 0);
    }

    private String expand(String text, int depth) {
        if (depth >= DEEPEST) {
            return text;
        }
        StringBuilder expanded = new StringBuilder();
        for (String line : text.split("\\R", -1)) {
            expanded.append(included(line, depth).orElse(line)).append('\n');
        }
        return expanded.toString();
    }

    /** What one line becomes when it is an include of a file that was given; empty when it stays as it is. */
    private Optional<String> included(String line, int depth) {
        List<String> words = words(line);
        if (words.isEmpty()) {
            return Optional.empty();
        }
        String directive = words.getFirst().toLowerCase(Locale.ROOT);
        if ((directive.equals(".include") || directive.equals(".inc")) && words.size() >= 2) {
            return includes.text(words.get(1)).map(file -> expand(file, depth + 1));
        }
        if (directive.equals(".lib") && words.size() >= 3) {
            return includes.text(words.get(1)).flatMap(file -> section(file, words.get(2)))
                    .map(section -> expand(section, depth + 1));
        }
        return Optional.empty();
    }

    /** The lines of a library file between {@code .lib section} and its {@code .endl}. */
    private static Optional<String> section(String file, String name) {
        StringBuilder lines = new StringBuilder();
        boolean inside = false;
        for (String line : file.split("\\R")) {
            List<String> words = words(line);
            String directive = words.isEmpty() ? "" : words.getFirst().toLowerCase(Locale.ROOT);
            if (!inside && directive.equals(".lib") && words.size() == 2 && words.get(1).equalsIgnoreCase(name)) {
                inside = true;
            } else if (inside && directive.equals(".endl")) {
                return Optional.of(lines.toString());
            } else if (inside) {
                lines.append(line).append('\n');
            }
        }
        return Optional.empty();
    }

    /** A line's words, quotes kept on a quoted path. */
    private static List<String> words(String line) {
        List<String> words = new ArrayList<>();
        for (String word : line.trim().split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }
}
