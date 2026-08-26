package org.jmouse.mapper.binding;

import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.errors.MappingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Running a set of {@link MappingAssertion}s at one point of a mapping. 🛑
 *
 * <h2>⚠️ Why this is shared rather than a method on the strategy that needed it first</h2>
 *
 * <p>It lived inside the bean strategy, which was correct while there was one strategy with
 * assertions. A second one — a pair converted whole — has the same assertions, at the same phases,
 * reported under the same codes, and copying twelve lines would be copying the one behaviour that
 * must not drift: <strong>every</strong> condition is evaluated rather than short-circuiting at the
 * first hit, so one run reports everything that is wrong instead of one thing at a time. Two copies
 * would agree until somebody optimised one of them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MappingAssertions {

    private MappingAssertions() {
    }

    /**
     * Refuses the mapping when any assertion for this point holds.
     *
     * @param assertions what may refuse
     * @param subject    what is being checked
     * @param phase      when
     * @param value      the object to check
     * @param code       the error code to report under
     * @param context    mapping context, for the path and the policy
     * @throws MappingException when at least one assertion holds
     */
    public static void refuse(
            List<MappingAssertion> assertions,
            MappingAssertion.Subject subject,
            MappingAssertion.Phase phase,
            Object value,
            String code,
            MappingContext context
    ) {
        if (assertions.isEmpty()) {
            return;
        }

        List<String> refusals = null;

        for (MappingAssertion assertion : assertions) {
            if (assertion.applies(subject, phase) && assertion.condition().test(value, context)) {
                refusals = refusals == null ? new ArrayList<>() : refusals;
                refusals.add(assertion.message());
            }
        }

        if (refusals != null) {
            throw new MappingException(code, String.join("; ", refusals))
                    .withPath(context.currentPath());
        }
    }
}
