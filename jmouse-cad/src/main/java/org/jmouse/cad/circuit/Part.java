package org.jmouse.cad.circuit;

import org.jmouse.cad.Setting;

import java.util.List;
import java.util.Optional;

/**
 * A part of a circuit: its reference, the component it is — {@code resistor}, {@code lm358@3} for a unit of one —, what
 * it is connected to, what it is set to, and how it is drawn or seated when not as its component says.
 *
 * <p>⚠️ Every part is a component. A resistor and a microcontroller are the same kind of thing here; which of them the
 * engine solves natively is the engine's to say, from the component, never a second sort of part.</p>
 *
 * @param symbol    the symbol it is drawn with when not its component's own, or null
 * @param footprint the footprint it sits on when not its component's own, or null
 */
public record Part(String reference, String component, List<Connection> connections, List<Setting> settings,
                   String symbol, String footprint) {

    public Part {
        connections = List.copyOf(connections);
        settings = List.copyOf(settings);
    }

    /** Whether it is connected in pin order — {@code (in, out)} — rather than pin by name. */
    public boolean inPinOrder() {
        return connections.stream().noneMatch(connection -> connection.named().isPresent());
    }

    public boolean connects(String net) {
        return connections.stream().anyMatch(connection -> connection.net().equals(net));
    }

    public Optional<Setting> setting(String name) {
        return settings.stream().filter(setting -> setting.name().equals(name)).findFirst();
    }
}
