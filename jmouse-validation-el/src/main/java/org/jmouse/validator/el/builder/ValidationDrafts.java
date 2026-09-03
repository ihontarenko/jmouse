package org.jmouse.validator.el.builder;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.validator.el.builder.ValidationDraft.CheckDraft;
import org.jmouse.validator.el.builder.ValidationDraft.ItemDraft;
import org.jmouse.validator.el.node.CheckBlockNode;
import org.jmouse.validator.el.node.CheckLineNode;
import org.jmouse.validator.el.node.CheckNode;
import org.jmouse.validator.el.node.InvariantNode;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.node.WhenBranchNode;
import org.jmouse.validator.el.node.WhenNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows into a document, and a document back into rows. 🔁
 *
 * <h2>⚠️ The rendering is not here, and that is the point</h2>
 *
 * <p>{@link #toDocument} builds a {@link ValidationDocumentNode} and stops. Turning that into
 * {@code .jmv} text is {@code JmvWriter}'s job — the same call an editor's save goes through — so a
 * builder and an editor cannot produce different text for the same validation. A helper here returning
 * a {@code String} would be a second writer of the language sitting beside the compiler, which is the
 * one shape this codebase has decided against everywhere it has come up.</p>
 *
 * <h2>⚠️ The other direction refuses rather than narrows</h2>
 *
 * <p>Somebody edits the text and switches back to the form. {@link #toDraft} rebuilds the rows — and
 * where the document holds something no row can carry, it <strong>says so and stops</strong>.</p>
 *
 * <p>The alternative is a form that shows what it understands and drops the rest. That form saves, and
 * the save deletes what somebody wrote. A builder that quietly narrows a file is worse than no builder:
 * the loss is invisible, it is permanent, and it happens to whoever trusted the tool most.</p>
 *
 * <p>⚠️ As the language stands, <strong>nothing is unshowable</strong> — the draft mirrors the tree,
 * nesting included. The refusal exists for the day the grammar grows past the form, which is exactly
 * the day nobody remembers to check.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ValidationDrafts {

    private ValidationDrafts() {
    }

    // ── Rows into a document ─────────────────────────────────────────────────────

    /**
     * Builds the document a draft describes.
     *
     * @param draft what the form holds
     * @return the tree, ready for a writer
     */
    public static ValidationDocumentNode toDocument(ValidationDraft draft) {
        ValidationDocumentNode document = new ValidationDocumentNode();

        document.setName(draft.name());
        document.addLeadingTrivia(triviaOf(draft.comments()));

        for (ItemDraft item : draft.items()) {
            document.addExpression(nodeOf(item));
        }

        return document;
    }

    /**
     * Builds one row's node.
     *
     * @param item the row
     * @return its node
     */
    private static Expression nodeOf(ItemDraft item) {
        Expression node = switch (item.kind()) {
            case BLOCK -> blockOf(item);
            case LINE -> lineOf(item);
            case GUARD -> guardOf(item);
            case INVARIANT -> invariantOf(item);
        };

        node.addLeadingTrivia(triviaOf(item.comments()));

        if (item.note() != null) {
            node.setTrailingTrivia(Trivia.comment(item.note()));
        }

        return node;
    }

    private static CheckBlockNode blockOf(ItemDraft item) {
        CheckBlockNode block = new CheckBlockNode();

        block.setKind(CheckBlockNode.Kind.valueOf(item.block().toUpperCase()));
        item.items().forEach(inner -> block.addExpression(nodeOf(inner)));

        return block;
    }

    private static CheckLineNode lineOf(ItemDraft item) {
        CheckLineNode line = new CheckLineNode();

        line.setField(item.field());
        line.setMessage(item.message());
        line.setChecksNote(item.checksNote());

        for (CheckDraft check : item.checks()) {
            CheckNode node = new CheckNode();

            node.setName(check.check());
            node.setStop(check.stop());
            node.setMessage(check.message());
            check.positional().forEach(node::addPositional);
            check.named().forEach(node::addNamed);

            line.addCheck(node);
        }

        return line;
    }

    /**
     * Builds a guard's node.
     *
     * <p>⚠️ An {@code otherwise} branch is added only where the draft carries one. A branch that was
     * never written and an empty one are different statements, and adding an empty branch for a
     * {@code null} would put {@code otherwise { }} into a file nobody wrote it in.</p>
     */
    private static WhenNode guardOf(ItemDraft item) {
        WhenNode        guard    = new WhenNode();
        WhenBranchNode  guarded  = new WhenBranchNode();

        guarded.setCondition(item.condition());
        item.items().forEach(inner -> guarded.addExpression(nodeOf(inner)));
        guard.addBranch(guarded);

        if (item.otherwise() == null) {
            return guard;
        }

        WhenBranchNode otherwise = new WhenBranchNode();

        item.otherwise().forEach(inner -> otherwise.addExpression(nodeOf(inner)));
        guard.addBranch(otherwise);

        return guard;
    }

    private static InvariantNode invariantOf(ItemDraft item) {
        InvariantNode invariant = new InvariantNode();

        invariant.setCondition(item.condition());
        invariant.setMessage(item.message());

        return invariant;
    }

    // ── A document back into rows ────────────────────────────────────────────────

    /**
     * Rebuilds the rows a document describes.
     *
     * @param document what was parsed
     * @return the rows
     * @throws UnshowableValidationException where the document holds what no row can carry
     */
    public static ValidationDraft toDraft(ValidationDocumentNode document) {
        return new ValidationDraft(document.getName(), commentsOf(document), rowsOf(document.getBody()));
    }

    /**
     * Rebuilds a list of rows.
     *
     * @param statements what to walk
     * @return the rows
     */
    private static List<ItemDraft> rowsOf(List<Expression> statements) {
        List<ItemDraft> rows = new ArrayList<>(statements.size());

        for (Expression statement : statements) {
            rows.add(rowOf(statement));
        }

        return rows;
    }

    /**
     * Rebuilds one row.
     *
     * @param statement what to read
     * @return the row, carrying what was written around it
     */
    private static ItemDraft rowOf(Expression statement) {
        ItemDraft row = switch (statement) {
            case CheckBlockNode block -> ItemDraft.block(block.getKind().name().toLowerCase(),
                                                         rowsOf(block.getExpressions()));
            case CheckLineNode line -> ItemDraft.line(line.getField(), checksOf(line),
                                                      line.getMessage());
            case WhenNode guard -> guardRowOf(guard);
            case InvariantNode invariant -> ItemDraft.invariant(invariant.getCondition(),
                                                                invariant.getMessage());
            default -> throw new UnshowableValidationException(
                    statement.getClass().getSimpleName(),
                    "the form has no row for it. Edit this document as text, or take the construct out "
                    + "and put it back once the form knows it");
        };

        return row.with(commentsOf(statement), noteOf(statement),
                        statement instanceof CheckLineNode line ? line.getChecksNote() : null);
    }

    private static ItemDraft guardRowOf(WhenNode guard) {
        List<ItemDraft> guarded   = List.of();
        List<ItemDraft> otherwise = null;

        for (WhenBranchNode branch : guard.getBranches()) {
            if (branch.isGuarded()) {
                guarded = rowsOf(branch.getExpressions());

                continue;
            }

            otherwise = rowsOf(branch.getExpressions());
        }

        return ItemDraft.guard(condition(guard), guarded, otherwise);
    }

    /**
     * A guard's condition, refusing one that reached here without any.
     *
     * @param guard the guard
     * @return its condition
     */
    private static String condition(WhenNode guard) {
        for (WhenBranchNode branch : guard.getBranches()) {
            if (branch.isGuarded()) {
                return branch.getCondition();
            }
        }

        throw new UnshowableValidationException("when", "a guard reached the form with no condition");
    }

    private static List<CheckDraft> checksOf(CheckLineNode line) {
        List<CheckDraft> checks = new ArrayList<>(line.getChecks().size());

        for (CheckNode check : line.getChecks()) {
            checks.add(new CheckDraft(check.getName(), check.getPositional(), check.getNamed(),
                                      check.isStop(), check.getMessage()));
        }

        return checks;
    }

    // ── Comments, both ways ──────────────────────────────────────────────────────

    /**
     * What was written above a node, as lines a form can show.
     *
     * <p>⚠️ A blank line travels as an empty string. Dropping it would let the form collapse a
     * document's paragraphs on the way through — losing no characters and all of the grouping.</p>
     *
     * @param node whose trivia to read
     * @return the lines
     */
    private static List<String> commentsOf(Node node) {
        List<Trivia> written = node.getLeadingTrivia();

        if (written.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(written.size());

        written.forEach(one -> lines.add(one.isBlank() ? "" : one.text()));

        return lines;
    }

    /**
     * What was written after a node on its line.
     *
     * @param node whose trivia to read
     * @return it, or {@code null}
     */
    private static String noteOf(Node node) {
        Trivia trailing = node.getTrailingTrivia();

        return trailing == null ? null : trailing.text();
    }

    /**
     * Lines a form holds, as trivia a document carries.
     *
     * @param lines what the form holds
     * @return the trivia
     */
    private static List<Trivia> triviaOf(List<String> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }

        List<Trivia> written = new ArrayList<>(lines.size());

        lines.forEach(line -> written.add(line.isBlank() ? Trivia.blank() : Trivia.comment(line)));

        return written;
    }
}
