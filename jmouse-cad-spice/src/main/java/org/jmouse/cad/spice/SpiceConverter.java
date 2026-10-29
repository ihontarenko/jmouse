package org.jmouse.cad.spice;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.CircuitElaboration;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.parser.SpiceParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The bridge between SPICE and jMX, both ways: a netlist read by jmouse-spice becomes a {@code circuit} or a
 * {@code component} — built as its document's tree, which writes itself in whichever spelling the product keeps —, and
 * a circuit or a component becomes a SPICE deck, built as jmouse-spice's nodes, which write themselves.
 *
 * <p>Nothing is dropped silently and nothing is guessed: what has no counterpart on the other side, or is carried
 * across differently, is a note — «line 12: E1 … left out». And what is built is written, read back and compiled
 * against the catalogue before it is returned — a deck read back by the SPICE parser —, so a conversion that builds what
 * its reader refuses fails here rather than handing it on.</p>
 *
 * <p>The catalogue is where the components a part names are found — their pins, parameters and units decide what a
 * part becomes —; the table ({@link SpiceDevices}) is which component each card and each {@code .model} becomes.</p>
 */
public final class SpiceConverter {

    private final CatalogueDocuments catalogue;
    private final DocumentLanguage   language;
    private final SpiceDevices       devices;
    private final SpiceParser        parser = new SpiceParser();

    public SpiceConverter(CatalogueDocuments catalogue, DocumentLanguage language) {
        this(catalogue, language, SpiceDevices.standard());
    }

    public SpiceConverter(CatalogueDocuments catalogue, DocumentLanguage language, SpiceDevices devices) {
        this.catalogue = catalogue;
        this.language = language;
        this.devices = devices;
    }

    /**
     * A netlist as jMX.
     *
     * @param document the document, as read back from what it writes
     * @param notes    what was not carried across, or was carried differently, by line
     */
    public record SpiceImport(DocumentNode document, List<String> notes) {

        public SpiceImport {
            notes = List.copyOf(notes);
        }
    }

    /** A document as a SPICE deck, and what was not carried across. */
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
     * @throws IllegalStateException                when the document built does not read back — the converter's failure
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
        SpiceImporter.Imported imported = SpiceImporter.convert(SpiceAnalyzer.analyze(deck), name, catalogue, devices);
        return new SpiceImport(readBack(imported.document()), imported.notes().stream().map(SpiceNote::toString).toList());
    }

    /**
     * Converts a {@code circuit} to a deck, or a {@code component} to its {@code .model} or {@code .subckt}.
     *
     * @throws org.jmouse.spice.SpiceParseException when it is a document SPICE cannot describe
     * @throws IllegalStateException                when the deck built does not read back — the converter's failure
     */
    public SpiceExport exportDocument(DocumentNode document) {
        SpiceExporter.Written written = new SpiceExporter(catalogue, devices, Map.of()).write(document);
        String text = written.text();
        try {
            parser.parse("exported", text);
        } catch (RuntimeException refused) {
            throw new IllegalStateException("The SPICE built for this document does not read back: " + refused.getMessage(),
                    refused);
        }
        return new SpiceExport(text, written.notes().stream().map(SpiceNote::toString).toList());
    }

    /** The deck a document is, as nodes, for a caller that writes or inspects it itself. */
    public SpiceDeck exportDeck(DocumentNode document) {
        return new SpiceExporter(catalogue, devices, Map.of()).write(document).deck();
    }

    /** The document as its reader reads what it writes, compiled — a component as itself, a circuit on a nominal bench. */
    private DocumentNode readBack(DocumentNode built) {
        try {
            DocumentNode read = language.readOne(language.write(built), built.slug());
            if (read.kind().equals(SimulationModule.COMPONENT)) {
                CatalogueDocuments withIt = slug -> slug.equals(read.slug()) ? Optional.of(read) : catalogue.find(slug);
                ComponentCompilation.compile(SimulationReading.component(read), withIt);
            } else {
                CircuitElaboration.of(SimulationReading.circuit(read), catalogue, BenchConditions.nominal());
            }
            return read;
        } catch (RuntimeException refused) {
            throw new IllegalStateException("The document built for this netlist does not read back: " + refused.getMessage(),
                    refused);
        }
    }
}
