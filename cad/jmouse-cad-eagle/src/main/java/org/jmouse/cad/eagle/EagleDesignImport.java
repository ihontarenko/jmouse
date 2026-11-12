package org.jmouse.cad.eagle;

import org.jmouse.cad.exchange.imported.SheetMarks;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.eagle.EagleBoard;
import org.jmouse.eagle.EagleSchematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An Eagle design as jM documents: its schematic — the footprints, symbols and components its libraries carry, and its
 * circuit — and, where its pcb is read with it, the pcb laid out from that circuit, its parts and nets the circuit's.
 * A pcb is read with its schematic, as Eagle opens it: the circuit is what says which pads belong together.
 */
public final class EagleDesignImport {

    private EagleDesignImport() {
    }

    /**
     * @param schematicFile the schematic's file, which the circuit says it was imported from
     * @param pcb         the pcb and its file, where it is read with the schematic
     * @param title         what the circuit is called; the pcb is called the same
     * @param marks         how the sheet's marks are drawn
     */
    public static ImportedDocuments read(EagleSchematic schematic, String schematicFile, Optional<Read> pcb, String title,
                                         EagleLibraries filing, SheetMarks marks) {
        EagleSchematicImport.Converted circuit = EagleSchematicImport.read(schematic, schematicFile, title, filing, marks);
        if (pcb.isEmpty()) {
            // Every Eagle library a drawing comes from is a category of its own, under Eagle's.
            return circuit.documents().filedBy(filing);
        }
        EaglePcbImport.Pcb laid = EaglePcbImport.read(pcb.get().pcb(), circuit, pcb.get().file(), title, filing);
        List<DocumentNode> designs = new ArrayList<>(circuit.documents().designs());
        // The pcb names its circuit, so the circuit is stored first; the pcb is what the import stands for.
        designs.add(laid.document());
        List<String> notes = new ArrayList<>(circuit.documents().notes());
        notes.addAll(laid.notes());
        return new ImportedDocuments(circuit.documents().library(), designs, notes).filedBy(filing);
    }

    /** A pcb, and the file it was read from. */
    public record Read(EagleBoard pcb, String file) {
    }
}
