package org.jmouse.script.el.function;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@code append(list, value, …)} — a new list: the given one with the values added at its end. The list itself is
 * not changed, so {@code xs = append(xs, x)} is how a program accumulates a series.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AppendFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        if (arguments.size() < 1) {
            throw new IllegalArgumentException("append(list, value, …) takes a list first");
        }
        List<Object> values = new ArrayList<>(elementsOf(arguments.get(0)));
        for (int index = 1; index < arguments.size(); index++) {
            values.add(arguments.get(index));
        }
        return values.toArray();
    }

    @Override
    public String getName() {
        return "append";
    }

    private static List<Object> elementsOf(Object list) {
        if (list == null) {
            return List.of();
        }
        if (list.getClass().isArray()) {
            List<Object> elements = new ArrayList<>();
            for (int index = 0; index < Array.getLength(list); index++) {
                elements.add(Array.get(list, index));
            }
            return elements;
        }
        if (list instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        throw new IllegalArgumentException("append(list, value, …) takes a list first, not " + list.getClass().getSimpleName());
    }
}
