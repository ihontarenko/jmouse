package org.jmouse.validator.el.translate;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.validator.el.node.CheckBlockNode;
import org.jmouse.validator.el.node.CheckLineNode;
import org.jmouse.validator.el.node.CheckNode;
import org.jmouse.validator.el.node.InvariantNode;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.node.WhenBranchNode;
import org.jmouse.validator.el.node.WhenNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a validation tree back into {@code .jmv} source. ✍️
 *
 * <h2>⚠️ A translator, not a {@code toString}</h2>
 *
 * <p>Compiling a tree for a runtime and rendering it back into text are the same operation with
 * different destinations, and the engine says so in one seam. That is not tidiness: a
 * {@code String}-returning helper sitting beside the compiler is a <strong>second implementation of
 * the language</strong>, and the two drift the moment the grammar grows — until a document written by
 * one and read by the other means two things. jMQ learned this already.</p>
 *
 * <h2>⚠️ This is what the form-builder saves through</h2>
 *
 * <p>A builder edits a document as rows and has to write a file. Every rendering path goes through
 * here, so a check the builder can draw is a check the file can carry, by construction.</p>
 *
 * <h2>⚠️ Comments and blank lines come back out</h2>
 *
 * <p>They are carried on the nodes as {@code Trivia} and printed here — the header a file opens with,
 * the paragraph above a check line, the aside at the end of one, and the empty lines that group them.
 * Without that, a builder that opened a document and saved it would throw away every explanation
 * somebody wrote, with no diff to notice it in.</p>
 *
 * <p>⚠️ <strong>One remainder, deliberately.</strong> Trivia standing after the last statement of a
 * block, with nothing below it to lead, is not yet emitted: it is collected onto the synthetic
 * container {@code StatementsParser} builds, and no node in this language owns that position. A parting
 * comment inside a block is therefore still lost; a header, an aside and everything between statements
 * is not.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvWriter implements ValidationTranslator<String> {

    private static final String INDENT     = "    ";
    private static final String LINE_BREAK = System.lineSeparator();

    /**
     * ⚠️ Everything, because a renderer of the language can write down whatever the language can say.
     * A translator into somebody else's evaluator declares less; this one is the language itself.
     */
    @Override
    public Capabilities capabilities() {
        return JmvCapability.everything("jmv-writer");
    }

    /**
     * Renders a node — a whole document, or one statement of one.
     *
     * @param node     what to render
     * @param bindings ⚠️ unused: rendering substitutes nothing. The seam is shared, and a destination
     *                 that binds values while translating — SQL does — needs the parameter.
     * @return the source
     */
    @Override
    public String translate(Node node, Bindings bindings) {
        requireSupport(node);

        if (node instanceof ValidationDocumentNode document) {
            return document(document);
        }

        if (node instanceof Expression statement) {
            return String.join(LINE_BREAK, statement(statement, ""));
        }

        throw new TranslationRefusedException(
                "the jMV writer renders a validation document or one of its statements, and was handed "
                + (node == null ? "nothing" : node.getClass().getSimpleName()));
    }

    /**
     * Renders a whole document.
     *
     * @param document what to render
     * @return the source, ending in a line break
     */
    private String document(ValidationDocumentNode document) {
        List<String> lines = new ArrayList<>(trivia(document, ""));

        lines.add("validation " + quoted(document.getName()) + " {");

        for (Expression statement : document.getExpressions()) {
            // ⚠️ Only where the document does not already say. A parsed statement carries whatever
            // spacing its author left, blank lines included; adding one anyway would put a second empty
            // line above every block on every save, and the file would grow a line each time.
            if (statement.getLeadingTrivia().isEmpty()) {
                lines.add("");
            }

            lines.addAll(statement(statement, INDENT));
        }

        lines.add("}");

        return String.join(LINE_BREAK, lines) + LINE_BREAK;
    }

    /**
     * Renders one statement.
     *
     * @param statement what to render
     * @param indent    what every line of it opens with
     * @return its lines
     */
    private List<String> statement(Expression statement, String indent) {
        List<String> lines = new ArrayList<>(trivia(statement, indent));

        lines.addAll(switch (statement) {
            case CheckBlockNode block -> block(block, indent);
            case CheckLineNode line -> line(line, indent);
            case WhenNode guard -> guard(guard, indent);
            case InvariantNode invariant -> List.of(indent + "invariant " + invariant.getCondition()
                                                    + " : " + invariant.getMessage());
            default -> throw new TranslationRefusedException(
                    "'%s' is not something a validation document holds"
                            .formatted(statement.getClass().getSimpleName()));
        });

        // ⚠️ On the LAST line of whatever was rendered, not the first: a trailing comment sat at the end
        // of its statement, and a statement that spans several lines still ends where it ends.
        if (statement.getTrailingTrivia() != null && !lines.isEmpty()) {
            lines.set(lines.size() - 1,
                      lines.getLast() + "   " + statement.getTrailingTrivia().text());
        }

        return lines;
    }

    /**
     * Renders a {@code gate} or an {@code always}.
     *
     * @param block  what to render
     * @param indent what every line of it opens with
     * @return its lines
     */
    private List<String> block(CheckBlockNode block, String indent) {
        List<String> lines = new ArrayList<>();

        lines.add(indent + block.getKind().name().toLowerCase() + " {");

        for (Expression statement : block.getExpressions()) {
            lines.addAll(statement(statement, indent + INDENT));
        }

        lines.add(indent + "}");

        return lines;
    }

    /**
     * Renders a {@code when} and its {@code otherwise}.
     *
     * <p>⚠️ The two branches are rendered as they were written, never merged or flattened. Depth and
     * flatness mean the same thing to the runtime and different things to a reader, and a writer that
     * chose between them would be arguing with whoever wrote the file.</p>
     *
     * @param guard  what to render
     * @param indent what every line of it opens with
     * @return its lines
     */
    private List<String> guard(WhenNode guard, String indent) {
        List<String> lines = new ArrayList<>();

        for (WhenBranchNode branch : guard.getBranches()) {
            String opening = branch.isGuarded()
                    ? indent + "when " + branch.getCondition() + " {"
                    : indent + "} otherwise {";

            // ⚠️ The `otherwise` reuses the previous branch's closing brace, so it has to replace the
            // line that closed it rather than follow it.
            if (!branch.isGuarded() && !lines.isEmpty()) {
                lines.removeLast();
            }

            lines.add(opening);

            for (Expression statement : branch.getExpressions()) {
                lines.addAll(statement(statement, indent + INDENT));
            }

            lines.add(indent + "}");
        }

        return lines;
    }

    /**
     * Renders one check line, and its continuation line where it carries one.
     *
     * @param line   what to render
     * @param indent what every line of it opens with
     * @return its lines
     */
    private List<String> line(CheckLineNode line, String indent) {
        List<String> rendered = new ArrayList<>(line.getChecks().size());

        for (CheckNode check : line.getChecks()) {
            rendered.add(check(check));
        }

        List<String> lines  = new ArrayList<>();
        String       checks = indent + line.getField() + " : " + String.join(", ", rendered);

        // ⚠️ On the checks, not on the message — a wrapped line has two ends, and an aside written on
        // the first one moves a line down on every save if it is printed on the second.
        lines.add(line.getChecksNote() == null ? checks : checks + "   " + line.getChecksNote());

        // ⚠️ A line message is a continuation line and cannot be written inline — inline it would be
        // indistinguishable from a message on the last check. The indent lines it up under the checks,
        // which is what makes it read as belonging to all of them.
        if (line.getMessage() != null) {
            lines.add(indent + " ".repeat(line.getField().length()) + " : " + line.getMessage());
        }

        return lines;
    }

    /**
     * Renders one check.
     *
     * @param check what to render
     * @return it, as it would be written
     */
    private String check(CheckNode check) {
        StringBuilder written = new StringBuilder(check.getName());

        List<String> arguments = new ArrayList<>(check.getPositional());

        for (Map.Entry<String, String> named : check.getNamed().entrySet()) {
            arguments.add(named.getKey() + ": " + named.getValue());
        }

        if (!arguments.isEmpty()) {
            written.append('(').append(String.join(", ", arguments)).append(')');
        }

        if (check.isStop()) {
            written.append(" stop");
        }

        if (check.getMessage() != null) {
            written.append(" : ").append(check.getMessage());
        }

        return written.toString();
    }

    /**
     * The comments and blank lines written above a node, ready to print.
     *
     * <p>⚠️ Emitted at the node's own indent, which is what keeps a comment attached to what it was
     * written about rather than drifting to the margin.</p>
     *
     * @param node   whose trivia to render
     * @param indent what every line of it opens with
     * @return the lines, empty where there was none
     */
    private List<String> trivia(Node node, String indent) {
        List<Trivia> written = node.getLeadingTrivia();

        if (written.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(written.size());

        for (Trivia one : written) {
            lines.add(one.isBlank() ? "" : indent + one.text());
        }

        return lines;
    }

    /**
     * A document's name, quoted the way a file writes it.
     *
     * @param name what the document calls itself
     * @return the quoted name
     */
    private static String quoted(String name) {
        return "\"" + name + "\"";
    }
}
