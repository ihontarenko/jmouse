package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.List;
import java.util.Set;

/**
 * A board written as a set for a board house: the Gerber set, its assembly tables by the house's profile. The plain set
 * is one, in KiCad's columns; a board house's plugin brings another with its own profile — JLCPCB's, PCBWay's.
 */
public final class FabricationExport implements DocumentExport {

    private final String          id;
    private final String          title;
    private final GerberSet       set;
    private final AssemblyProfile profile;

    /**
     * @param id      the format's word on the exchange routes: {@code jlcpcb}
     * @param profile how its assembly tables are written
     */
    public FabricationExport(String id, String title, GerberSet set, AssemblyProfile profile) {
        this.id = id;
        this.title = title;
        this.set = set;
        this.profile = profile;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(BoardModule.PCB);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        BoardFabrication fabrication = BoardFabrication.of(document, catalogue);
        return new ExportedFiles(fabrication.name() + "-" + id, set.write(fabrication, profile, fabrication.notes()),
                fabrication.notes());
    }
}
