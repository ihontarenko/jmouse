package org.jmouse.mapper.el.translate;

import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Capability;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;
import org.jmouse.mapper.el.node.TargetNode;

/**
 * Walks a mapping document and asks the destination whether it can honour each construct it finds.
 *
 * <h2>⚠️ One walk, so two destinations cannot disagree about what a document contains</h2>
 *
 * <p>{@link JmmSourceTranslator} and {@link JmmJsonTranslator} render nothing alike, and if each did
 * its own capability checking on the way through, the first thing to go wrong is the construct one of
 * them forgets — {@code when} being the obvious candidate, because a rule with a guard and a rule
 * without look identical until you read the field. Then a destination declares it cannot do guards,
 * is handed a document full of them, and cheerfully writes one out anyway.</p>
 *
 * <p>So the check is separate from the rendering and shared by both. What each destination does with
 * a document is its own business; <em>what is in the document</em> is not a question two of them
 * should be answering separately.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class JmmCapabilityCheck {

    private final Capabilities capabilities;

    JmmCapabilityCheck(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Refuses the document unless the destination declared everything in it.
     *
     * @param document the parsed file
     */
    void check(MappingDocumentNode document) {
        if (!document.getImports().isEmpty()) {
            require(JmmCapability.USE);
        }

        if (!document.getFragments().isEmpty()) {
            require(JmmCapability.FRAGMENT);
        }

        for (RuleBlockNode fragment : document.getFragments().values()) {
            check(fragment);
        }

        for (TargetNode target : document.getTargets()) {
            check(target);
        }
    }

    /**
     * Checks one {@code target} block and everything under it.
     *
     * @param target the block
     */
    private void check(TargetNode target) {
        require(JmmCapability.TARGET);

        if (target.getUnmapped() == TargetNode.Unmapped.FAIL) {
            require(JmmCapability.UNMAPPED);
        }

        if (target.getAlways() != null && !target.getAlways().isEmpty()) {
            require(JmmCapability.ALWAYS);
            check(target.getAlways());
        }

        if (!target.getRefusals().isEmpty()) {
            require(JmmCapability.REFUSE);
        }

        for (FromNode source : target.getSources()) {
            check(source);
        }
    }

    /**
     * Checks one {@code from} block.
     *
     * <p>⚠️ A converted pair is its own capability rather than a flavour of {@code from}. It parses
     * today and the engine refuses it (JMF-193), which makes it exactly the construct a destination is
     * most likely to drop on the floor — and a round trip that quietly loses {@code : via(…)} produces
     * a file that maps property by property instead, which is a different mapping rather than a
     * formatting difference.</p>
     *
     * @param source the block
     */
    private void check(FromNode source) {
        require(JmmCapability.FROM);

        if (source.isConverted()) {
            require(JmmCapability.VIA);
        }

        if (source.getRefusal() != null) {
            require(JmmCapability.REFUSE);
        }

        if (source.getRules() != null) {
            check(source.getRules());
        }
    }

    /**
     * Checks a block of rules — the same shape whether it came from {@code always}, a {@code from} or
     * a {@code fragment}.
     *
     * @param block the rules
     */
    private void check(RuleBlockNode block) {
        if (!block.getIncludes().isEmpty()) {
            require(JmmCapability.INCLUDE);
        }

        if (!block.getBindings().isEmpty()) {
            require(JmmCapability.LET);
        }

        for (RuleNode rule : block.getRules().values()) {
            require(rule.isIgnored() ? JmmCapability.IGNORE : JmmCapability.RULE);

            if (rule.getCondition() != null) {
                require(JmmCapability.WHEN);
            }
        }
    }

    /**
     * Asks the destination for one capability, naming it the way a file writes it.
     *
     * @param capability what the document asked for
     */
    private void require(Capability capability) {
        capabilities.require(capability, JmmCapability.keyword(capability));
    }
}
