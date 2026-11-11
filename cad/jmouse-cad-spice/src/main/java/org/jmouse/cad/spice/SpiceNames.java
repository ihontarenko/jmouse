package org.jmouse.cad.spice;

import org.jmouse.cad.circuit.Circuit;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * SPICE's node and element names made names jMX reads as one word — {@code [A-Za-z_][A-Za-z0-9_]*} —, kept distinct, each
 * renaming remembered so a note can list them. Ground stays ground.
 */
final class SpiceNames {

    /** The longest name a renaming leaves. */
    private static final int LONGEST = 32;

    private final Map<String, String> chosen  = new LinkedHashMap<>();
    private final Map<String, String> renamed = new LinkedHashMap<>();
    private final Set<String>         taken   = new HashSet<>();
    private final String prefix;

    /** @param prefix what a name that starts with a digit is given in front: {@code n} for a node */
    SpiceNames(String prefix) {
        this.prefix = prefix;
    }

    String of(String written) {
        if (written.equals(Circuit.GROUND)) {
            return Circuit.GROUND;
        }
        String known = chosen.get(written);
        if (known != null) {
            return known;
        }
        String name = written.replaceAll("[^A-Za-z0-9_]", "_");
        if (name.isEmpty() || !Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
            name = prefix + name;
        }
        if (name.length() > LONGEST) {
            name = name.substring(0, LONGEST);
        }
        String candidate = name;
        int counter = 2;
        while (taken.contains(candidate)) {
            candidate = name + "_" + counter;
            counter += 1;
        }
        taken.add(candidate);
        chosen.put(written, candidate);
        if (!candidate.equals(written)) {
            renamed.put(written, candidate);
        }
        return candidate;
    }

    /** Each name that had to change → what it became, in the order met. */
    Map<String, String> renamed() {
        return renamed;
    }
}
