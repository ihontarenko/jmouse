package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockNode;

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
    private final String       type;
    private final int          line;

    /**
     * @param type        the role within the kind — {@code resistor}, {@code timer} —, or null
     */
    public DocumentNode(DocumentKind kind, String title, String slug, String type, int line) {
        this.kind = kind;
        this.title = title;
        this.slug = slug;
        this.type = type;
        this.line = line;
    }

    /**
     * The same document under another title, holding what it holds and written where it was. Its slug stays: a title is
     * what a document is called and a slug is where it is found.
     */
    public DocumentNode retitled(String newTitle) {
        DocumentNode retitled = new DocumentNode(kind, newTitle, slug, type, line);
        retitled.addLeadingTrivia(getLeadingTrivia());
        retitled.setTrailingTrivia(getTrailingTrivia());
        getExpressions().forEach(retitled::addExpression);
        return retitled;
    }

    /**
     * The same document at another address, holding what it holds: its title stays.
     */
    public DocumentNode readdressed(String newSlug) {
        DocumentNode readdressed = new DocumentNode(kind, title, newSlug, type, line);
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

    /**
     * The word a reference names this document by: its technical code ({@code meta { code "FP.CHIP:R0805" }}), or its
     * slug where it writes none — what every writer of a reference writes, and what a reader matches a reference against.
     */
    public String referenceName() {
        return block(MetaNode.class).flatMap(meta -> meta.text(MetaNode.CODE)).filter(code -> !code.isBlank()).orElse(slug);
    }

    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    public int line() {
        return line;
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

    /** Written as the language writes every document: as terms. */
    @Override
    public void writeSource(SourceWriter writer) {
        new TermDocumentWriter(true).document(writer, this);
    }

    @Override
    public String toString() {
        return kind.word() + " \"" + title + "\"";
    }
}
