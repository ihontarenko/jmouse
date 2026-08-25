package org.jmouse.query.translate;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.query.el.node.QueryBlockNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a tree — what each node IS, what it stands for, and what hangs under it.
 *
 * <h2>⚠️ It is a view of the tree, not a re-encoding of the language</h2>
 *
 * <p>A node holds most of what it means in <strong>fields</strong> rather than in children — a table
 * name, a column, an operator. Walking only {@link Node#getChildren()} would therefore produce an
 * accurate skeleton of empty shells, and filling those shells in would mean teaching a third walker
 * every node kind the language has. That walker is exactly what {@link JmqTranslator} already is, and
 * two of them drift.</p>
 *
 * <p>So each node contributes two things and no more: <strong>what kind it is</strong>, and
 * <strong>the source it stands for</strong> — its own {@code toSource()}, the same one that writes jMQ
 * and the same one that parses back. Nothing here knows what a {@code where} is.</p>
 *
 * <h2>⚠️ Nothing reads these back, deliberately</h2>
 *
 * <p>The renderings built on this are for <em>looking at</em>. A reader for them would be a second
 * front end for the language — a second thing that decides what a query means — and the one it competes
 * with is the one that runs. If a machine-readable form of a query is ever wanted, the form is jMQ,
 * which already round-trips.</p>
 *
 * @param kind     the node's own class name — {@code WhereNode}, {@code BinaryOperation}
 * @param source   what it stands for, or {@code null} where a node cannot write itself out
 * @param children what hangs under it
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Outline(String kind, String source, List<Outline> children) {

    /**
     * The outline of a node and everything beneath it.
     *
     * @param node where to start
     * @return the outline
     */
    public static Outline of(Node node) {
        // ⚠️ `toSource()` lives on Expression rather than on every Node, and a node without one
        // contributes its kind alone. Inventing text for it would be this walker having an opinion about
        // the language, which is the one thing it must not have.
        String source = node instanceof Expression expression ? expression.toSource() : null;

        return new Outline(node.getClass().getSimpleName(), source, beneath(node));
    }

    /**
     * What hangs under a node.
     *
     * <h2>⚠️ ONE exception to "children only", and it is the difference between a tree and a leaf</h2>
     *
     * <p>A {@link QueryBlockNode} keeps its clauses in a <strong>field</strong> rather than among its
     * children — because a clause is answered about by keyword, and a list that had to be searched for
     * one would be searched at every overlay. Walking {@code getChildren()} alone therefore rendered
     * every query as a single node with the whole text inside it, which is a leaf pretending to be a
     * tree and is worth nothing to look at.</p>
     *
     * <p>⚠️ It is the ONE hop this walker takes into the language, and it stays one on purpose. Asking a
     * clause what is inside it, or a condition what its operands are, is where a viewer turns into a
     * second implementation of the grammar — and the one it would compete with is the one that runs.</p>
     */
    private static List<Outline> beneath(Node node) {
        List<Outline> below = new ArrayList<>();

        if (node instanceof QueryBlockNode block) {
            block.getClauses().forEach(clause -> below.add(of(clause)));
        }

        node.getChildren().forEach(child -> below.add(of(child)));

        return below;
    }
}
