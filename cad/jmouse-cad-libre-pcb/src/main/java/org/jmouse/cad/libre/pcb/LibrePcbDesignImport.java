package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.exchange.imported.SheetMarks;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.libre.pcb.LibrePcbBoard;
import org.jmouse.libre.pcb.LibrePcbProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A LibrePCB project as jM documents: the footprints, symbols and components its library carries, its circuit and its first
 * board, its parts and nets the circuit's. What the import stands for is the pcb where the project has one.
 */
public final class LibrePcbDesignImport {

    private LibrePcbDesignImport() {
    }

    /**
     * @param modelBytes the project's 3D models, by their path inside it
     * @param file  the project's file, which the documents say they were imported from
     * @param title what the circuit is called; the pcb is called the same
     * @throws IllegalArgumentException when the project's files disagree, saying how
     */
    public static ImportedDocuments read(LibrePcbProject project, Map<String, byte[]> modelBytes, String file, String title, LibrePcbFiling filing,
                                         SheetMarks marks) {
        LibrePcbSchematicImport.Converted circuit = LibrePcbSchematicImport.read(project, modelBytes, file, title, filing, marks);
        if (project.boards().isEmpty()) {
            return circuit.documents();
        }
        LibrePcbBoard board = project.boards().getFirst();
        LibrePcbBoardImport.Pcb laid = LibrePcbBoardImport.read(project, board, circuit, file, title, filing);
        // The pcb names its circuit, so the circuit is stored first.
        List<DocumentNode> designs = new ArrayList<>(circuit.documents().designs());
        designs.add(laid.document());
        List<String> notes = new ArrayList<>(circuit.documents().notes());
        notes.addAll(laid.notes());
        if (project.boards().size() > 1) {
            notes.add("The project has " + project.boards().size() + " boards: " + board.name() + " is read, the first of them.");
        }
        return new ImportedDocuments(circuit.documents().library(), designs, notes, circuit.documents().files());
    }
}
