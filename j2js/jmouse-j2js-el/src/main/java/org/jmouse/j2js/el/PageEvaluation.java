package org.jmouse.j2js.el;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.access.accessor.ArrayAccessorProvider;
import org.jmouse.core.access.accessor.CollectionAccessorProvider;
import org.jmouse.core.access.accessor.MapAccessorProvider;
import org.jmouse.core.access.accessor.ScalarValueAccessorProvider;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.BasicValuesChain;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.ExpressionLanguageConversion;

import java.util.List;
import java.util.Map;

/**
 * 🌐 jME as a page runs it: one language, and each expression evaluated over the values the page gives — as JSON gives them,
 * maps, lists, texts, numbers and truth values. Those are reached by their own accessors, never through reflection: a page
 * hands no Java objects, and the browser has no reflection to read one with.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class PageEvaluation {

    /** The language, built once: its extensions and its parsed expressions are kept for every evaluation. */
    private static final ExpressionLanguage LANGUAGE = new ExpressionLanguage();

    /** What an expression may read of a value: what JSON gives, and nothing a bean or a record would need reflection for. */
    private static final AccessorWrapper PAGE_VALUES = new ObjectAccessorWrapper(List.of(
            new ScalarValueAccessorProvider(),
            new ArrayAccessorProvider(),
            new CollectionAccessorProvider(),
            new MapAccessorProvider()
    ));

    /**
     * How values are converted, built once: its converters form a graph that costs more to build than an evaluation costs to
     * run, and nothing an evaluation does changes it.
     */
    private static final Conversion CONVERSION = new ExpressionLanguageConversion();

    private PageEvaluation() {
    }

    /** The value of {@code expression}, each of {@code values} known to it by its name. */
    static Object evaluate(String expression, Map<String, Object> values) {
        DefaultEvaluationContext context = new DefaultEvaluationContext(
                new BasicValuesChain(), LANGUAGE.getExtensions(), CONVERSION, PAGE_VALUES);

        values.forEach(context::setValue);

        return LANGUAGE.compile(expression).evaluate(context);
    }
}
