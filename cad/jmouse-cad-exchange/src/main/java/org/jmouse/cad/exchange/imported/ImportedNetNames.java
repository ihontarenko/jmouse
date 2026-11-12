package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.circuit.Circuit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Another product's net names as jMX names them: a name a part line can write — {@code +12V} is {@code plus_12V} —, each
 * of the product's nets one jMX net, and the design's reference ground node {@value Circuit#GROUND}. What a product writes
 * into a name that is no part of it — KiCad's sheet path, its {@code Net-(R5-Pad1)} — its import takes off first.
 */
public final class ImportedNetNames {

    /** What a net's name opens with where the product's opens with no letter: {@code 3V3} is {@code net_3V3}. */
    private static final String OPENING = "net_";

    /** How long a jMX net's name is at most. */
    private static final int LONGEST = 32;

    private final UniqueIdentifiers taken = new UniqueIdentifiers(OPENING, LONGEST);

    /** The product's net that is node 0. */
    private final String ground;
    /** What the product's own spelling of a name becomes before it is made a jMX one. */
    private final UnaryOperator<String> local;

    private final Map<String, String> named = new HashMap<>();

    /**
     * @param ground the product's net that is node 0
     * @param local  the product's own spelling taken off a name: {@code /control/IN} is {@code IN} for KiCad
     */
    public ImportedNetNames(String ground, UnaryOperator<String> local) {
        this.ground = ground;
        this.local = local;
    }

    /** The jMX net a product's net is, the same every time it is asked. */
    public String of(String written) {
        return named.computeIfAbsent(written, this::fresh);
    }

    private String fresh(String written) {
        if (written.equals(ground)) {
            return Circuit.GROUND;
        }
        return taken.of(local.apply(written));
    }
}
