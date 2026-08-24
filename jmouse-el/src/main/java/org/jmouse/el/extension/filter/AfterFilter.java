package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * Everything after the last occurrence of a separator — {@code "3300|mΩ" | after("|")} is {@code mΩ}.
 *
 * <p>The other half of {@link BeforeFilter}, and literal for the same reason: a data source's
 * {@code SUBSTRING_INDEX(x, sep, -1)} / {@code split_part(x, sep, -1)} take a literal delimiter, so a
 * regex-taking filter could not be honoured by one.</p>
 *
 * <p>⚠️ <strong>The LAST occurrence, not the second field.</strong> {@code "a|b|c" | after("|")} is
 * {@code c}. That is what both databases do with {@code -1}, and picking the same answer is the whole
 * point — a filter that meant "everything after the first" would need a different SQL shape on each and
 * would eventually be given a different one.</p>
 *
 * <p>A value with no separator comes back whole, matching {@link BeforeFilter} and both databases.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AfterFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (!(input instanceof String string) || arguments.isEmpty()) {
            return input;
        }

        String separator = context.getConversion().convert(arguments.getFirst(), String.class);

        if (separator == null || separator.isEmpty()) {
            return string;
        }

        int at = string.lastIndexOf(separator);

        return at < 0 ? string : string.substring(at + separator.length());
    }

    @Override
    public String getName() {
        return "after";
    }
}
