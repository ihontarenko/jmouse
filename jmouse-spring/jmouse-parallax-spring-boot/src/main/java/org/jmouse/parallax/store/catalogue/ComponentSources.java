package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which symbols a component is drawn with, and how its pins land on their ports, as its text says it:
 * {@code symbol uses diode-symbol { a anode;  b cathode }}. The first symbol named is the default; a port the braces
 * leave out is the pin of the same name. That line is the only place the choice is kept — the component editor's
 * symbol field is written into it by {@link #withSymbol}.
 */
final class ComponentSources {

    /** A {@code symbol uses} line: the words before the slugs, the slugs, and the brace or comment after them. */
    private static final Pattern USES_LINE = Pattern.compile("^(\\s*symbol\\s+uses\\s+)([^{#]*?)(\\s*(?:[{#].*)?)$");

    private ComponentSources() {
    }

    /** The symbols a component names, the default first; empty when it is drawn from its own pins. */
    static List<String> symbolsOf(ExperienceDocument component) {
        return component.block(Block.Symbol.class).map(Block.Symbol::uses).orElse(List.of());
    }

    /**
     * The component pin each port of a symbol is, in the symbol's port order; a port that is no pin is left out.
     *
     * @param pins  the component's pins, its base's included: a part that `extends` one and writes none of its own
     *              has them all from it, and read from its own text alone it had none and was drawn as a box
     * @param ports the ports the symbol draws
     */
    static Map<String, String> pinsByPort(ExperienceDocument component, Set<String> pins, List<String> ports) {
        Map<String, String> written = writtenPorts(component);
        Map<String, String> bound = new LinkedHashMap<>();
        for (String port : ports) {
            String pin = written.getOrDefault(port, port);
            if (pins.contains(pin)) {
                bound.put(port, pin);
            }
        }
        return bound;
    }

    /**
     * Why a symbol cannot draw the component — a mapped port it does not draw, a mapped pin the component does not
     * have, a pin left without a port — or empty when every pin lands on a port of its own.
     */
    static Optional<String> bindingProblem(ExperienceDocument component, Set<String> pins, String symbolSlug, List<String> ports) {
        Map<String, String> written = writtenPorts(component);
        for (Map.Entry<String, String> entry : written.entrySet()) {
            if (!ports.contains(entry.getKey())) {
                return Optional.of("'" + component.slug() + "' maps port '" + entry.getKey() + "', which "
                        + symbolSlug + " does not draw; it draws " + String.join(", ", ports));
            }
            if (!pins.contains(entry.getValue())) {
                return Optional.of("'" + component.slug() + "' maps port '" + entry.getKey() + "' to '"
                        + entry.getValue() + "', which is not one of its pins");
            }
        }
        Map<String, String> bound = pinsByPort(component, pins, ports);
        Set<String> landed = new LinkedHashSet<>(bound.values());
        if (landed.size() != bound.size()) {
            return Optional.of("'" + component.slug() + "' puts one pin on two ports of " + symbolSlug);
        }
        for (String pin : pins) {
            if (!landed.contains(pin)) {
                return Optional.of("pin '" + pin + "' of '" + component.slug() + "' lands on no port of " + symbolSlug
                        + "; map it with 'symbol uses " + symbolSlug + " { <port> " + pin + " }'");
            }
        }
        return Optional.empty();
    }

    /**
     * The component's text with the chosen symbol as its default — first on its {@code symbol uses} line, the others
     * kept after it — or unchanged when nothing is chosen or the choice is already the default. A component drawn
     * from its own pins ({@code symbol box}) is refused: which pin lands on which port is for its text to say.
     *
     * @param symbolSlug the chosen symbol, or null for no choice
     */
    static String withSymbol(String source, String symbolSlug) {
        if (symbolSlug == null) {
            return source;
        }
        ExperienceDocument document = readComponent(source);
        List<String> variants = symbolsOf(document);
        if (!variants.isEmpty() && variants.getFirst().equals(symbolSlug)) {
            return source;
        }
        Optional<String> shape = document.block(Block.Symbol.class).flatMap(Block.Symbol::shape);
        if (shape.isPresent()) {
            throw ParallaxProblem.invalidDocument("'" + document.slug() + "' is drawn as a " + shape.get()
                    + " from its pins; to draw it with " + symbolSlug + ", write 'symbol uses " + symbolSlug
                    + " { port pin … }' in place of its symbol block.", document.line(), 0);
        }
        List<String> chosen = new ArrayList<>();
        chosen.add(symbolSlug);
        variants.stream().filter(variant -> !variant.equals(symbolSlug)).forEach(chosen::add);
        List<String> lines = new ArrayList<>(List.of(source.split("\n", -1)));
        if (variants.isEmpty()) {
            insertUsesLine(lines, "    symbol uses " + symbolSlug);
        } else {
            replaceUsesLine(lines, String.join(", ", chosen));
        }
        String written = String.join("\n", lines);
        List<String> recorded = symbolsOf(readComponent(written));
        if (recorded.isEmpty() || !recorded.getFirst().equals(symbolSlug)) {
            throw ParallaxProblem.invalidDocument("The symbol could not be recorded in the component's text; write "
                    + "'symbol uses " + symbolSlug + "' in it.", document.line(), 0);
        }
        return written;
    }

    private static void replaceUsesLine(List<String> lines, String slugs) {
        for (int index = 0; index < lines.size(); index++) {
            Matcher uses = USES_LINE.matcher(lines.get(index));
            if (uses.matches()) {
                lines.set(index, uses.group(1) + slugs + uses.group(3));
                return;
            }
        }
    }

    /** Before the brace that closes the component: its last line holding only {@code }}. */
    private static void insertUsesLine(List<String> lines, String usesLine) {
        for (int index = lines.size() - 1; index >= 0; index--) {
            if (lines.get(index).strip().equals("}")) {
                lines.add(index, usesLine);
                return;
            }
        }
    }

    private static Map<String, String> writtenPorts(ExperienceDocument component) {
        return component.block(Block.Symbol.class).map(Block.Symbol::ports).orElse(Map.of());
    }


    static ExperienceDocument readComponent(String source) {
        try {
            return ExperienceReader.readOne(source, "component");
        } catch (IllegalArgumentException unreadable) {
            throw ParallaxProblem.invalid("invalid-document", unreadable.getMessage());
        }
    }
}
