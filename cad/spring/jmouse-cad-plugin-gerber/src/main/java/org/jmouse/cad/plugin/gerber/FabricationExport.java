package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.exchange.ExportPurpose;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.List;
import java.util.function.Supplier;
import java.util.Set;

/**
 * A pcb written as a set for a pcb house: the Gerber set, its assembly tables by the house's profile. The plain set
 * is one, in KiCad's columns; a pcb house's plugin brings another with its own profile — JLCPCB's, PCBWay's.
 */
public final class FabricationExport implements DocumentExport {

    private final String          id;
    private final String          title;
    private final GerberSet       set;
    private final Supplier<AssemblyProfile> profile;

    /**
     * @param id      the format's word on the exchange routes: {@code jlcpcb}
     * @param profile how its assembly tables are written, as the installation has it when a pcb is written
     */
    public FabricationExport(String id, String title, GerberSet set, Supplier<AssemblyProfile> profile) {
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

    /** A pcb house makes the pcb from these files: written only from a pcb its checks pass. */
    @Override
    public ExportPurpose purpose() {
        return ExportPurpose.FABRICATION;
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(PcbModule.PCB);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        PcbFabrication fabrication = PcbFabrication.of(document, catalogue);
        return new ExportedFiles(fabrication.name() + "-" + id, set.write(fabrication, profile.get(), fabrication.notes()),
                fabrication.notes());
    }
}
