package org.jmouse.kicad.parser.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A list of a KiCad file: {@code (at 62.23 69.85 0)}, {@code (symbol (lib_id "Device:R") …)}. Its first item is, in
 * every list KiCad writes, an atom naming what the list is — its {@link #name()}.
 *
 * @param items          what it holds, in order
 * @param closingLeading the blanks written before its closing parenthesis; null for a list a program built
 */
public record KiCadList(List<KiCadNode> items, String closingLeading, KiCadPosition position, String leading)
        implements KiCadNode {

    public KiCadList {
        items = List.copyOf(items);
    }

    /** A list a program built: its name, then its items. */
    public static KiCadList of(String name, KiCadNode... items) {
        List<KiCadNode> all = new ArrayList<>(items.length + 1);
        all.add(KiCadAtom.of(name));
        all.addAll(List.of(items));
        return new KiCadList(all, null, KiCadPosition.NONE, null);
    }

    /** What the list is: its first item when that is an atom, else empty. */
    public String name() {
        return !items.isEmpty() && items.getFirst() instanceof KiCadAtom atom ? atom.value() : "";
    }

    public int size() {
        return items.size();
    }

    public KiCadNode item(int index) {
        return items.get(index);
    }

    /** Its lists of that name, in order. */
    public List<KiCadList> lists(String name) {
        return items.stream().filter(item -> item instanceof KiCadList list && list.name().equals(name))
                .map(KiCadList.class::cast).toList();
    }

    /** Its first list of that name. */
    public Optional<KiCadList> list(String name) {
        return items.stream().filter(item -> item instanceof KiCadList list && list.name().equals(name))
                .map(KiCadList.class::cast).findFirst();
    }

    /**
     * Its first list of that name.
     *
     * @throws IllegalArgumentException naming the list and where it stands, when it has none
     */
    public KiCadList requireList(String name) {
        return list(name).orElseThrow(() -> new IllegalArgumentException(
                "(" + name() + ") at " + position + " has no (" + name + ")."));
    }

    /**
     * The word at an index — an atom's value or a text's words —, the name being index 0.
     *
     * @throws IllegalArgumentException when there is no word there
     */
    public String word(int index) {
        if (index < items.size()) {
            KiCadNode item = items.get(index);
            if (item instanceof KiCadAtom atom) {
                return atom.value();
            }
            if (item instanceof KiCadString text) {
                return text.text();
            }
        }
        throw new IllegalArgumentException("(" + name() + ") at " + position + " has no word at " + index + ".");
    }

    /**
     * The number at an index.
     *
     * @throws IllegalArgumentException when there is no number there
     */
    public double number(int index) {
        if (index < items.size() && items.get(index) instanceof KiCadAtom atom && atom.isNumber()) {
            return atom.number();
        }
        throw new IllegalArgumentException("(" + name() + ") at " + position + " has no number at " + index + ".");
    }

    /** The word of its list of that name — {@code (lib_id "Device:R")} gives {@code Device:R} —, if it has one. */
    public Optional<String> wordOf(String name) {
        return list(name).filter(list -> list.size() > 1).map(list -> list.word(1));
    }

    /** Whether its list of that name says yes — {@code (hide yes)}, or KiCad 7's bare {@code hide} — . */
    public boolean says(String name) {
        Optional<KiCadList> flag = list(name);
        if (flag.isPresent()) {
            return flag.get().size() < 2 || flag.get().word(1).equals("yes");
        }
        return items.stream().anyMatch(item -> item instanceof KiCadAtom atom && atom.value().equals(name));
    }

    /** The same list with one more item at its end, laid out as KiCad lays it. */
    public KiCadList with(KiCadNode item) {
        List<KiCadNode> more = new ArrayList<>(items);
        more.add(item);
        return new KiCadList(more, closingLeading, position, leading);
    }
}
