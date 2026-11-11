package org.jmouse.cad.component;

import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.DocumentHead;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.circuit.Bench;
import org.jmouse.cad.circuit.Expectation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A component: what a part is. Its parameters and pins, the nodes and states of its own, its equations for jMF and its
 * behaviour for jMS, the figures it shows and an instrument reads, how it is drawn, how it looks, what it sits on, and
 * what it is expected to do on a bench.
 *
 * @param bases     the components it extends, by slug — a datasheet part extends the model it is a member of
 * @param uses      the families and libraries of functions it uses, by slug
 * @param units     the blocks of its package — {@code units A, B} —, each with its own pin numbers and states; empty for
 *                  a package of one
 * @param types     the quantity types it names itself — {@code type transconductance = current / voltage ^ 2} —, as
 *                  written
 * @param formulas  its own definitions for jMF — {@code formulas { … }} —, before the libraries it uses add theirs
 * @param behaviour its jMS, as written, or null for a component its equations say all of
 * @param symbol    how it is drawn, or null for a component drawn as its base is
 * @param portrait  how it looks in the hand, or null
 * @param bench     the bench its expectations run on when they name none, or null
 * @param parts     what it is made of, when it is a subcircuit — its own parts, wired to its pins and nets of its
 *                  own —; empty for a component its equations or behaviour say all of
 */
public record Component(DocumentHead head, List<String> bases, List<String> uses, List<String> units, List<Parameter> parameters,
                        List<Pin> pins, List<InternalNode> nodes, List<State> states, List<Figure> display,
                        List<Figure> readings, List<Equation> equations, List<Equation> formulas, String behaviour, Drawing symbol, Drawing portrait,
                        List<Seat> seats, Map<String, Formula> types, Bench bench, List<Expectation> expectations,
                        List<Lead> leads, List<Part> parts) {

    public Component {
        bases = List.copyOf(bases);
        uses = List.copyOf(uses);
        units = List.copyOf(units);
        parameters = List.copyOf(parameters);
        pins = List.copyOf(pins);
        nodes = List.copyOf(nodes);
        states = List.copyOf(states);
        display = List.copyOf(display);
        readings = List.copyOf(readings);
        equations = List.copyOf(equations);
        formulas = List.copyOf(formulas);
        seats = List.copyOf(seats);
        types = Map.copyOf(types);
        expectations = List.copyOf(expectations);
        leads = List.copyOf(leads);
        parts = List.copyOf(parts);
    }

    public Optional<Pin> pin(String name) {
        return pins.stream().filter(pin -> pin.name().equals(name)).findFirst();
    }

    public Optional<String> script() {
        return Optional.ofNullable(behaviour);
    }

    public Optional<Drawing> drawn() {
        return Optional.ofNullable(symbol);
    }
}
