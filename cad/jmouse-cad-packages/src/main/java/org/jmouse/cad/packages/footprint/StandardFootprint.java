package org.jmouse.cad.packages.footprint;

import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.packages.LibraryFiling;
import org.jmouse.el.grammar.Nodes;

import java.util.List;

/**
 * A footprint of a library worked out from a standard's table: its name, the folder it is filed in, how it was worked
 * out, and its drawing. It becomes its document as a tree — {@link #document()} —, which writes itself in either
 * spelling; nothing here writes the language's text.
 *
 * @param designation its package's designation from its kind's {@code designation} template — {@code AXIAL:R0204-P7.62-H}
 *                    —: its technical code once {@code :F} says it is a footprint
 * @param folder      where under the library's footprints its file stands
 * @param category    the category it files itself in, as its table names it: {@code resistors-surface}
 * @param mounting    {@code surface} or {@code through-hole}
 * @param source      the standard its dimensions were read from
 * @param notes       how its copper was worked out, written above its lines for a reviewer
 */
public record StandardFootprint(String slug, String designation, String title, String summary, String folder, String category, String mounting,
                                String source, LandPatternRules rules, List<String> notes, FootprintDrawing drawing) {

    public StandardFootprint {
        notes = List.copyOf(notes);
    }

    /** Its technical code — {@code AXIAL:R0204-P7.62-H:F} —: what a dense list leads with. */
    public String code() {
        return TechnicalClass.FOOTPRINT.code(designation);
    }

    /** The footprint as the tree of its document. */
    public DocumentNode document() {
        DocumentNode document = new DocumentNode(PcbModule.FOOTPRINT, title, slug, null, 1);
        document.addExpression(meta());
        drawing.lines(notes).forEach(document::addExpression);
        return document;
    }

    private MetaNode meta() {
        LibraryFiling filing = rules.filing();
        MetaNode meta = MetaNode.filed(summary, code(), category,
                filing.tags(PcbModule.FOOTPRINT.word(), mounting));
        filing.provenance(source).writeInto(meta);
        return meta;
    }
}
