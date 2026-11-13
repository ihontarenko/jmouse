package org.jmouse.settings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 🌳 A structured value unfolded into its parameters, one key a value, and folded back: a record's field is a word
 * ({@code circuit.gap}), a list's item its index ({@code papers.0.width}), a map's key that is not one plain word — it holds a
 * dot, or is all digits — in brackets ({@code habits[pcb.look]}), so it is never taken for depth or for an index. An empty record
 * is one parameter of {@value #EMPTY_RECORD}, an empty list one of {@value #EMPTY_LIST}, a null one with no value.
 *
 * <p>The tree is plain Java — maps, lists, text, numbers, switches, nulls —, so the library reads no format: whoever declares a
 * section turns its source into such a tree, and reads a folded tree back into its record ({@link SectionReader}). Folded, every
 * value is text; the reader takes it as the type its record says.</p>
 */
public final class ParameterTree {

    /** An empty record's value. */
    public static final String EMPTY_RECORD = "{}";

    /** An empty list's value. */
    public static final String EMPTY_LIST = "[]";

    private ParameterTree() {
    }

    /** One word of a key: a map's key, or a list's index. */
    public record Segment(String word, boolean index) {

        /** How it is written in a key, after a dot unless it is the first; in brackets where a plain word would mislead. */
        String written(boolean first) {
            if (index) {
                return (first ? "" : ".") + word;
            }
            if (word.isEmpty() || word.chars().allMatch(Character::isDigit) || word.contains(".")) {
                return "[" + word + "]";
            }
            return (first ? "" : ".") + word;
        }
    }

    /** The parameters of a tree: each value under its key, in the tree's order. */
    public static Map<String, String> unfold(Object tree) {
        Map<String, String> parameters = new LinkedHashMap<>();
        unfold(tree, new ArrayList<>(), parameters);
        return parameters;
    }

    private static void unfold(Object node, List<Segment> path, Map<String, String> parameters) {
        switch (node) {
            case Map<?, ?> record when record.isEmpty() -> parameters.put(keyOf(path), EMPTY_RECORD);
            case Map<?, ?> record -> record.forEach((name, value) -> {
                String word = String.valueOf(name);
                if (word.contains("[") || word.contains("]")) {
                    throw new IllegalArgumentException("A key's word holds no bracket: '" + word + "'");
                }
                path.add(new Segment(word, false));
                unfold(value, path, parameters);
                path.removeLast();
            });
            case List<?> list when list.isEmpty() -> parameters.put(keyOf(path), EMPTY_LIST);
            case List<?> list -> {
                for (int index = 0; index < list.size(); index++) {
                    path.add(new Segment(String.valueOf(index), true));
                    unfold(list.get(index), path, parameters);
                    path.removeLast();
                }
            }
            case null -> parameters.put(keyOf(path), null);
            case Double number -> parameters.put(keyOf(path), BigDecimal.valueOf(number).stripTrailingZeros().toPlainString());
            case Float number -> parameters.put(keyOf(path), new BigDecimal(number.toString()).stripTrailingZeros().toPlainString());
            case BigDecimal number -> parameters.put(keyOf(path), number.stripTrailingZeros().toPlainString());
            default -> parameters.put(keyOf(path), String.valueOf(node));
        }
    }

    /** A key written from its words. */
    public static String keyOf(List<Segment> path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("A value is a parameter only under a key: a section's root is a record or a list.");
        }
        StringBuilder key = new StringBuilder();
        for (int position = 0; position < path.size(); position++) {
            key.append(path.get(position).written(position == 0));
        }
        return key.toString();
    }

    /** A key's words: {@code papers.0.width} → papers, index 0, width; {@code habits[pcb.look]} → habits, pcb.look. */
    public static List<Segment> segmentsOf(String key) {
        List<Segment> segments = new ArrayList<>();
        int position = 0;
        while (position < key.length()) {
            char at = key.charAt(position);
            if (at == '.') {
                position++;
                continue;
            }
            if (at == '[') {
                int close = key.indexOf(']', position);
                if (close < 0) {
                    throw new IllegalArgumentException("'" + key + "' opens a bracket it does not close.");
                }
                segments.add(new Segment(key.substring(position + 1, close), false));
                position = close + 1;
                continue;
            }
            int end = position;
            while (end < key.length() && key.charAt(end) != '.' && key.charAt(end) != '[') {
                end++;
            }
            String word = key.substring(position, end);
            segments.add(new Segment(word, word.chars().allMatch(Character::isDigit)));
            position = end;
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("A parameter's key holds a word: '" + key + "'");
        }
        return segments;
    }

    /**
     * The tree the parameters make: maps, lists in their indices' order, and every value as text — {@value #EMPTY_RECORD} and
     * {@value #EMPTY_LIST} an empty record and list, no value a null.
     *
     * @throws IllegalArgumentException where two keys say one place is both a record and a list, or both a value and more
     */
    public static Object fold(Map<String, String> parameters) {
        Node root = null;
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            List<Segment> segments = segmentsOf(parameter.getKey());
            root = Node.place(root, segments, 0, parameter.getValue(), parameter.getKey());
        }
        return root == null ? new LinkedHashMap<>() : root.tree();
    }

    /** A place of the tree as it is being folded. */
    private sealed interface Node {

        Object tree();

        static Node place(Node node, List<Segment> segments, int depth, String value, String key) {
            if (depth == segments.size()) {
                // An empty record or list only says nothing is there: what another key holds there stands.
                if (node instanceof Branches branches && (EMPTY_RECORD.equals(value) && !branches.list || EMPTY_LIST.equals(value) && branches.list)) {
                    return branches;
                }
                if (node != null) {
                    throw new IllegalArgumentException("'" + key + "' is a value where another key holds more.");
                }
                return new Leaf(value);
            }
            if (node instanceof Leaf leaf && (EMPTY_RECORD.equals(leaf.value()) || EMPTY_LIST.equals(leaf.value()))) {
                node = null;
            }
            Segment segment = segments.get(depth);
            if (segment.index()) {
                Branches list = node == null ? new Branches(true) : requireBranches(node, true, key);
                list.children.put(segment.word(), place(list.children.get(segment.word()), segments, depth + 1, value, key));
                return list;
            }
            Branches record = node == null ? new Branches(false) : requireBranches(node, false, key);
            record.children.put(segment.word(), place(record.children.get(segment.word()), segments, depth + 1, value, key));
            return record;
        }

        private static Branches requireBranches(Node node, boolean list, String key) {
            if (node instanceof Branches branches && branches.list == list) {
                return branches;
            }
            throw new IllegalArgumentException("'" + key + "' says a place is " + (list ? "a list" : "a record") + " that another key says is not.");
        }
    }

    private record Leaf(String value) implements Node {

        @Override
        public Object tree() {
            if (EMPTY_RECORD.equals(value)) {
                return new LinkedHashMap<>();
            }
            if (EMPTY_LIST.equals(value)) {
                return new ArrayList<>();
            }
            return value;
        }
    }

    private static final class Branches implements Node {

        private final boolean           list;
        /** A list's children by index, in its order; a record's by name, in theirs. */
        private final Map<String, Node> children;

        private Branches(boolean list) {
            this.list = list;
            this.children = list ? new TreeMap<>((one, other) -> Integer.compare(Integer.parseInt(one), Integer.parseInt(other)))
                                 : new LinkedHashMap<>();
        }

        @Override
        public Object tree() {
            if (list) {
                List<Object> items = new ArrayList<>();
                children.values().forEach(child -> items.add(child.tree()));
                return items;
            }
            Map<String, Object> record = new LinkedHashMap<>();
            children.forEach((name, child) -> record.put(name, child.tree()));
            return record;
        }
    }
}
