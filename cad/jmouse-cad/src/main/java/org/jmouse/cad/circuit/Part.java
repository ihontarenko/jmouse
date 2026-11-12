package org.jmouse.cad.circuit;

import org.jmouse.cad.Text;
import org.jmouse.cad.Setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * @param pads      which pin each pad of that footprint takes, by pad, where this part's differ from what its component
 *                  says: a footprint another product numbers its own way — a KiCad diode's pad 1 is its cathode. Empty
 *                  otherwise.
 */
public record Part(String reference, String component, List<Connection> connections, List<Setting> settings,
                   String symbol, String footprint, Map<String, String> pads) {

    /** The setting a part's value in words is written under. */
    public static final String VALUE = "value";

    public Part {
        connections = List.copyOf(connections);
        settings = List.copyOf(settings);
        pads = pads == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(pads));
    }

    /** A part on its component's pads. */
    public Part(String reference, String component, List<Connection> connections, List<Setting> settings, String symbol,
                String footprint) {
        this(reference, component, connections, settings, symbol, footprint, Map.of());
    }

    /** Whether it is connected in pin order — {@code (in, out)} — rather than pin by name. */
    public boolean inPinOrder() {
        return connections.stream().noneMatch(connection -> connection.named().isPresent());
    }

    public boolean connects(String net) {
        return connections.stream().anyMatch(connection -> connection.net().equals(net));
    }

    /**
     * The value a person reads, written on the part itself in words — {@code value: "100 nF 1%"} —: what a design says of a part
     * whose component has no parameter for it. A component's own parameter of that name, a number, is that parameter.
     */
    public Optional<String> valueInWords() {
        return setting(VALUE).map(Setting::value).filter(Text.class::isInstance).map(words -> ((Text) words).text());
    }

    public Optional<Setting> setting(String name) {
        return settings.stream().filter(setting -> setting.name().equals(name)).findFirst();
    }
}
