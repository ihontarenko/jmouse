package org.jmouse.ipc2581;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The names a file writes, each unique where the standard keys it: designators among designators, part numbers among
 * part numbers, every other name — a layer, a net, a package, a shape, a hole, the step — among the rest. A name is
 * written without the colon the standard keeps for qualifying, and one already taken by another thing gains a suffix;
 * the same thing asked again is answered the same name, so every reference to it agrees.
 */
final class Ipc2581Names {

    /** Which key a name is unique under. */
    enum Kind { DESIGNATOR, PART_NUMBER, NAME }

    private final Map<Kind, Map<String, String>> written = new EnumMap<>(Kind.class);
    private final Map<Kind, Set<String>>         taken   = new EnumMap<>(Kind.class);

    /** What a thing of this kind, called {@code given}, is written as. */
    String of(Kind kind, String given) {
        Map<String, String> names = written.computeIfAbsent(kind, ignored -> new HashMap<>());
        String known = names.get(given);
        if (known != null) {
            return known;
        }
        Set<String> used = taken.computeIfAbsent(kind, ignored -> new HashSet<>());
        String base = given.isBlank() ? "_" : given.replace(':', '_');
        String name = base;
        for (int suffix = 1; used.contains(name); suffix++) {
            name = base + "_" + suffix;
        }
        used.add(name);
        names.put(given, name);
        return name;
    }

    String name(String given) {
        return of(Kind.NAME, given);
    }
}
