package org.jmouse.spice.printer;

import org.jmouse.el.node.Trivia;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceNode;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.node.SpiceVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes a SPICE syntax tree as text — the one way SPICE is written, whether the tree was read or built.
 *
 * <p>Values, expressions and words are written as they were read: {@code 10uF} stays {@code 10uF}, {@code {2*R1}}
 * stays in its braces. Comments come back where the tree keeps them: a card's leading comments before it, its trailing
 * one after it on its line. Continuation lines are joined — a card is one line — and keywords are written in the case
 * SPICE's manuals write them; neither changes what the deck means.</p>
 */
public final class SpicePrinter implements SpiceVisitor<List<String>> {

    private SpicePrinter() {
    }

    /** The deck as SPICE text, one card a line. */
    public static String print(SpiceDeck deck) {
        return String.join("\n", deck.accept(new SpicePrinter())) + "\n";
    }

    /** One statement as SPICE text, its comments with it. */
    public static String print(SpiceStatement statement) {
        return String.join("\n", new SpicePrinter().statement(statement)) + "\n";
    }

    @Override
    public List<String> visitDeck(SpiceDeck deck) {
        List<String> lines = new ArrayList<>();
        deck.title().ifPresent(lines::add);
        deck.statements().forEach(statement -> lines.addAll(statement(statement)));
        deck.closingTrivia().forEach(trivia -> lines.add(comment(trivia)));
        // SPICE reads a deck's first line as its title: a deck without one that does not begin with a dot card gets an
        // empty title line, or its first card would be read as the title.
        if (deck.title().isEmpty() && !lines.isEmpty() && !lines.getFirst().startsWith(".")) {
            lines.addFirst("");
        }
        return lines;
    }

    /** A statement's lines: its leading comments, then the statement with its trailing comment. */
    private List<String> statement(SpiceNode statement) {
        List<String> lines = new ArrayList<>();
        statement.getLeadingTrivia().forEach(trivia -> lines.add(comment(trivia)));
        List<String> own = new ArrayList<>(statement.accept(this));
        Trivia trailing = statement.getTrailingTrivia();
        if (trailing != null && !own.isEmpty()) {
            own.set(0, own.getFirst() + " " + trailingComment(trailing));
        }
        lines.addAll(own);
        return lines;
    }

