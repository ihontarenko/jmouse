package org.jmouse.libre.pcb;

import java.util.Map;

/** What the format lets words stand for, as it writes it. */
public enum LibrePcbPlaceholder {

    NAME("{{NAME}}"),
    VALUE("{{VALUE}}"),
    PROJECT("{{PROJECT}}"),
    AUTHOR("{{AUTHOR}}"),
    VERSION("{{VERSION}}"),
    BOARD("{{BOARD}}");

    private final String written;

    LibrePcbPlaceholder(String written) {
        this.written = written;
    }

    /** Whether words are this and nothing else. */
    public boolean isAll(String words) {
        return written.equals(words.strip());
    }

    /** Words with what the placeholders given stand for written in. */
    public static String filled(String words, Map<LibrePcbPlaceholder, String> values) {
        String filled = words;
        for (Map.Entry<LibrePcbPlaceholder, String> value : values.entrySet()) {
            filled = filled.replace(value.getKey().written, value.getValue());
        }
        return filled;
    }
}
