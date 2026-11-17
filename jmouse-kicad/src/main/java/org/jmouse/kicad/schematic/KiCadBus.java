package org.jmouse.kicad.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A bus as KiCad names one in a label's or a sheet pin's text — and a text names a bus only when it is written so:
 * <ul>
 *     <li>a vector, {@code PREFIX[START..END]SUFFIX} — {@code D[0..7]}, {@code IRQ-[1..7]} —, whose nets are the prefix, each number
 *     from the start to the end, and the suffix: {@code IRQ-1} … {@code IRQ-7};</li>
 *     <li>a group, {@code NAME{A B C}} or {@code {A B C}}, its members parted by spaces or commas, each a net or a vector; a named
 *     group's nets are its name, a dot and the member — {@code I2C{SDA SCL}} is {@code I2C.SDA}, {@code I2C.SCL} —, an unnamed
 *     group's the members themselves.</li>
 * </ul>
 * A bus joins its nets across a sheet's edge, member by member: a box's pin {@code IRQ-[1..7]} joins each {@code IRQ-n} of the sheet
 * it stands on to the {@code IRQ-n} of the sheet it opens — as KiCad's netlist does (checked with kicad-cli on its ColdFire demo).
 *
 * @param text    the text it is written as
 * @param name    what it is called: a vector's prefix, a group's name — empty for a group with none
 * @param members the names of its nets, in the order written
 */
public record KiCadBus(String text, String name, List<String> members) {

    private static final Pattern VECTOR = Pattern.compile("([^\\s\\[\\]{}]*)\\[(\\d+)\\.\\.(\\d+)]([^\\s\\[\\]{}]*)");
    /** KiCad's text markup — an overbar {@code ~{RESET}}, a superscript {@code ^{2}}, a subscript {@code _{in}} —: braces of a name, no group's. */
    private static final String MARKUP = "~^_";

    /**
     * KiCad's escapes of a character a name may not hold — {@code {slash}}, {@code {space}} … — written in braces too, and no group:
     * a text holding one names a net.
     */
    private static final Set<String> ESCAPES = Set.of("slash", "backslash", "lt", "gt", "colon", "dblquote", "quote", "space", "tab",
            "return", "brace", "bar", "comma", "dollar", "tilde", "caret");

    public KiCadBus {
        members = List.copyOf(members);
    }

    /** The bus a text names, with no alias known; nothing when it names a net. */
    public static Optional<KiCadBus> of(String text) {
        return of(text, Map.of());
    }

    /**
     * The bus a text names, the project's bus aliases known: an alias alone — {@code ETH} — is a bus of its nets, and a group's member
     * that is one — {@code ETH_PI{ETH}} — is each of its nets under the group's name; nothing when the text names a net.
     *
     * @param aliases the project's bus aliases, each name the nets it stands for ({@link org.jmouse.kicad.KiCadProject#busAliases})
     */
    public static Optional<KiCadBus> of(String text, Map<String, List<String>> aliases) {
        String written = text.trim();
        if (aliases.containsKey(written)) {
            return Optional.of(new KiCadBus(text, written, expanded(aliases.get(written))));
        }
        Optional<List<String>> vector = vector(written);
        if (vector.isPresent()) {
            Matcher prefix = VECTOR.matcher(written);
            prefix.matches();
            return Optional.of(new KiCadBus(text, prefix.group(1), vector.get()));
        }
        int open = groupOpening(written);
        if (open < 0 || !written.endsWith("}") || closing(written, open) != written.length() - 1) {
            return Optional.empty();
        }
        String name = written.substring(0, open);
        String inside = written.substring(open + 1, written.length() - 1);
        if (ESCAPES.contains(inside.trim()) || name.chars().anyMatch(character -> Character.isWhitespace(character) || character == '[')) {
            return Optional.empty();
        }
        List<String> members = new ArrayList<>();
        for (String member : membersOf(inside)) {
            List<String> nets = aliases.containsKey(member) ? expanded(aliases.get(member)) : vector(member).orElse(List.of(member));
            for (String net : nets) {
                members.add(name.isEmpty() ? net : name + "." + net);
            }
        }
        return members.isEmpty() ? Optional.empty() : Optional.of(new KiCadBus(text, name, members));
    }

    /** An alias's nets, a vector among them taken as its nets. */
    private static List<String> expanded(List<String> members) {
        List<String> nets = new ArrayList<>();
        members.forEach(member -> nets.addAll(vector(member).orElse(List.of(member))));
        return nets;
    }

    /** Where a group's brace opens: the first brace that is no markup's — {@code SWD{~{RESET} SWDIO}} opens at its fourth character. */
    private static int groupOpening(String written) {
        for (int index = 0; index < written.length(); index++) {
            if (written.charAt(index) == '{') {
                return index > 0 && MARKUP.indexOf(written.charAt(index - 1)) >= 0 ? -1 : index;
            }
        }
        return -1;
    }

    /** Where the brace opened at an index closes, braces inside it counted. */
    private static int closing(String written, int open) {
        int depth = 0;
        for (int index = open; index < written.length(); index++) {
            char character = written.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    /** A group's members, parted by commas or spaces outside any markup's braces: {@code ~{RESET}, SWDIO} is two. */
    private static List<String> membersOf(String inside) {
        List<String> members = new ArrayList<>();
        StringBuilder member = new StringBuilder();
        int depth = 0;
        for (char character : inside.toCharArray()) {
            depth += character == '{' ? 1 : character == '}' ? -1 : 0;
            if (depth == 0 && (character == ',' || Character.isWhitespace(character))) {
                if (!member.isEmpty()) {
                    members.add(member.toString());
                    member.setLength(0);
                }
                continue;
            }
            member.append(character);
        }
        if (!member.isEmpty()) {
            members.add(member.toString());
        }
        return members;
    }

    /** How KiCad writes a vector: {@code D_[0..7]}, its nets {@code D_0} … {@code D_7} — the prefix and each number. */
    public static String vectorText(String prefix, int first, int last) {
        return prefix + "[" + first + ".." + last + "]";
    }

    /** How KiCad writes a group of nets with no name of its own: {@code {SDA SCL}}, its nets those it names. */
    public static String groupText(List<String> members) {
        return "{" + String.join(" ", members) + "}";
    }

    /** Whether a text names a bus rather than a net. */
    public static boolean names(String text) {
        return of(text).isPresent();
    }

    private static Optional<List<String>> vector(String written) {
        Matcher vector = VECTOR.matcher(written);
        if (!vector.matches()) {
            return Optional.empty();
        }
        int from = Integer.parseInt(vector.group(2));
        int to = Integer.parseInt(vector.group(3));
        int step = from <= to ? 1 : -1;
        List<String> nets = new ArrayList<>();
        for (int number = from; number != to + step; number += step) {
            nets.add(vector.group(1) + number + vector.group(4));
        }
        return Optional.of(nets);
    }
}
