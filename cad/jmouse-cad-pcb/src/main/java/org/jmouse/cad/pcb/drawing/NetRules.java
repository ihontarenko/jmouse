package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbVocabulary;

import java.util.Map;
import java.util.Optional;

/**
 * What a pcb's rules give a net: the gap other copper keeps from it, the width it is laid with, the via it changes layer
 * through — its class's where it is in one, never less than the pcb's own. One reading for the checks and for the router,
 * so what is laid is what is checked.
 *
 * @param values the values the pcb is held to, by rule word
 */
public record NetRules(PcbDrawing pcb, Map<String, Double> values) {

    public NetRules {
        values = Map.copyOf(values);
    }

    /** A rule's value; 0 where the rules give none. */
    public double value(String word) {
        return values.getOrDefault(word, 0.0);
    }

    /** The class a net is in; empty for none, or for copper on no net. */
    public Optional<NetClass> classOf(Optional<String> net) {
        return net.flatMap(name -> pcb.netClasses().stream().filter(netClass -> netClass.nets().contains(name)).findFirst());
    }

    /** How far other copper keeps from a net's: its class's clearance, never less than the pcb's. */
    public double clearanceOf(Optional<String> net) {
        return Math.max(value(PcbVocabulary.RULE_CLEARANCE), classOf(net).map(NetClass::clearance).orElse(0.0));
    }

    /**
     * The gap a zone's pour keeps from other nets' copper: its own where it says one, else its net's. The gap kept from a piece of
     * another net is the wider of this and that net's own.
     */
    public double zoneGapOf(Zone zone) {
        return zone.clearance() > 0 ? zone.clearance() : clearanceOf(Optional.of(zone.net()));
    }

    /** The track a net is laid with: its pair's width, else its class's, else the pcb's usual one. */
    public double trackWidthOf(Optional<String> net) {
        Optional<Double> paired = net.flatMap(name -> pcb.pairs().stream()
                .filter(pair -> pair.positive().equals(name) || pair.negative().equals(name)).findFirst())
                .map(Pair::width).filter(width -> width > 0);
        return paired.or(() -> classOf(net).map(NetClass::width).filter(width -> width > 0))
                .orElse(value(PcbVocabulary.RULE_TRACK_WIDTH));
    }

    /** The via a net is laid with: its class's, else the pcb's usual one. */
    public ViaSize viaOf(Optional<String> net) {
        return classOf(net).filter(netClass -> netClass.drill() > 0 && netClass.diameter() > 0)
                .map(netClass -> new ViaSize(netClass.drill(), netClass.diameter()))
                .orElse(new ViaSize(value(PcbVocabulary.RULE_VIA_DRILL), value(PcbVocabulary.RULE_VIA_DIAMETER)));
    }
}
