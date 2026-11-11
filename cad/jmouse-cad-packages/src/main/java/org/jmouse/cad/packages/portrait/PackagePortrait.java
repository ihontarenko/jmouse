package org.jmouse.cad.packages.portrait;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.packages.LibraryFiling;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Lines;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.node.Expression;

import java.util.List;

/**
 * A package's portrait worked out from its table: its name, the folder it is filed in, and its drawing's statements —
 * its pins and its elements. It becomes its document as a tree — {@link #document()} —, which writes itself.
 *
 * @param code     its technical code, {@code PT.DIP:8-W7.62}
 * @param mounting {@code through-hole} or {@code surface-mount}, its tag
 * @param source   the standard its outline was read from; empty when the table names none
 * @param note     how it was worked out, written above its head for a reviewer
 * @param input    the input its component gives the printed marking's words by
 */
public record PackagePortrait(String slug, String code, String title, String summary, String folder, String mounting,
                              String source, LibraryFiling filing, String note, double width, double height, String input,
                              List<Expression> statements) {

    /** The tag a package's portrait carries beside a part's. */
    private static final String PACKAGE = "package";

    public PackagePortrait {
        statements = List.copyOf(statements);
    }

    /** The portrait as the tree of its document. */
    public DocumentNode document() {
        DocumentNode document = new DocumentNode(SchematicModule.PORTRAIT, title, slug, true, null, 1);
        document.addLeadingTrivia(List.of(Lines.comment(note)));
        document.addExpression(meta());
        document.addExpression(LineBuilder.line(DrawingGrammar.SIZE).slot("extent", Nodes.extent(width, height)).build());
        // The words on the body are the part's: its component binds them, and the package alone prints none.
        document.addExpression(LineBuilder.line(DrawingGrammar.INPUT).slot("name", Nodes.name(input)).flag("text").build());
        statements.forEach(document::addExpression);
        return document;
    }

    private MetaNode meta() {
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary)));
        meta.addExpression(MetaLineNode.of(MetaNode.CODE, Nodes.text(code)));
        meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(filing.category())));
        meta.addExpression(MetaLineNode.of(MetaNode.TAGS, Nodes.words(filing.tags(SchematicModule.PORTRAIT.word(), PACKAGE, mounting))));
        if (!source.isEmpty()) {
            meta.addExpression(MetaLineNode.of(MetaNode.SOURCE, Nodes.text(source)));
        }
        meta.addExpression(MetaLineNode.of(MetaNode.AUTHORS, Nodes.text(filing.authors())));
        meta.addExpression(MetaLineNode.of(MetaNode.LICENSE, Nodes.text(filing.license())));
        return meta;
    }
}
