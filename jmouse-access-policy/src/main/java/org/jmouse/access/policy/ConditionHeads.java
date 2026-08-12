package org.jmouse.access.policy;

import org.jmouse.access.CallerView;
import org.jmouse.access.PlaceView;

import java.util.List;
import java.util.Map;

/**
 * The names a condition may write a dot after, and what may follow each — the half of the rule
 * language that <strong>can</strong> be checked before anybody runs it.
 *
 * <p>A condition sees five things. Two of them are engine-owned values with a fixed shape, so what a
 * rule may say about them is a vocabulary and a typo is answerable at load. The rest are not, and
 * pretending otherwise would be worse than silence:
 *
 * <table border="1">
 *   <caption>What each head is, and whether it can be checked</caption>
 *   <tr><th>Head</th><th>Bound to</th><th>Checked</th></tr>
 *   <tr><td>{@code caller}</td><td>{@link CallerView}</td><td>✅ exhaustively</td></tr>
 *   <tr><td>{@code place}</td><td>{@link PlaceView}</td><td>✅ exhaustively</td></tr>
 *   <tr><td>{@code resource}</td><td>a product's row</td><td>❌ ⚠️ never — polymorphic across every
 *       route, and a checker that guessed would refuse rules that work</td></tr>
 *   <tr><td>{@code action}</td><td>a string</td><td>— compared, never dereferenced</td></tr>
 *   <tr><td>a published value</td><td>whatever was published</td><td>❌ checked as a <em>name</em>
 *       instead, by the pair check</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>An unknown head answers null rather than empty.</strong> The two mean opposite things:
 * empty would refuse every member of it, null says <em>this one is not mine to judge</em>. Getting
 * that backwards would turn the checker on `resource` and break every working rule that reads a row.
 */
public final class ConditionHeads {

    private static final Map<String, List<String>> DECLARED = Map.of(
            "caller", CallerView.MEMBERS,
            "place",  PlaceView.MEMBERS);

    private ConditionHeads() {
    }

    /**
     * What may follow this head, or null where nothing here can say.
     *
     * @param head the bare name before the first dot
     */
    public static List<String> membersOf(String head) {
        return DECLARED.get(head);
    }

    /** Every head with a declared shape — what an editor completes from. */
    public static Map<String, List<String>> declared() {
        return DECLARED;
    }
}