    // ─── elements ───────────────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> visitPassive(SpiceElement.Passive passive) {
        return line(passive.name(), passive.positive(), passive.negative(), passive.value().written(),
                arguments(passive.rest()));
    }

    @Override
    public List<String> visitSource(SpiceElement.Source source) {
        List<String> words = new ArrayList<>(List.of(source.name(), source.positive(), source.negative()));
        source.direct().ifPresent(direct -> {
            if (source.directWord()) {
                words.add("DC");
            }
            words.add(direct.written());
        });
        source.waveform().ifPresent(waveform -> words.add(waveform.written()));
        source.rest().forEach(argument -> words.add(argument.written()));
        return List.of(String.join(" ", words));
    }

    @Override
    public List<String> visitDiode(SpiceElement.Diode diode) {
        return line(diode.name(), diode.anode(), diode.cathode(), diode.model(), arguments(diode.rest()));
    }

    @Override
    public List<String> visitBipolar(SpiceElement.Bipolar bipolar) {
        return line(bipolar.name(), bipolar.collector(), bipolar.base(), bipolar.emitter(),
                bipolar.substrate().orElse(""), bipolar.model(), arguments(bipolar.rest()));
    }

    @Override
    public List<String> visitMosfet(SpiceElement.Mosfet mosfet) {
        return line(mosfet.name(), mosfet.drain(), mosfet.gate(), mosfet.source(), mosfet.bulk(), mosfet.model(),
                arguments(mosfet.rest()));
    }

    @Override
    public List<String> visitInstance(SpiceElement.Instance instance) {
        return line(instance.name(), String.join(" ", instance.nodes()), instance.subcircuit(),
                parameters(instance.parameters()));
    }

    @Override
    public List<String> visitBehavioural(SpiceElement.Behavioural behavioural) {
        return line(behavioural.name(), behavioural.positive(), behavioural.negative(),
                (behavioural.isVoltage() ? "V=" : "I=") + behavioural.expression());
    }

    @Override
    public List<String> visitControlled(SpiceElement.Controlled controlled) {
        return line(controlled.name(), controlled.positive(), controlled.negative(), arguments(controlled.control()));
    }

    @Override
    public List<String> visitCoupling(SpiceElement.Coupling coupling) {
        return line(coupling.name(), String.join(" ", coupling.inductors()), coupling.coefficient().written());
    }

    @Override
    public List<String> visitGenericElement(SpiceElement.Generic element) {
        return line(element.name(), arguments(element.arguments()));
    }

    // ─── cards ──────────────────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> visitModel(SpiceCard.Model model) {
        return List.of(".model " + model.name() + " " + model.type() + "(" + parameters(model.parameters()) + ")");
    }

    @Override
    public List<String> visitSubcircuit(SpiceCard.Subcircuit subcircuit) {
        List<String> lines = new ArrayList<>();
        String parameters = subcircuit.parameters().isEmpty() ? "" : "params: " + parameters(subcircuit.parameters());
        lines.add(String.join(" ", line(".subckt", subcircuit.name(), String.join(" ", subcircuit.pins()), parameters)));
        subcircuit.statements().forEach(statement -> lines.addAll(statement(statement)));
        subcircuit.closingTrivia().forEach(trivia -> lines.add(comment(trivia)));
        lines.add(".ends " + subcircuit.name());
        return lines;
    }

    @Override
    public List<String> visitParameters(SpiceCard.Parameters parameters) {
        return List.of(".param " + parameters(parameters.parameters()));
    }

    @Override
    public List<String> visitInclude(SpiceCard.Include include) {
        return line("." + include.directive(), "\"" + include.path() + "\"", include.section().orElse(""));
    }

    @Override
    public List<String> visitTransient(SpiceCard.Transient analysis) {
        return line(".tran", analysis.step().written(), analysis.stop().written(),
                analysis.start().map(SpiceValue::written).orElse(analysis.largestStep().isPresent() ? "0" : ""),
                analysis.largestStep().map(SpiceValue::written).orElse(""), analysis.initialConditions() ? "uic" : "");
    }

    @Override
    public List<String> visitAc(SpiceCard.Ac analysis) {
        return line(".ac", analysis.variation(), analysis.points().written(), analysis.start().written(),
                analysis.stop().written());
    }

    @Override
    public List<String> visitDc(SpiceCard.Dc analysis) {
        return line(".dc", arguments(analysis.sweeps()));
    }

    @Override
    public List<String> visitOperatingPoint(SpiceCard.OperatingPoint analysis) {
        return List.of(".op");
    }

    @Override
    public List<String> visitControl(SpiceCard.Control control) {
        List<String> lines = new ArrayList<>();
        lines.add(".control");
        lines.addAll(control.lines());
        lines.add(".endc");
        return lines;
    }

    @Override
    public List<String> visitDirective(SpiceCard.Directive directive) {
        return line("." + directive.directive(), arguments(directive.arguments()));
    }

    @Override
    public List<String> visitEnd(SpiceCard.End end) {
        return List.of(".end");
    }

    // ─── pieces ─────────────────────────────────────────────────────────────────────────────────────

    /** Words joined by one blank, empty ones left out. */
    private static List<String> line(String... words) {
        List<String> written = new ArrayList<>();
        for (String word : words) {
            if (word != null && !word.isEmpty()) {
                written.add(word);
            }
        }
        return List.of(String.join(" ", written));
    }

    private static String arguments(List<SpiceArgument> arguments) {
        return String.join(" ", arguments.stream().map(SpiceArgument::written).toList());
    }

    private static String parameters(List<SpiceParameter> parameters) {
        return String.join(" ", parameters.stream().map(SpiceParameter::written).toList());
    }

    /** A comment line as written; one a program wrote without SPICE's mark gets {@code *} in front. */
    private static String comment(Trivia trivia) {
        if (trivia.isBlank()) {
            return "";
        }
        String text = trivia.text();
        return text.startsWith("*") || text.startsWith(";") || text.startsWith("$") ? text : "* " + text;
    }

    /** A comment after a card on its line; a {@code *} comment cannot stand there, so it is written after {@code ;}. */
    private static String trailingComment(Trivia trivia) {
        String text = trivia.text();
        return text.startsWith(";") || text.startsWith("$") ? text : "; " + (text.startsWith("*") ? text.substring(1).strip() : text);
    }
}
