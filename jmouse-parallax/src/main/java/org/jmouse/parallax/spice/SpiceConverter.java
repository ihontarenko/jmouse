package org.jmouse.parallax.spice;

import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.circuit.CircuitEngine;
import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.parser.SpiceParser;
import org.jmouse.spice.printer.SpicePrinter;

import java.util.List;
import java.util.Map;

/**
 * The bridge between SPICE and jMX, both ways: a netlist read by jmouse-spice becomes a jMX {@code circuit} or
 * {@code component}, and a jMX circuit or component becomes a SPICE deck printed by jmouse-spice.
 *
 * <p>Nothing is dropped silently and nothing is guessed: what has no counterpart on the other side, or is carried
 * across differently, is a note — «line 12: E1 (a voltage-controlled voltage source) has no jMX counterpart; left
 * out». And what is written is read back before it is returned — the jMX by the jMX reader and compiled against the
 * component library, the SPICE by the SPICE parser —, so a conversion that writes text its reader refuses fails here
 * rather than handing that text on.</p>
 *
 * <p>The library is where the components a part names are read from — their parameters, pins and equations decide
 * what a part becomes; the carriers are which component a {@code .model} becomes when its name names none.</p>
 */
public final class SpiceConverter {

    private final ComponentLibrary   library;
    private final SpiceModelCarriers carriers;
    private final SpiceParser        parser = new SpiceParser();

    public SpiceConverter(ComponentLibrary library) {
        this(library, SpiceModelCarriers.standard());
    }

    public SpiceConverter(ComponentLibrary library, SpiceModelCarriers carriers) {
        this.library = library;
        this.carriers = carriers;
    }

    /**
     * A netlist as jMX.
     *
     * @param source the jMX document
     * @param kind   {@link Kind#CIRCUIT} or {@link Kind#COMPONENT}
     * @param notes  what was not carried across, or was carried differently, by line
     */
    public record SpiceImport(String source, Kind kind, List<String> notes) {
        public SpiceImport {
            notes = List.copyOf(notes);
        }
    }

    /** A jMX document as a SPICE deck, and what was not carried across. */
    public record SpiceExport(String text, List<String> notes) {
        public SpiceExport {
            notes = List.copyOf(notes);
        }
    }

    /**
     * Converts a SPICE netlist: a {@code circuit} when it has parts of its own, else the {@code component} its
     * {@code .subckt} is.
     *
     * @param name the document's title, or {@code null} for the netlist's own; when the text holds subcircuits alone,
     *             also which one to convert
     * @throws org.jmouse.spice.SpiceParseException when the text is not SPICE, or a value in it does not read, naming
     *                                              the line and column
     * @throws IllegalStateException                when the jMX written does not read back — the converter's failure
     */
    public SpiceImport importNetlist(String text, String name) {
        return importNetlist(text, name, SpiceIncludes.none());
    }

    /**
     * The same, the files its {@code .include} and {@code .lib} lines name written in from {@code includes} first; a
     * file not among them is left out with a note.
     */
    public SpiceImport importNetlist(String text, String name, SpiceIncludes includes) {
        SpiceDeck deck = parser.parse("netlist", new IncludeExpansion(includes).expand(text));
        SpiceNetlist netlist = SpiceAnalyzer.analyze(deck);
        SpiceImporter.Written written = new SpiceImporter(library, carriers).write(netlist, name);
        requireReadable(written.source(), written.kind());
        return new SpiceImport(written.source(), written.kind(), lines(written.notes()));
    }

    /**
     * Converts a jMX {@code circuit} to a deck, or a {@code component} to its {@code .model} or {@code .subckt}.
     *
     * @throws org.jmouse.parallax.script.ExperienceParseException when the text is not jMX
     * @throws org.jmouse.spice.SpiceParseException                when it is jMX that SPICE cannot describe
     */
    public SpiceExport exportDocument(String source) {
        SpiceExporter.Written written = new SpiceExporter(library).write(source);
        String text = written.text();
        try {
            parser.parse("exported", text);
        } catch (RuntimeException refused) {
            throw new IllegalStateException("The SPICE written for this document does not read back: "
                    + refused.getMessage(), refused);
        }
        return new SpiceExport(text, lines(written.notes()));
    }

    /**
     * A jMX circuit's transient computed by SPICE instead of the engine — by {@link SpiceEngine}, as every analysis SPICE
     * runs, at the parameters given. A part the deck could not carry is in the notes rather than refused: the reader
     * sees them beside the answer.
     *
     * @param parameters parameters at other values than the document's: where the bench's sliders stand
     * @throws SpiceRunException when SPICE gives no answer
     */
    public SpiceTransient simulateTransient(String source, Map<String, Double> parameters, SpiceSimulator simulator) {
        DefinedObject defined = ExperienceDefinitions.read(source, "circuit");
        CompiledObject circuit = DefinitionCompiler.compile(defined.definition(), defined.parameters(), parameters, library);
        SpiceEngine.SpiceRun run = new SpiceEngine(simulator).transientRun(circuit,
                new CircuitEngine.TransientRequest(SpiceExporter.PRINTED_SAMPLES, false, false, null));
        return new SpiceTransient((Dataset.TimeSeries) run.dataset(), run.deck(), run.notes());
    }

    /** A transient computed by SPICE: its series, the deck it ran, and what the deck could not carry. */
    public record SpiceTransient(Dataset.TimeSeries series, String deck, List<String> notes) {
        public SpiceTransient {
            notes = List.copyOf(notes);
        }
    }

    /** The deck a jMX document is, as nodes, for a caller that writes or inspects it itself. */
    public SpiceDeck exportDeck(String source) {
        return new SpiceExporter(library).write(source).deck();
    }

    /** Prints a deck — the one way SPICE is written. */
    public static String print(SpiceDeck deck) {
        return SpicePrinter.print(deck);
    }

    private void requireReadable(String source, Kind kind) {
        try {
            ExperienceDocument document = ExperienceReader.readOne(source, "imported");
            if (kind == Kind.COMPONENT) {
                ComponentDocuments.read(source, document.slug(), library);
            } else {
                DefinedObject defined = ExperienceDefinitions.read(source, "imported");
                DefinitionCompiler.compile(defined.definition(), defined.parameters(), Map.of(), library);
            }
        } catch (RuntimeException refused) {
            throw new IllegalStateException("The jMX written for this netlist does not read back: "
                    + refused.getMessage(), refused);
        }
    }

    private static List<String> lines(List<SpiceNote> notes) {
        return notes.stream().map(SpiceNote::toString).toList();
    }
}
