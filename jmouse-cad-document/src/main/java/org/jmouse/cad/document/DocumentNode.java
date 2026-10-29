package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockNode;
import org.jmouse.el.grammar.StatementWriting;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.writer.SourceWriter;

import java.util.List;
import java.util.Optional;

/**
 * A document — {@code kind "Title" [as slug] [type role] { … }} — as the tree it is read into: its head, and the
 * statements of its body (its declarations and its blocks) as its expressions, in the order written. The tree is the
 * model: what reads a document asks the nodes, and the document writes itself back from them.
 */
public class DocumentNode extends ExpressionsNode {

    private final DocumentKind kind;
    private final String       title;
    private final String       slug;
    private final boolean      slugWritten;
    private final String       type;
    private final int          line;
    private Spelling           spelling = Spelling.BLOCKS;

    /** How a document is written: its blocks with their words, or as terms — {@code component:diode("Diode") { … }}. */
    public enum Spelling { BLOCKS, TERMS }

    /**
     * @param slugWritten whether the head writes {@code as slug}, rather than the slug following from the title
     * @param type        the role within the kind — {@code resistor}, {@code timer} —, or null
     */
    public DocumentNode(DocumentKind kind, String title, String slug, boolean slugWritten, String type, int line) {
        this.kind = kind;
        this.title = title;
        this.slug = slug;
        this.slugWritten = slugWritten;
        this.type = type;
        this.line = line;
    }

    /**
     * The same document under another title, holding what it holds, spelled as it was and written where it was. Its slug
     * stays: a slug that followed from the old title is written out when the new one would give another, since a title
     * is what a document is called and a slug is where it is found.
     */
    public DocumentNode retitled(String newTitle) {
        boolean written = slugWritten || !DocumentParser.slugOf(newTitle).equals(slug);
        DocumentNode retitled = new DocumentNode(kind, newTitle, slug, written, type, line);
        retitled.spelledAs(spelling);
        retitled.addLeadingTrivia(getLeadingTrivia());
        retitled.setTrailingTrivia(getTrailingTrivia());
        getExpressions().forEach(retitled::addExpression);
        return retitled;
    }

    /**
     * The same document at another address, holding what it holds, spelled as it was: its title stays, and its slug is
     * written out unless the title gives it.
     */
    public DocumentNode readdressed(String newSlug) {
        DocumentNode readdressed = new DocumentNode(kind, title, newSlug, !DocumentParser.slugOf(title).equals(newSlug), type, line);
        readdressed.spelledAs(spelling);
        readdressed.addLeadingTrivia(getLeadingTrivia());
        readdressed.setTrailingTrivia(getTrailingTrivia());
        getExpressions().forEach(readdressed::addExpression);
        return readdressed;
    }

    public DocumentKind kind() {
        return kind;
    }

    public String title() {
        return title;
    }

    public String slug() {
        return slug;
    }

    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    public int line() {
        return line;
    }

    /** How its text spelled it, so that what is written back of it after an edit is spelled the same. */
    public Spelling spelling() {
        return spelling;
    }

    /** Records how its text spelled it: the reader's, once it has read a term's head. */
    public void spelledAs(Spelling spelling) {
        this.spelling = spelling;
    }

    /** Every block of the body, in the order written. */
    public List<BlockNode> blocks() {
        return blocks(BlockNode.class);
    }

    /** Every block of a type: a document may hold several {@code expect}. */
    public <T extends BlockNode> List<T> blocks(Class<T> type) {
        return getExpressions().stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** The first block of a type, if the document holds one. */
    public <T extends BlockNode> Optional<T> block(Class<T> type) {
        return blocks(type).stream().findFirst();
    }

    /** Every block opened by a word — every {@code expect} —, in the order written. */
    public List<BlockNode> blocks(String word) {
        return blocks().stream().filter(block -> block.word().equals(word)).toList();
    }

    /** The block opened by a word, if the document holds one. */
    public Optional<BlockNode> block(String word) {
        return blocks(word).stream().findFirst();
    }

    /** What a declaration names — {@code use cmos-hc, smooth} —; empty when the document writes none. */
    public List<String> declared(String word) {
        return getExpressions().stream().filter(DeclarationNode.class::isInstance).map(DeclarationNode.class::cast)
                .filter(declaration -> declaration.word().equals(word)).flatMap(declaration -> declaration.names().stream())
                .toList();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        StatementWriting.leading(writer, this);
        writer.word(kind.word()).text(title);
        if (slugWritten) {
            writer.word("as").word(slug);
        }
        if (type != null) {
            writer.word("type").word(type);
        }
        writer.block(body -> StatementWriting.statements(body, getExpressions()));
    }

    @Override
    public String toString() {
        return kind.word() + " \"" + title + "\"";
    }
}
