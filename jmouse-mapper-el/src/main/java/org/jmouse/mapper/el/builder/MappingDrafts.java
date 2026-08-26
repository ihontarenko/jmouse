package org.jmouse.mapper.el.builder;

import org.jmouse.mapper.el.builder.MappingDraft.MappingRow;
import org.jmouse.mapper.el.builder.MappingDraft.SourceDraft;
import org.jmouse.mapper.el.builder.MappingDraft.TargetDraft;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.node.UseNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rows into a document, and a document back into rows. 🔁
 *
 * <h2>⚠️ The rendering is not here, and that is the point</h2>
 *
 * <p>This builds {@code MappingDocumentNode} and stops. Turning that into {@code .jmm} text is
 * {@code JmmSourceTranslator}'s job — the same call an editor's save goes through — so a builder and an
 * editor cannot produce different text for the same mapping. A helper here that returned a String would
 * be a second writer of the language sitting beside the compiler, which is the one shape this codebase
 * has decided against everywhere it has come up.</p>
 *
 * <h2>⚠️ The other direction, and why it refuses rather than narrows</h2>
 *
 * <p>Somebody edits the text and switches back to the form. {@link #toDraft} rebuilds the rows — and
 * when the document holds something a row cannot carry, it <strong>says so and stops</strong>.</p>
 *
 * <p>The alternative is a form that shows what it understands and drops the rest. That form saves, and
 * the save deletes a fragment somebody wrote. A builder that quietly narrows a file is worse than no
 * builder: the loss is invisible, it is permanent, and it happens to the person who trusted the tool
 * most.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MappingDrafts {

    private MappingDrafts() {
    }

    /**
     * Builds the document a draft describes.
     *
     * @param draft what the form holds
     * @return the tree, ready for a translator
     */
    public static MappingDocumentNode toDocument(MappingDraft draft) {
        MappingDocumentNode document = new MappingDocumentNode();

        document.setName(draft.name());

        for (String imported : draft.imports()) {
            UseNode node = new UseNode();

            node.setQualifiedName(imported);
            document.add(node);
        }

        for (TargetDraft target : draft.targets()) {
            document.add(toNode(target));
        }

        return document;
    }

    /**
     * One target block.
     *
     * @param draft the target
     * @return the node
     */
    private static TargetNode toNode(TargetDraft draft) {
        TargetNode target = new TargetNode();

        target.setTargetType(draft.type());

        // ⚠️ An empty `always` is left unset rather than set to an empty block. A block with nothing in
        // it renders as two braces around nothing, and a form with no always-rows did not ask for that.
        if (!draft.always().isEmpty()) {
            target.setAlways(toBlock(draft.always()));
        }

        for (SourceDraft source : draft.sources()) {
            FromNode from = new FromNode();

            from.setSourceType(source.type());
            from.setRules(toBlock(source.rules()));
            target.add(from);
        }

        return target;
    }

    /**
     * A block of rules.
     *
     * @param rows the rows
     * @return the block
     */
    private static RuleBlockNode toBlock(List<MappingRow> rows) {
        RuleBlockNode block = new RuleBlockNode();

        for (MappingRow row : rows) {
            RuleNode rule = new RuleNode();

            rule.setProperty(row.target());

            if (row.ignored()) {
                // ⚠️ An ignored rule carries no value. Setting one anyway would make a document that
                // round-trips into something with both an ignore and an expression on one line, which
                // the language has no reading for.
                rule.setIgnored(true);
            } else {
                rule.setValue(row.expression());
                rule.setCondition(row.condition());
            }

            block.add(rule);
        }

        return block;
    }

    /**
     * Rebuilds the rows a document describes.
     *
     * @param document the parsed file
     * @return the draft
     * @throws UnshowableMappingException when the document holds something a row cannot carry
     */
    public static MappingDraft toDraft(MappingDocumentNode document) {
        if (!document.getFragments().isEmpty()) {
            throw new UnshowableMappingException("fragment",
                    "a fragment is rules named once and included where they are wanted, and a row "
                    + "carries one rule");
        }

        List<String> imports = new ArrayList<>();

        for (UseNode imported : document.getImports().values()) {
            imports.add(imported.getQualifiedName());
        }

        List<TargetDraft> targets = new ArrayList<>();

        for (TargetNode target : document.getTargets()) {
            targets.add(toDraft(target));
        }

        return new MappingDraft(document.getName(), imports, targets);
    }

    /**
     * One target block, as rows.
     *
     * @param target the block
     * @return the draft
     */
    private static TargetDraft toDraft(TargetNode target) {
        if (target.getUnmapped() == TargetNode.Unmapped.FAIL) {
            throw new UnshowableMappingException("unmapped fail",
                    "it is a check on the whole target rather than on any one property, and the form "
                    + "has no row to put it on");
        }

        if (!target.getRefusals().isEmpty()) {
            throw new UnshowableMappingException("refuse",
                    "an assertion is a condition and a message, which is not the shape of a rule row");
        }

        List<SourceDraft> sources = new ArrayList<>();

        for (FromNode from : target.getSources()) {
            if (from.isConverted()) {
                throw new UnshowableMappingException("from … : via(…)",
                        "the pair converts whole, so there are no properties to show rows for");
            }

            if (from.getRefusal() != null) {
                throw new UnshowableMappingException("refuse source",
                        "an assertion is a condition and a message, which is not the shape of a rule row");
            }

            sources.add(new SourceDraft(from.getSourceType(), toRows(from.getRules())));
        }

        return new TargetDraft(target.getTargetType(), toRows(target.getAlways()), sources);
    }

    /**
     * A block of rules, as rows.
     *
     * @param block the block, which may be {@code null} for a target with no {@code always}
     * @return the rows
     */
    private static List<MappingRow> toRows(RuleBlockNode block) {
        if (block == null) {
            return List.of();
        }

        if (!block.getIncludes().isEmpty()) {
            throw new UnshowableMappingException("include",
                    "it pulls in rules written elsewhere, and the form would have to show them as if "
                    + "they were written here");
        }

        if (!block.getBindings().isEmpty()) {
            throw new UnshowableMappingException("let",
                    "a binding is a value named once and used by several rows, which no single row owns");
        }

        List<MappingRow> rows = new ArrayList<>();

        for (Map.Entry<String, RuleNode> rule : block.getRules().entrySet()) {
            RuleNode node = rule.getValue();

            rows.add(node.isIgnored()
                    ? MappingRow.ignored(node.getProperty())
                    : new MappingRow(node.getProperty(), node.getValue(), node.getCondition(), false));
        }

        return rows;
    }
}
