package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.el.node.Expression;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.ScriptNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The behaviour a part runs, as one jMS script: each behaviour of its lineage read by jMS — the model keeps them as
 * written —, then folded the most general first. A member's handlers run after its base's, every one of them heard; a
 * function it writes again replaces the base's of that name, so the base's handlers call the member's — the way a
 * protocol written once in a base hands each byte to what the part does with it. The fold is on jMS's trees; no text is
 * put together.
 */
final class BehaviourScripts {

    /** What a behaviour block is called, as the script it becomes is named. */
    private static final String BEHAVIOUR = "behaviour";

    /** jMS as the documents it was written in read it. */
    private static final DocumentLanguage LANGUAGE = DocumentLanguage.of(new SimulationModule());

    private BehaviourScripts() {
    }

    /** The part's behaviour folded into one script, or null for a part whose lineage writes none. */
    static ScriptNode of(SimulatedComponent component) {
        List<String> behaviours = component.behaviours();
        ScriptNode merged = null;
        for (String behaviour : behaviours) {
            ScriptNode read = LANGUAGE.script(BEHAVIOUR, behaviour, component.title());
            merged = merged == null ? read : over(read, merged);
        }
        return merged;
    }

    /** A member's script over its base's: the base's statements but the functions it writes again, then its own. */
    private static ScriptNode over(ScriptNode member, ScriptNode base) {
        Set<String> rewritten = member.getFunctions().stream().map(FunctionDeclarationNode::getName).collect(Collectors.toSet());
        ScriptNode merged = new ScriptNode();
        merged.setName(member.getName());
        for (Expression expression : base.getExpressions()) {
            boolean replaced = expression instanceof FunctionDeclarationNode function && rewritten.contains(function.getName());
            if (!replaced) {
                merged.addExpression(expression);
            }
        }
        member.getExpressions().forEach(merged::addExpression);
        return merged;
    }
}
