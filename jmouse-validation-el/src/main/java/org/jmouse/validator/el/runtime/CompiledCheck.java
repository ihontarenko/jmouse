package org.jmouse.validator.el.runtime;

import org.jmouse.el.node.Expression;
import org.jmouse.mapper.Mapper;
import org.jmouse.validator.constraint.adapter.el.ConstraintDefinitionMapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One check, ready to be applied to a value. ✅
 *
 * <h2>⚠️ The constraint is built per record, not once</h2>
 *
 * <p>Because an argument is an expression: {@code min(other_field)} and
 * {@code max(quantity - reserved)} are things somebody will write, and a constraint carrying a number
 * frozen at compile time would be answering about a record it never saw. The <em>expressions</em> are
 * compiled once — which is the expensive half — and only the binding happens per record.</p>
 *
 * <p>⚠️ It is assembled through {@link ValidationDefinition} rather than by composing
 * {@code "@Size('min':3)"} and parsing it back. Writing a string for a parser this same object could
 * have skipped is a round trip through a syntax, and every escaping rule in it becomes a way to be
 * subtly wrong — which is exactly the failure {@code ValidationCheckCatalogue} refuses quotes to
 * avoid.</p>
 *
 * @param signature what the written word means
 * @param arguments the arguments by property name, each still an expression
 * @param message   what to say when it fails, or {@code null} to take the line's
 * @param stop      whether a failure here silences the rest of this field's checks
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record CompiledCheck(
        CheckSignature            signature,
        Map<String, List<Expression>> arguments,
        Expression                message,
        boolean                   stop
) {

    public CompiledCheck {
        arguments = Map.copyOf(arguments);
    }

    /**
     * Builds the constraint this check describes, for one record.
     *
     * @param registry where constraint names are resolved
     * @param mapper   what binds the definition onto the constraint bean
     * @param record   the record being judged, for evaluating arguments
     * @return the constraint
     */
    public Constraint constraint(ConstraintTypeRegistry registry, Mapper mapper, RecordJudgement record) {
        ValidationDefinition definition = new ValidationDefinition(signature.constraint());

        definition.putAll(signature.fixed());

        arguments.forEach((key, expressions) -> definition.put(key, bind(expressions, record)));

        Class<? extends Constraint> type = registry.resolve(signature.constraint()).orElseThrow(
                () -> new IllegalStateException(
                        "'%s' builds the constraint '%s', which is registered nowhere"
                                .formatted(signature.check(), signature.constraint())));

        return new ConstraintDefinitionMapper(mapper).map(definition, type);
    }

    /**
     * One property's value: a single argument, or the list a collecting check gathers.
     *
     * @param expressions the argument expressions under that property
     * @param record      the record being judged
     * @return what to bind
     */
    private Object bind(List<Expression> expressions, RecordJudgement record) {
        if (!signature.variadic()) {
            return expressions.isEmpty() ? null : record.value(expressions.getFirst());
        }

        List<Object> values = new ArrayList<>(expressions.size());

        for (Expression expression : expressions) {
            values.add(record.value(expression));
        }

        return values;
    }
}
