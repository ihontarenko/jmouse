package org.jmouse.mapper.el.translate;

import org.jmouse.el.lexer.support.SourceWriting;
import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Capability;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.el.translate.Translator;
import org.jmouse.mapper.el.node.AssertionNode;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.IncludeNode;
import org.jmouse.mapper.el.node.LetNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RefuseNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.node.UseNode;

import java.util.Map;

/**
 * A mapping document written back out as {@code .jmm} — and therefore the language's formatter.
 *
 * <h2>⚠️ This is the language's own regression test, and it costs nothing extra</h2>
 *
 * <p>A document that parses, translates and parses again to the same tree has proved the reader and
 * the writer agree about every construct. Nothing else the language has says that: a mapping that
 * works proves the rules that <em>fired</em> were understood, and says nothing about the {@code when}
 * that did not, the {@code refuse} that never tripped, or the {@code : via(…)} nobody exercised.</p>
 *
 * <h2>⚠️ It renders rather than asking the nodes to</h2>
 *
 * <p>{@code .jmp} does the opposite — its nodes carry {@code toSource()} and its translator is a gate
 * in front of them. That is right there and wrong here, and the difference is not taste:</p>
 *
 * <ul>
 *   <li>{@code .jmp} rendering predates the seam, its output is <strong>stored</strong> as policy
 *       revisions an installation reverts to, and moving it would risk changing a byte for nothing.</li>
 *   <li>{@code .jmm} has no rendering at all and never had — {@link org.jmouse.el.node.Expression}'s
 *       default {@code toSource()} throws. So there is nothing to preserve, and a second destination
 *       (see {@link JmmJsonTranslator}) has to walk the tree regardless. Putting the walk in the
 *       translator makes the two destinations the same shape, which is the thing this ticket set out
 *       to find out.</li>
 * </ul>
 *
 * <p>What matters is the rule underneath both, and it holds either way: <strong>there is exactly one
 * writer.</strong> Two would drift.</p>
 *
 * <h2>⚠️ What a rendering is not</h2>
 *
 * <p><strong>Comments do not survive</strong> — the parser does not keep them, so this is a rendering
 * of the mapping rather than an edit of somebody's text.</p>
 *
 * <p><strong>Nor does the order inside a block.</strong> {@link RuleBlockNode} holds its includes, its
 * bindings and its rules as three lists, so a file that interleaved them comes back with the
 * {@code include} lines first, then the {@code let} lines, then the rules. Nothing changes meaning —
 * a {@code let} is resolved by name, not by position — but it is a diff, and somebody saving over
 * their own file has to be told once.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmSourceTranslator implements Translator<String> {

    private static final String INDENT     = "    ";
    private static final String LINE_BREAK = "\n";

    private static final Capabilities EVERYTHING = Capabilities.of("jmm", JmmCapability.every());

    /** The full destination — everything the language can say. */
    public static final JmmSourceTranslator INSTANCE = new JmmSourceTranslator(EVERYTHING);

    private final Capabilities capabilities;

    private JmmSourceTranslator(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * A destination that writes only some of what a mapping can hold.
     *
     * <p>⚠️ It refuses a document holding anything else rather than rendering a subset — a formatter
     * that silently drops a {@code when} produces a file that maps differently.</p>
     *
     * @param capabilities what it will write — from {@link JmmCapability}
     * @return the narrowed destination
     */
    public static JmmSourceTranslator writing(Capability... capabilities) {
        return new JmmSourceTranslator(Capabilities.of("jmm", capabilities));
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    /**
     * Renders a mapping document as source.
     *
     * @param node     a {@link MappingDocumentNode}
     * @param bindings nothing — a mapping names no value a caller supplies
     * @return the file's text, ready to be parsed back
     * @throws TranslationRefusedException where the tree is not a mapping document, holds a construct
     *                                     this destination does not declare, or came with bindings
     */
    @Override
    public String translate(Node node, Bindings bindings) {
        if (!bindings.isEmpty()) {
            throw new TranslationRefusedException(
                    ("the 'jmm' translator supplies nothing by name; a mapping reads its values from "
                     + "the object being mapped, and these would be silently dropped: %s")
                            .formatted(bindings.names()));
        }

        if (!(node instanceof MappingDocumentNode document)) {
            throw new TranslationRefusedException(
                    "the 'jmm' translator writes a mapping document, and %s is not one"
                            .formatted(node.getClass().getSimpleName()));
        }

        new JmmCapabilityCheck(capabilities).check(document);

        StringBuilder source = new StringBuilder();

        write(source, document);

        return source.toString();
    }

    /**
     * Writes the file: its header, what it imports, what it names, and what it builds.
     *
     * @param source where to write
     * @param document the parsed file
     */
    private void write(StringBuilder source, MappingDocumentNode document) {
        source.append("mapping ").append(SourceWriting.literal(document.getName())).append(" {")
                .append(LINE_BREAK).append(LINE_BREAK);

        for (UseNode imported : document.getImports().values()) {
            line(source, 1, "use " + imported.getQualifiedName());
        }

        if (!document.getImports().isEmpty()) {
            source.append(LINE_BREAK);
        }

        for (Map.Entry<String, RuleBlockNode> fragment : document.getFragments().entrySet()) {
            line(source, 1, "fragment " + fragment.getKey() + " {");
            write(source, 2, fragment.getValue());
            line(source, 1, "}");
            source.append(LINE_BREAK);
        }

        for (TargetNode target : document.getTargets()) {
            write(source, target);
        }

        close(source, 0);
    }

    /**
     * Writes one {@code target} block.
     *
     * <p>⚠️ {@code unmapped ignore} is left unwritten and {@code unmapped fail} never is. Nothing is
     * lost: {@code ignore} is what a target with no line means, so a document cannot tell the terse
     * form from the spelled-out one. What it buys is that the line which <em>changes</em> what happens
     * is the one thing a reader cannot skim past.</p>
     *
     * @param source where to write
     * @param target the block
     */
    private void write(StringBuilder source, TargetNode target) {
        line(source, 1, "target " + target.getTargetType() + " {");
        source.append(LINE_BREAK);

        if (target.getUnmapped() == TargetNode.Unmapped.FAIL) {
            line(source, 2, "unmapped fail");
            source.append(LINE_BREAK);
        }

        if (target.getAlways() != null && !target.getAlways().isEmpty()) {
            line(source, 2, "always {");
            write(source, 3, target.getAlways());
            line(source, 2, "}");
            source.append(LINE_BREAK);
        }

        for (RefuseNode refusal : target.getRefusals()) {
            write(source, 2, refusal);
        }

        for (FromNode from : target.getSources()) {
            write(source, from);
        }

        close(source, 1);
        source.append(LINE_BREAK);
    }

    /**
     * Writes one {@code from} block, or the one-line form that converts the pair whole.
     *
     * @param source where to write
     * @param from   the block
     */
    private void write(StringBuilder source, FromNode from) {
        if (from.isConverted()) {
            line(source, 2, "from " + from.getSourceType() + " : " + from.getConversion());
            source.append(LINE_BREAK);

            return;
        }

        line(source, 2, "from " + from.getSourceType() + " {");
        source.append(LINE_BREAK);

        if (from.getRefusal() != null) {
            write(source, 3, from.getRefusal());
        }

        write(source, 3, from.getRules());
        close(source, 2);
        source.append(LINE_BREAK);
    }

    /**
     * Writes one {@code refuse} block.
     *
     * @param source where to write
     * @param depth  how far in
     * @param refusal the block
     */
    private void write(StringBuilder source, int depth, RefuseNode refusal) {
        line(source, depth, "refuse %s %s {".formatted(
                refusal.getSubject().name().toLowerCase(), refusal.getPhase().name().toLowerCase()));

        for (AssertionNode assertion : refusal.getAssertions()) {
            line(source, depth + 1,
                 assertion.getCondition() + " : " + SourceWriting.literal(assertion.getMessage()));
        }

        line(source, depth, "}");
        source.append(LINE_BREAK);
    }

    /**
     * Writes a block of rules — includes first, then bindings, then the rules themselves.
     *
     * @param source where to write
     * @param depth  how far in
     * @param block  the rules, which may be {@code null} for a source that declared none
     */
    private void write(StringBuilder source, int depth, RuleBlockNode block) {
        if (block == null) {
            return;
        }

        for (IncludeNode include : block.getIncludes()) {
            line(source, depth, "include " + include.getName());
        }

        for (LetNode binding : block.getBindings()) {
            line(source, depth, "let " + binding.getName() + " = " + binding.getExpression());
        }

        for (RuleNode rule : block.getRules().values()) {
            line(source, depth, write(rule));
        }
    }

    /**
     * Writes one rule.
     *
     * @param rule the rule
     * @return the line, without its indent
     */
    private String write(RuleNode rule) {
        if (rule.isIgnored()) {
            return rule.getProperty() + " : ignore";
        }

        String line = rule.getProperty() + " : " + rule.getValue();

        if (rule.getCondition() != null) {
            line += " when " + rule.getCondition();
        }

        return line;
    }

    /**
     * Closes a block, with no blank line left hanging above the brace.
     *
     * <p>⚠️ Blocks are separated by a blank line <em>after</em> each one, which is the only way to write
     * it without every caller knowing whether it is the last. The cost is a stray blank before each
     * closing brace, and this is where it is paid — once, rather than by nine callers each remembering
     * not to leave one.</p>
     *
     * @param source where to write
     * @param depth  how far in the brace sits
     */
    private void close(StringBuilder source, int depth) {
        while (source.length() >= 2 && source.charAt(source.length() - 1) == '\n'
               && source.charAt(source.length() - 2) == '\n') {
            source.deleteCharAt(source.length() - 1);
        }

        line(source, depth, "}");
    }

    /**
     * Appends one line at a depth.
     *
     * <p>The line break is {@code \n} rather than the platform's, because what this produces is a
     * {@code .jmm} file's text — the same on every machine that reads it back.</p>
     *
     * @param source where to write
     * @param depth  how many indents
     * @param text   the line
     */
    private void line(StringBuilder source, int depth, String text) {
        source.append(INDENT.repeat(depth)).append(text).append(LINE_BREAK);
    }
}
