package org.jmouse.cad.store.habits;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * What the interface remembers of how its reader works — a column's width, a grid, a layer's colour, a look —, one short value
 * under each key. The keys are the interface's to name, so they are held to a form rather than a list: a group, then dotted
 * lower-case words ({@code pcb.look}, {@code layout.pcb-editor.width}, {@code appearance.palette}); a value is one word, figure
 * or colour, never a document.
 *
 * @param habits each habit's value by its key
 */
public record InterfaceHabits(Map<String, String> habits) {

    /** {@code pcb.colour.classic.top}: a group's word, then dotted lower-case words. */
    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9-]*(\\.[a-z0-9][a-z0-9_-]*)+");

    /** The longest key a habit is kept under. */
    private static final int LONGEST_KEY = 120;

    /** {@code classic}, {@code 0.635}, {@code #c83434}, {@code on}: a word, a figure or a colour, at most 128 characters. */
    private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9#._:/ +-]{0,128}");

    public InterfaceHabits {
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> habit : (habits == null ? Map.<String, String>of() : habits).entrySet()) {
            String key = habit.getKey();
            if (key.length() > LONGEST_KEY || !KEY.matcher(key).matches()) {
                throw new IllegalArgumentException("'" + key + "' is not a habit's key: a group, then dotted lower-case words, as pcb.look.");
            }
            String value = habit.getValue();
            if (value == null || !VALUE.matcher(value).matches()) {
                throw new IllegalArgumentException(key + ": '" + value + "' is not a habit's value — a word, a figure or a colour.");
            }
            sorted.put(key, value);
        }
        habits = Map.copyOf(sorted);
    }
}
