package org.jmouse.query.translate.row;

import org.jmouse.query.translate.UnsupportedQueryException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A delimited file, read as rows keyed by whatever its header calls them.
 *
 * <h2>⚠️ This is a row SOURCE, not a backend</h2>
 *
 * <p>What comes out is a list of maps, and from that point on a file is indistinguishable from a list
 * somebody already had — the same {@link RowTranslator} filters, sorts, projects and limits both. The
 * only thing that differs is the mapping, which is where a header name becomes an attribute.</p>
 *
 * <p>That is the whole reason there is no {@code CsvTranslator}: a second implementation of the pipeline
 * would agree with the first until somebody fixed a bug in one of them.</p>
 *
 * <h2>⚠️ Every cell is text</h2>
 *
 * <p>A file carries no types. What a value <em>is</em> comes from the structure, and converting it is the
 * mapping's business — not this reader's. Guessing here would mean a column of order numbers becoming
 * numbers in one file and text in another, decided by whether the first row happened to look numeric.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CsvRows {

    private static final String DEFAULT_DELIMITER = ",";

    private CsvRows() {
    }

    /** The file, with a header line and commas. */
    public static List<Map<String, Object>> of(Path file) {
        return of(file, DEFAULT_DELIMITER, true, StandardCharsets.UTF_8);
    }

    public static List<Map<String, Object>> of(Path file, String delimiter, boolean header) {
        return of(file, delimiter, header, StandardCharsets.UTF_8);
    }

    /**
     * Reads the whole file.
     *
     * @param file      where it is
     * @param delimiter what separates one cell from the next
     * @param header    whether the first line names the columns
     * @param charset   how the bytes are text
     * @return one map per line, keyed by header name — or by position when there is no header
     */
    public static List<Map<String, Object>> of(Path file, String delimiter, boolean header, Charset charset) {
        List<String> lines = read(file, charset);

        if (lines.isEmpty()) {
            return List.of();
        }

        List<String> names = header
                ? cells(lines.getFirst(), delimiter)
                : positions(cells(lines.getFirst(), delimiter).size());

        List<Map<String, Object>> rows  = new ArrayList<>();
        int                       first = header ? 1 : 0;

        for (int index = first; index < lines.size(); index++) {
            String line = lines.get(index);

            if (line.isBlank()) {
                continue;
            }

            rows.add(row(names, cells(line, delimiter)));
        }

        return rows;
    }

    /**
     * ⚠️ A short line is short, and stays short.
     *
     * <p>A missing cell is left absent rather than filled with an empty string, so a filter on it behaves
     * the way a filter on an absent value behaves everywhere else instead of matching {@code ''}.</p>
     */
    private static Map<String, Object> row(List<String> names, List<String> cells) {
        Map<String, Object> row = new LinkedHashMap<>();

        for (int index = 0; index < names.size() && index < cells.size(); index++) {
            row.put(names.get(index), cells.get(index));
        }

        return row;
    }

    /**
     * Splits one line, honouring quotes.
     *
     * <h2>⚠️ Not {@code String.split}, and the difference is a wrong answer rather than a crash</h2>
     *
     * <p>A quoted cell may contain the delimiter — {@code "Насос, великий";NEW} — and splitting on the
     * character first turns one cell into two, shifting every column after it by one. Every row still
     * parses, every field still has a value, and the values belong to the wrong attributes.</p>
     *
     * <p>A doubled quote inside a quoted cell is one quote, which is what every spreadsheet writes.</p>
     */
    private static List<String> cells(String line, String delimiter) {
        List<String>  cells   = new ArrayList<>();
        StringBuilder cell    = new StringBuilder();
        boolean       quoted  = false;
        int           index   = 0;

        while (index < line.length()) {
            char character = line.charAt(index);

            if (character == '"') {
                boolean doubled = quoted && index + 1 < line.length() && line.charAt(index + 1) == '"';

                if (doubled) {
                    cell.append('"');
                    index += 2;

                    continue;
                }

                quoted = !quoted;
                index++;

                continue;
            }

            if (!quoted && line.startsWith(delimiter, index)) {
                cells.add(cell.toString().trim());
                cell.setLength(0);
                index += delimiter.length();

                continue;
            }

            cell.append(character);
            index++;
        }

        cells.add(cell.toString().trim());

        return cells;
    }

    private static List<String> positions(int count) {
        List<String> names = new ArrayList<>();

        for (int index = 1; index <= count; index++) {
            names.add("column" + index);
        }

        return names;
    }

    private static List<String> read(Path file, Charset charset) {
        try {
            return Files.readAllLines(file, charset);
        } catch (IOException unreadable) {
            throw new UnsupportedQueryException(
                    "'%s' cannot be read: %s".formatted(file, unreadable.getMessage()));
        } catch (UncheckedIOException unreadable) {
            throw new UnsupportedQueryException("'%s' cannot be read".formatted(file));
        }
    }
}
