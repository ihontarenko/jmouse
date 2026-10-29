package org.jmouse.cad.simulation.elaboration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A part on the sheet that is simulated as the parts it is made of — a subcircuit of several —: the nets its pins are
 * on, and the pins of every part it is made of, each with its net. What it reports is theirs, added up.
 *
 * @param pins    each of its pins → the net it is on
 * @param members each part it is made of → each of that part's pins → its net
 */
public record Assembly(String part, Map<String, String> pins, Map<String, Map<String, String>> members) {

    public Assembly {
        pins = Map.copyOf(pins);
        members = Map.copyOf(members);
    }

    /**
     * The current into each of its pins: what flows into the pins of its parts that sit on that pin's net — the current
     * that crosses its boundary there. A pin on an inner net crosses nothing and counts for no pin of its own.
     */
    public Map<String, Double> pinCurrents(Map<String, Map<String, Double>> terminals) {
        Map<String, Double> currents = new LinkedHashMap<>();
        members.forEach((member, nets) -> {
            Map<String, Double> into = terminals.get(member);
            if (into == null) {
                return;
            }
            nets.forEach((terminal, net) -> {
                Double current = into.get(terminal);
                String pin = pinOn(net);
                if (current != null && pin != null) {
                    currents.merge(pin, current, Double::sum);
                }
            });
        });
        return currents;
    }

    private String pinOn(String net) {
        return pins.entrySet().stream().filter(pin -> pin.getValue().equals(net)).map(Map.Entry::getKey).sorted().findFirst()
                .orElse(null);
    }
}
