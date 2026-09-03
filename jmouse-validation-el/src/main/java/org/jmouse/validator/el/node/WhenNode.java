package org.jmouse.validator.el.node;

import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that apply only while a condition holds — {@code when mount_type == 'SMD' { … } otherwise { … }}.
 *
 * <h2>⚠️ Nesting is conjunction, so a file may be flat or deep</h2>
 *
 * <p>A {@code when} inside a {@code when} carries both guards, which makes these two the same document:</p>
 *
 * <pre>{@code
 * when a { when b { field : required } }
 * when a and b { field : required }
 * }</pre>
 *
 * <p>Both are legal on purpose. Depth reads better when several fields share the outer condition and
 * only some share the inner one; flat reads better when there is one field. Neither is the canonical
 * form, and a reader that rewrote one into the other would be arguing with whoever wrote the file.</p>
 *
 * <h2>⚠️ {@code otherwise} binds to its own {@code when}</h2>
 *
 * <p>Never to an outer one. Both branches are brace blocks, so the dangling-else ambiguity cannot be
 * written down — avoided by construction rather than settled by a precedence rule nobody remembers.</p>
 *
 * <h2>⚠️ A field under a false guard is not checked, and that is not the same as passing</h2>
 *
 * <p>A guard is about <em>applicability</em>: under a false one, the field is simply not part of this
 * record's shape. Anything that must hold whatever the branch belongs in {@code always}, and whatever
 * reports results has to keep the two apart — otherwise a caller cannot tell "fine" from "not asked".</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class WhenNode extends ExpressionsNode {

    private final List<WhenBranchNode> branches = new ArrayList<>();

    /**
     * Adds a branch, in the order it was written.
     *
     * @param branch the guarded branch, or the {@code otherwise}
     */
    public void addBranch(WhenBranchNode branch) {
        branches.add(branch);
    }

    /** @return the branches, guarded one first; never {@code null} */
    public List<WhenBranchNode> getBranches() {
        return List.copyOf(branches);
    }

    /**
     * Whether an {@code otherwise} was written at all.
     *
     * <p>⚠️ Distinct from its being empty. {@code otherwise { }} says somebody considered the other
     * case and decided nothing applies; no branch at all says nobody considered it. The two look
     * identical in a list of items and are not the same statement.</p>
     *
     * @return whether the file carried an unguarded branch
     */
    public boolean hasOtherwise() {
        return branches.stream().anyMatch(branch -> !branch.isGuarded());
    }

    @Override
    public String toString() {
        return "when … " + branches.size() + " branch(es)";
    }
}
