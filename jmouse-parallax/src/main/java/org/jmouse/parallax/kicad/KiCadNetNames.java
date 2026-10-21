package org.jmouse.parallax.kicad;

import org.jmouse.parallax.object.ObjectDefinition.Circuit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * KiCad's net names as jMX names them: a name a part line can write — {@code +12V} is {@code plus_12V},
 * {@code /control/IN} is {@code IN}, {@code Net-(R5-Pad1)} is {@code net_R5_Pad1} —, each KiCad net one jMX net, and
 * the net KiCad's {@code GND} symbol names node {@value Circuit#GROUND}.
 */
final class KiCadNetNames {

    /** What KiCad's ground symbol names its net. */
    static final String KICAD_GROUND = "GND";

    /** How long a jMX net's name is at most. */
    private static final int LONGEST = 32;

    private final Map<String, String> named = new HashMap<>();
    private final Set<String> taken = new HashSet<>();

    /** The jMX net a KiCad net is, the same every time it is asked. */
    String of(String kicad) {
        return named.computeIfAbsent(kicad, this::fresh);
    }

    private String fresh(String kicad) {
        if (kicad.equals(KICAD_GROUND)) {
            return Circuit.GROUND;
        }
        String name = kicad.replaceAll(".*/", "");
        if (name.startsWith("Net-(")) {
            name = "net_" + name.substring(5).replace(")", "");
        }
        name = name.replaceAll("^\\+", "plus_").replaceAll("^-", "minus_").replace("+", "_plus").replace("~", "not_")
                .replaceAll("[^A-Za-z0-9_]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (name.isEmpty() || !Character.isLetter(name.charAt(0))) {
            name = "net_" + name;
        }
        if (name.length() > LONGEST) {
            name = name.substring(0, LONGEST);
        }
        String unique = name;
        for (int suffix = 2; !taken.add(unique.toLowerCase(Locale.ROOT)); suffix++) {
            String ending = "_" + suffix;
            unique = name.substring(0, Math.min(name.length(), LONGEST - ending.length())) + ending;
        }
        return unique;
    }
}
