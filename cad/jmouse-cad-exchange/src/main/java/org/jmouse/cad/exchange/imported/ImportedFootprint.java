package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;

import java.util.List;

/**
 * A footprint converted from another product: what it is called — its slug, where it is stored, and its code, the name
 * every reference to it writes ({@link ImportedNames#code}) — and what it says of itself, its pads, the lines it
 * draws, where the placed part's designator and value stand, the 3D models it is shown with, and whose it is — as its
 * footprint document.
 */
public record ImportedFootprint(String title, String slug, String code, String summary, Provenance provenance, List<FootprintLines.Pad> pads,
                                List<FootprintLines.Outline> outlines, List<FootprintLines.Field> fields, List<FootprintLines.Model> models) {

    public ImportedFootprint {
        pads = List.copyOf(pads);
        outlines = List.copyOf(outlines);
        fields = List.copyOf(fields);
        models = List.copyOf(models);
    }

    /** The footprint's document, filed as the product it came from files its footprints. */
    public DocumentNode document(ImportFiling filing) {
        DocumentNode document = new DocumentNode(PcbModule.FOOTPRINT, title, slug, null, 1);
        MetaNode meta = MetaNode.filed(ImportedSummary.shortened(summary), code, filing.categoryOf(provenance), filing.footprintTags());
        provenance.writeInto(meta);
        document.addExpression(meta);
        pads.forEach(pad -> document.addExpression(FootprintLines.pad(pad, List.of())));
        outlines.forEach(outline -> document.addExpression(FootprintLines.outline(outline, List.of())));
        fields.forEach(field -> document.addExpression(FootprintLines.field(field, List.of())));
        models.forEach(model -> document.addExpression(FootprintLines.model(model, List.of())));
        return document;
    }
}
