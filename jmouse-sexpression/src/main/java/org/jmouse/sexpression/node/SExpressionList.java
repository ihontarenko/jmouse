package org.jmouse.sexpression.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A list of an S-expression file: {@code (at 62.23 69.85 0)}, {@code (symbol (lib_id "Device:R") …)}. Its first item is, in
 * every list KiCad writes, an atom naming what the list is — its {@link #name()}.
 *
 * @param items          what it holds, in order
 * @param closingLeading the blanks written before its closing parenthesis; null for a list a program built
 */
public record SExpressionList(List<SExpressionNode> items, String closingLeading, SExpressionPosition position, String leading)
        implements SExpressionNode {

    public SExpressionList {
        items = List.copyOf(items);
    }

    /** A list a program built: its name, then its items. */
    public static SExpressionList of(String name, SExpressionNode... items) {
        List<SExpressionNode> all = new ArrayList<>(items.length + 1);
        all.add(SExpressionAtom.of(name));
        all.addAll(List.of(items));
        return new SExpressionList(all, null, SExpressionPosition.NONE, null);
    }

    /** What the list is: its first item when that is an atom, else empty. */
    public String name() {
        return !items.isEmpty() && items.getFirst() instanceof SExpressionAtom atom ? atom.value() : "";
    }

    public int size() {
        return items.size();
    }

    public SExpressionNode item(int index) {
        return items.get(index);
    }

    /** Every list it holds, in order. */
    public List<SExpressionList> lists() {
        return items.stream().filter(SExpressionList.class::isInstance).map(SExpressionList.class::cast).toList();
    }

    /** Its lists of that name, in order. */
    public List<SExpressionList> lists(String name) {
        return items.stream().filter(item -> item instanceof SExpressionList list && list.name().equals(name))
                .map(SExpressionList.class::cast).toList();
    }

    /** Its first list of that name. */
    public Optional<SExpressionList> list(String name) {
        return items.stream().filter(item -> item instanceof SExpressionList list && list.name().equals(name))
                .map(SExpressionList.class::cast).findFirst();
    }

    /**
     * Its first list of that name.
     *
     * @throws IllegalArgumentException naming the list and where it stands, when it has none
     */
    public SExpressionList requireList(String name) {
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
            SExpressionNode item = items.get(index);
            if (item instanceof SExpressionAtom atom) {
                return atom.value();
            }
            if (item instanceof SExpressionString text) {
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
        if (index < items.size() && items.get(index) instanceof SExpressionAtom atom && atom.isNumber()) {
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
        Optional<SExpressionList> flag = list(name);
        if (flag.isPresent()) {
            return flag.get().size() < 2 || flag.get().word(1).equals("yes");
        }
        return items.stream().anyMatch(item -> item instanceof SExpressionAtom atom && atom.value().equals(name));
    }

    /** Its lists of that name, in order. */
    public List<SExpressionList> lists(SExpressionName name) {
        return lists(name.word());
    }

    /** Its first list of that name. */
    public Optional<SExpressionList> list(SExpressionName name) {
        return list(name.word());
    }

    /** Its first list of that name, refused when it has none. */
    public SExpressionList requireList(SExpressionName name) {
        return requireList(name.word());
    }

    /** The word of its list of that name, if it has one. */
    public Optional<String> wordOf(SExpressionName name) {
        return wordOf(name.word());
    }

    /** Whether its list of that name says yes. */
    public boolean says(SExpressionName name) {
        return says(name.word());
    }

    /** Whether it is a list of that name. */
    public boolean is(SExpressionName name) {
        return name().equals(name.word());
    }

    /** A list a program built: its name, then its items. */
    public static SExpressionList of(SExpressionName name, SExpressionNode... items) {
        return of(name.word(), items);
    }

    /** The same list with one more item at its end, laid out as KiCad lays it. */
    public SExpressionList with(SExpressionNode item) {
        List<SExpressionNode> more = new ArrayList<>(items);
        more.add(item);
        return new SExpressionList(more, closingLeading, position, leading);
    }
}
