package org.jmouse.script.spi;

import org.jmouse.script.el.host.BoundScript;

import java.util.List;

/**
 * The rules an installation has, already bound and ready to run at one stage — in one tenant, where a
 * product keeps them per tenant.
 *
 * <h2>⚠️ A SEAM, BECAUSE STORAGE IS NOT THE DISPATCH PATH'S BUSINESS</h2>
 *
 * <p>Where a rule lives, when it is compiled, what happens at startup to one that no longer binds, and
 * which stages it has been assigned to are a whole screen's worth of decisions. None of it is knowledge
 * the dispatcher needs; it needs one thing — <em>what runs here, in what order</em>.</p>
 *
 * <p>Which also means dispatch can be built and proved before any of that exists. The default answers
 * nothing, so an installation with no store behaves exactly as it did before rules: every stage is
 * registered, finds nothing to run, and returns.</p>
 *
 * <h2>⚠️ ANSWERING FAST IS PART OF THE CONTRACT</h2>
 *
 * <p>This is asked once per item inside a walk. An implementation that queried a table per call would
 * put a round trip in front of every item in every run. Cache, and invalidate on a write.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ScriptDocuments {

    /**
     * The bound rules assigned to one stage in one tenant, in the order they should run.
     *
     * <h2>⚠️ ORDER IS THE STORE'S AND MUST BE STABLE</h2>
     *
     * <p>Two rules both handling one stage run in this order, so a list that came back in whatever order
     * a database felt like would make an installation's behaviour depend on a query plan — and the day
     * it changed, nobody would connect the two.</p>
     *
     * <h2>⚠️ THE SCOPE COMES FIRST, AND THAT IS NOT ARBITRARY</h2>
     *
     * <p>Two strings in a row is a signature whose arguments can be swapped without the compiler
     * noticing, and the failure that follows is the bad kind: a rule that runs in the wrong tenant and
     * looks entirely healthy. Scope-first matches every other scoped call in this workspace, so the
     * habit is one habit rather than two.
     *
     * <h2>⚠️ AN INSTALLATION-WIDE RULE RUNS IN EVERY SCOPE, AND RUNS FIRST</h2>
     *
     * <p>A stored rule with no scope belongs to the installation and is answered for every tenant,
     * ahead of that tenant's own. Both halves are decisions rather than fallout:</p>
     *
     * <ul>
     *   <li><strong>Every scope</strong>, because that is what a rule with no tenant on it means — and
     *       because it is what every row written before scopes existed means. The alternative reading,
     *       <em>no scope at all</em>, would silently stop every such rule the day the column arrived.</li>
     *   <li><strong>First</strong>, because the installation's rule is the general one and a tenant's is
     *       the particular one. Interleaving the two by sort order would make the answer depend on
     *       numbers chosen in two places that cannot see each other.</li>
     * </ul>
     *
     * @param scope the tenant to answer for, or {@code null} for the installation as a whole
     * @param stage the stage name a script writes after {@code on}
     * @return the rules assigned to it, or an empty list; never {@code null}
     */
    List<BoundScript> forStage(String scope, String stage);

    /**
     * Rules for an installation that has no store yet.
     *
     * @return documents that hold nothing
     */
    static ScriptDocuments none() {
        return (scope, stage) -> List.of();
    }

}
