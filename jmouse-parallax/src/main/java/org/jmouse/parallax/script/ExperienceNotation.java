package org.jmouse.parallax.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * What an author writes and jMS's ASCII lexer does not read, turned into what it does before the lexer sees the text:
 * datasheet symbols ({@code Ω}, {@code µ}, {@code ±}, {@code °}) and {@code """ … """} text that spans lines.
 *
 * <p>Text inside quotes is left as written, so a pin labelled {@code "−"} keeps its minus and a URL keeps its
 * {@code #fragment}.</p>
 *
 * <h2>⚠️ Long text keeps the line numbers</h2>
 *
 * <p>The lexer has no multi-line string. A {@code """ … """} block is taken out into {@link Normalized#longTexts()}
 * and replaced by a one-line placeholder string, followed by as many line breaks as the block had — so every line
 * after it keeps the number the author sees in the editor.</p>
 */
public final class ExperienceNotation {

    /** Starts a placeholder for a long text: a control character no author writes. */
    public static final char LONG_TEXT_MARK = '\u0001';

    private static final Map<Character, String> REPLACEMENTS = Map.ofEntries(
            Map.entry('Ω', "Ohm"), Map.entry('µ', "u"), Map.entry('μ', "u"), Map.entry('±', "+-"),
            Map.entry('−', "-"), Map.entry('–', "-"), Map.entry('²', "^2"), Map.entry('³', "^3"),
            Map.entry('×', "*"), Map.entry('·', "*"), Map.entry('°', "deg"), Map.entry('π', "pi"));

    private ExperienceNotation() {
    }

    /**
     * @param source    the text the lexer reads
     * @param longTexts the {@code """ … """} blocks, dedented, in the order their placeholders appear
     * @param original  the text as the author wrote it
     * @param origins   for every position of {@code source}, and one past its end, the position in {@code original}
     *                  it came from — so an expression is cut out of what the author wrote ({@code x²}, {@code kΩ}),
     *                  not out of what the lexer was given ({@code x^2}, {@code kOhm})
     */
    public record Normalized(String source, List<String> longTexts, String original, int[] origins) {

        /** The author's text between two positions of the normalized one. */
        public String originalBetween(int start, int end) {
            return original.substring(origins[start], origins[end]);
        }

        /** Where a position of the normalized text lies in the author's. */
        public int originOf(int position) {
            return origins[Math.max(0, Math.min(position, origins.length - 1))];
        }
    }

    public static Normalized normalize(String text) {
        StringBuilder normalized = new StringBuilder(text.length());
        Origins origins = new Origins(text.length());
        List<String> longTexts = new ArrayList<>();
        char quote = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                }
                normalized.append(character);
                origins.record(index, 1);
            } else if (text.startsWith("\"\"\"", index)) {
                int end = text.indexOf("\"\"\"", index + 3);
                if (end < 0) {
                    end = text.length();
                }
                String body = text.substring(index + 3, end);
                longTexts.add(dedent(body));
                int before = normalized.length();
                normalized.append('"').append(LONG_TEXT_MARK).append(longTexts.size() - 1).append('"');
                normalized.append("\n".repeat((int) body.chars().filter(symbol -> symbol == '\n').count()));
                origins.record(index, normalized.length() - before);
                index = Math.min(text.length(), end + 3) - 1;
            } else if (character == '"' || character == '\'') {
                quote = character;
                normalized.append(character);
                origins.record(index, 1);
            } else if (character == '#') {
                int end = text.indexOf('\n', index);
                end = end < 0 ? text.length() : end;
                normalized.append(text, index, end);
                for (int commented = index; commented < end; commented++) {
                    origins.record(commented, 1);
                }
                index = end - 1;
            } else {
                String replacement = REPLACEMENTS.getOrDefault(character, String.valueOf(character));
                normalized.append(replacement);
                origins.record(index, replacement.length());
            }
        }
        return new Normalized(normalized.toString(), List.copyOf(longTexts), text, origins.finish(text.length()));
    }

    /** The position each normalized character came from, written as the normalized text grows. */
    private static final class Origins {

        private int[] positions;
        private int   size;

        Origins(int expected) {
            positions = new int[expected + 16];
        }

        /** {@code count} normalized characters that all came from the original character at {@code origin}. */
        void record(int origin, int count) {
            for (int written = 0; written < count; written++) {
                if (size == positions.length) {
                    positions = Arrays.copyOf(positions, positions.length * 2);
                }
                positions[size++] = origin;
            }
        }

        /** The positions, with one more for the end of the text. */
        int[] finish(int originalLength) {
            int[] finished = Arrays.copyOf(positions, size + 1);
            finished[size] = originalLength;
            return finished;
        }
    }

    /** Removes the indentation every non-blank line shares, and the blank lines around the text. */
    public static String dedent(String body) {
        List<String> lines = new ArrayList<>(List.of(body.split("\n", -1)));
        while (!lines.isEmpty() && lines.getFirst().isBlank()) {
            lines.removeFirst();
        }
        while (!lines.isEmpty() && lines.getLast().isBlank()) {
            lines.removeLast();
        }
        int indentation = lines.stream().filter(line -> !line.isBlank())
                .mapToInt(line -> line.length() - line.stripLeading().length()).min().orElse(0);
        return String.join("\n", lines.stream()
                .map(line -> line.isBlank() ? "" : line.substring(indentation).stripTrailing()).toList());
    }
}
