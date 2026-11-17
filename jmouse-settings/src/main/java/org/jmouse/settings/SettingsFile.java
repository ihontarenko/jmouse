package org.jmouse.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 📄 Settings as a file a person keeps, reads and carries to another installation: the rows the store keeps, block by block, as they
 * are kept — never another shape of them. A block is headed by whose its rows are, the owner and the section; a row is its key and
 * its value:
 *
 * <pre>
 * # jmCAD settings · 2026-11-14
 * [reader cad-engine interface/editor/grid]
 * dotPixels = 3
 * [installation cad-gerber files]
 * [0].ending = -F_Cu.gbr
 * </pre>
 *
 * A key is written as {@link ParameterTree} writes it, so a row may begin with a bracket as a head does; what tells them apart is
 * the {@code " = "} every row holds and no head can. A line that starts with {@code #} says something to whoever reads the file. A
 * value's line break is written {@code \n} and its backslash {@code \\}, so a row is one line. Whose a reader's rows are is not
 * written: they are the reader's who imports them.
 */
public final class SettingsFile {

    /** Whose a block's rows are: a reader's own, or the installation's. */
    public enum Holding {
        READER("reader"),
        INSTALLATION("installation");

        private final String word;

        Holding(String word) {
            this.word = word;
        }

        /** How a block's head says it. */
        public String word() {
            return word;
        }

        static Holding of(String word, int line) {
            for (Holding holding : values()) {
                if (holding.word.equals(word)) {
                    return holding;
                }
            }
            throw new SettingRefused("Line " + line + ": a block is the reader's or the installation's, not '" + word + "'.");
        }
    }

    /** One section's rows of one holder, in the order they are written. */
    /**
     * @param rows     the rows kept: what a file carries over and an import takes in
     * @param standing every value of the section as it stands, those kept among them: the rest are written as notes for whoever
     *                 reads the file — the whole section seen —, and a note is never taken in, so a default left alone stays one
     */
    public record Block(Holding holding, String owner, String path, Map<String, String> rows, Map<String, String> standing) {

        public Block {
            rows = Collections.unmodifiableMap(new LinkedHashMap<>(rows));
            standing = Collections.unmodifiableMap(new LinkedHashMap<>(standing));
        }

        /** A block of rows alone, as a file read back has it: its notes are not rows. */
        public Block(Holding holding, String owner, String path, Map<String, String> rows) {
            this(holding, owner, path, rows, Map.of());
        }
    }

    /** What begins a note of a value as it stands, not kept: a line a reader reads and an import passes over. */
    private static final String STANDING = "# ";

    /** What stands between a row's key and its value. */
    private static final String BETWEEN = " = ";

    private SettingsFile() {
    }

    /** The file of these blocks, under a line of words saying what it is. */
    public static String write(String heading, List<Block> blocks) {
        StringBuilder file = new StringBuilder("# ").append(heading.replace('\n', ' ')).append('\n');
        for (Block block : blocks) {
            file.append('[').append(block.holding().word()).append(' ').append(block.owner()).append(' ').append(block.path()).append("]\n");
            block.rows().forEach((key, value) -> file.append(key).append(BETWEEN).append(escaped(value)).append('\n'));
            block.standing().forEach((key, value) -> {
                if (!block.rows().containsKey(key)) {
                    // A setting with no value is shown with nothing after it: a note only shows.
                    file.append(STANDING).append(key).append(BETWEEN).append(escaped(value == null ? "" : value)).append('\n');
                }
            });
        }
        return file.toString();
    }

    /**
     * The blocks of a file, in its order.
     *
     * @throws SettingRefused naming the line that is neither a head, a row, a remark nor empty — then nothing of the file is read
     */
    public static List<Block> read(String text) {
        List<Block> blocks = new ArrayList<>();
        Holding holding = null;
        String owner = null;
        String path = null;
        Map<String, String> rows = new LinkedHashMap<>();
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].strip();
            int number = index + 1;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int between = lines[index].indexOf(BETWEEN);
            if (between < 0 && line.startsWith("[") && line.endsWith("]")) {
                if (holding != null) {
                    blocks.add(new Block(holding, owner, path, rows));
                }
                String[] words = line.substring(1, line.length() - 1).strip().split("\\s+");
                if (words.length != 3) {
                    throw new SettingRefused("Line " + number + ": a block is headed [reader|installation <owner> <section>].");
                }
                holding = Holding.of(words[0], number);
                owner = words[1];
                path = words[2];
                rows = new LinkedHashMap<>();
                continue;
            }
            if (holding == null) {
                throw new SettingRefused("Line " + number + ": a row stands under a block's head.");
            }
            if (between <= 0) {
                throw new SettingRefused("Line " + number + ": a row is written <key> = <value>.");
            }
            rows.put(lines[index].substring(0, between).strip(), unescaped(lines[index].substring(between + BETWEEN.length()), number));
        }
        if (holding != null) {
            blocks.add(new Block(holding, owner, path, rows));
        }
        return List.copyOf(blocks);
    }

    private static String escaped(String value) {
        return value.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n");
    }

    private static String unescaped(String written, int line) {
        StringBuilder value = new StringBuilder();
        for (int at = 0; at < written.length(); at++) {
            char character = written.charAt(at);
            if (character != '\\') {
                value.append(character);
                continue;
            }
            if (at + 1 == written.length()) {
                throw new SettingRefused("Line " + line + ": a backslash ends the value; write \\\\ for one.");
            }
            char next = written.charAt(++at);
            if (next == 'n') {
                value.append('\n');
            } else if (next == '\\') {
                value.append('\\');
            } else {
                throw new SettingRefused("Line " + line + ": \\" + next + " is not written in a value; \\n is a line break, \\\\ a backslash.");
            }
        }
        return value.toString();
    }
}
