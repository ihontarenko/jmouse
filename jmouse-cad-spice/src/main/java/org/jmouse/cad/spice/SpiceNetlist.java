package org.jmouse.cad.spice;

import org.jmouse.formula.Formula;
import org.jmouse.spice.SpicePosition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a SPICE netlist means, as far as jMX can follow it: elements of the kinds both sides have, models, subcircuits,
 * parameters and the transient — every value a jMF {@link Formula}, every node a lower-case name ({@code 0} for ground),
 * every item with the line it was written on. Whatever the {@link SpiceAnalyzer} could not put here is in
 * {@link #notes()}, by line.
 *
 * @param title       the netlist's first line, when it has one
 * @param elements    the elements outside any subcircuit, in order
 * @param subcircuits every subcircuit, by its lower-case name
 * @param models      every model, by its lower-case name
 * @param parameters  the {@code .param} values outside any subcircuit, in order
 */
public record SpiceNetlist(Optional<String> title, List<Element> elements, Map<String, Subcircuit> subcircuits,
                           Map<String, Model> models, List<Parameter> parameters, Optional<Transient> analysis,
                           List<SpiceNote> notes) {

    public SpiceNetlist {
        elements = List.copyOf(elements);
        subcircuits = Collections.unmodifiableMap(new LinkedHashMap<>(subcircuits));
        models = Collections.unmodifiableMap(new LinkedHashMap<>(models));
        parameters = List.copyOf(parameters);
        notes = List.copyOf(notes);
    }

    /** {@code .param R=1k}: a name, as declared, and its value. */
    public record Parameter(String name, Formula value, SpicePosition position) {
    }

    /** {@code .tran step stop [start]}. */
    public record Transient(double step, double stop, double start, SpicePosition position) {
    }

    /** The device a {@code .model} card describes. */
    public enum ModelType {
        D, NPN, PNP, NMOS, PMOS, NJF, PJF
    }

    /**
     * {@code .model D1N4148 D(IS=2.52n RS=0.568)}.
     *
     * @param parameters each parameter by its upper-case SPICE name, in order
     */
    public record Model(String name, ModelType type, Map<String, Formula> parameters, SpicePosition position) {
        public Model {
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }

    /**
     * {@code .subckt name pins… params: …} with its elements.
     *
     * @param parameters its {@code params:} defaults, then its own {@code .param} cards, in order
     */
    public record Subcircuit(String name, List<String> pins, List<Parameter> parameters, List<Element> elements,
                             SpicePosition position) {
        public Subcircuit {
            pins = List.copyOf(pins);
            parameters = List.copyOf(parameters);
            elements = List.copyOf(elements);
        }
    }

    // ─── elements ───────────────────────────────────────────────────────────────────────────────────

    /** One element card of a kind jMX has. */
    public sealed interface Element {
        String name();

        SpicePosition position();
    }

    public enum PassiveKind { RESISTOR, CAPACITOR, INDUCTOR }

    /** {@code R1 in out 1k}, {@code C1 out 0 {C}}, {@code L1 a b 10m}. */
    public record Passive(String name, PassiveKind kind, String positive, String negative, Formula value,
                          SpicePosition position) implements Element {
    }

    /** {@code V1 in 0 DC 5}, {@code V2 a b SIN(0 1 50)}, {@code I1 0 x 1m}: SPICE's nodes, in SPICE's order. */
    public record Source(String name, boolean voltage, String positive, String negative, Signal signal,
                         SpicePosition position) implements Element {
    }

    /** {@code D1 anode cathode model}. */
    public record Diode(String name, String anode, String cathode, String model, SpicePosition position)
            implements Element {
    }

    /** {@code Q1 collector base emitter [substrate] model}. */
    public record Bipolar(String name, String collector, String base, String emitter, String model,
                          SpicePosition position) implements Element {
    }

    /**
     * {@code M1 drain gate source bulk model [L= W= …]}. {@code width} and {@code length} are null where the line does not
     * write them: SPICE's default, the same for both.
     */
    public record Mosfet(String name, String drain, String gate, String source, String bulk, String model,
                         Formula width, Formula length, SpicePosition position) implements Element {
    }

    /** {@code J1 drain gate source model}. */
    public record Jfet(String name, String drain, String gate, String source, String model, SpicePosition position)
            implements Element {
    }

    /**
     * A linear controlled source: {@code E1 out 0 in+ in- 10} — a voltage of {@code gain} times the controlling voltage —
     * or {@code G1 out 0 in+ in- 1m}, a current of it.
     */
    public record LinearControlled(String name, boolean voltage, String positive, String negative, String controlPositive,
                                   String controlNegative, Formula gain, SpicePosition position) implements Element {
    }

    /** {@code X1 nodes… subcircuit [name=value …]}. */
    public record Instance(String name, List<String> nodes, String subcircuit, Map<String, Formula> parameters,
                           SpicePosition position) implements Element {
        public Instance {
            nodes = List.copyOf(nodes);
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }

    // ─── source signals ─────────────────────────────────────────────────────────────────────────────

    /** What a source gives over time. */
    public sealed interface Signal {
    }

    /** {@code DC 5}, or a value alone. */
    public record Constant(Formula value) implements Signal {
    }

    /**
     * {@code SIN(VO VA FREQ TD THETA PHASE)}; a value not written is SPICE's default — 0, and for the frequency
     * {@code null}, meaning one period over the transient.
     *
     * @param phase in degrees, as SPICE writes it
     */
    public record Sine(Formula offset, Formula amplitude, Formula frequency, Formula delay, Formula damping,
                       Formula phase) implements Signal {
    }

    /**
     * {@code PULSE(V1 V2 TD TR TF PW PER)}; the width and the period are {@code null} when not written — SPICE then
     * holds the pulse to the end of the transient.
     */
    public record Pulse(Formula initial, Formula pulsed, Formula delay, Formula rise, Formula fall, Formula width,
                        Formula period) implements Signal {
    }
}
