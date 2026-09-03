package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One {@code .jmm} file: {@code mapping "name" { … }}.
 *
 * <p>⚠️ <strong>A document lists exceptions, not schemas.</strong> Properties named the same on both
 * sides travel by themselves and never appear in a file, so a target needing no exceptions needs no
 * block and a target needing three has three lines however many properties it has. Anything walking
 * this tree and expecting to find every property of a type will find nothing of the kind.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MappingDocumentNode extends ExpressionsNode {

    private final Map<String, UseNode>       imports   = new LinkedHashMap<>();
    private final Map<String, RuleBlockNode> fragments = new LinkedHashMap<>();
    private final List<TargetNode>           targets   = new ArrayList<>();

    private String name;

    /** @return what the file calls itself */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds an import.
     *
     * @param node the import to add
     * @return the import already present under that simple name, or {@code null} when it is the first
     */
    public UseNode add(UseNode node) {
        return imports.putIfAbsent(node.getSimpleName(), node);
    }

    /** @return the imports, keyed by the simple name the file writes */
    public Map<String, UseNode> getImports() {
        return imports;
    }

    /**
     * Adds a fragment.
     *
     * <p>⚠️ Fragments are file-scoped and flat: a fragment does not include another fragment. That
     * keeps a cycle impossible to write and a resolution order unnecessary to define.</p>
     *
     * @param name  the fragment's name
     * @param rules its rules
     * @return the fragment already present under that name, or {@code null} when it is the first
     */
    public RuleBlockNode add(String name, RuleBlockNode rules) {
        return fragments.putIfAbsent(name, rules);
    }

    /** @return the fragments, keyed by name */
    public Map<String, RuleBlockNode> getFragments() {
        return fragments;
    }

    /**
     * Adds a target block.
     *
     * @param target the target to add
     */
    public void add(TargetNode target) {
        targets.add(target);
    }

    /** @return the targets, in the order written */
    public List<TargetNode> getTargets() {
        return targets;
    }

    /**
     * Resolves a type name the file used.
     *
     * @param simpleName the name as written
     * @return the qualified name, or the input unchanged when nothing imported it — a file may spell a
     *         type out in full rather than importing it
     */
    public String resolve(String simpleName) {
        UseNode node = imports.get(simpleName);
        return node == null ? simpleName : node.getQualifiedName();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "mapping \"%s\" [%d targets]".formatted(name, targets.size());
    }
}
