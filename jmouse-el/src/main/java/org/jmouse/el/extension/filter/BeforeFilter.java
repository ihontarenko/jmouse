package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * Everything up to the first occurrence of a separator — {@code "3300|mΩ" | before("|")} is {@code 3300}.
 *
 * <h2>⚠️ Why this exists when {@code split} already does</h2>
 *
 * <p>{@link SplitFilter} calls {@code String.split(regex)}, and its argument is a <strong>regular
 * expression</strong>. That is correct and useful in memory, and it is untranslatable: a data source's
 * {@code SUBSTRING_INDEX} / {@code split_part} take a <strong>literal</strong> delimiter, so the two
 * disagree in opposite directions and neither complains.</p>
 *
 * <table>
 *   <caption>The divergence this filter avoids</caption>
 *   <tr><th>Written</th><th>In memory</th><th>In SQL</th></tr>
 *   <tr><td>{@code split("|")}</td><td>{@code |} is alternation — splits on every character</td>
 *       <td>splits on {@code |}</td></tr>
 *   <tr><td>{@code split("\\|")}</td><td>splits on {@code |}</td>
 *       <td>looks for the two characters {@code \|} — never matches</td></tr>
 * </table>
 *
 * <p>So this takes a <strong>literal</strong> separator, by definition, and the two worlds agree by
 * construction rather than by care.</p>
 *
 * <p>⚠️ A value with no separator in it comes back whole — the same answer both databases give, and the
 * useful one: a reading stored as {@code "240"} rather than {@code "240|pcs"} still reads as 240.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BeforeFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (!(input instanceof String string) || arguments.isEmpty()) {
            return input;
        }

        String separator = context.getConversion().convert(arguments.getFirst(), String.class);

        if (separator == null || separator.isEmpty()) {
            return string;
        }

        int at = string.indexOf(separator);

        return at < 0 ? string : string.substring(0, at);
    }

    @Override
    public String getName() {
        return "before";
    }
}
