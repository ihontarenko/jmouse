package org.jmouse.validator.el.runtime;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.mapper.Mapper;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

import java.util.List;
import java.util.Map;

/**
 * A {@code .jmv} document, compiled once and applied to record after record. 🗂️
 *
 * <h2>⚠️ Compiled once — the thing the current Innoventa code does not do</h2>
 *
 * <p>{@code ValidationService} builds a registry, a schema, a processor and a validator on <em>every
 * submit</em>. Everything expensive here — parsing the file, compiling every guard, message and
 * argument — happens once; a record costs one {@link RecordJudgement} and the constraints its own
 * checks need.</p>
 *
 * <p>⚠️ Which is also why this object is immutable and safe to share. All the state of a run lives in
 * the judgement, so two threads validating two records share the tree and touch nothing of each
 * other's.</p>
 *
 * <h2>⚠️ The three stopping behaviours, and where each one lives</h2>
 *
 * <ul>
 *   <li><strong>Collect everything</strong> — the default, and the shape of this loop.</li>
 *   <li><strong>{@code stop}</strong> — inside {@link CompiledLine}, because it stops one field's list
 *       and a document-level loop could not express "the rest of this line".</li>
 *   <li><strong>{@code gate}</strong> — here, and it is the only early return in the file.</li>
 * </ul>
 *
 * <p>⚠️ None of it is {@code ValidationPolicy}. That enum is {@code FAIL_FAST | COLLECT_ALL} over a
 * whole run and cannot say "this field, no further"; jMV orders its own checks instead of growing the
 * enum a third constant only one caller would understand.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CompiledValidation {

    private final String                 name;
    private final List<CompiledItem>     gate;
    private final List<CompiledItem>     body;
    private final ExpressionLanguage     expressionLanguage;
    private final ConstraintTypeRegistry registry;
    private final Mapper                 mapper;

    public CompiledValidation(
            String name,
            List<CompiledItem> gate,
            List<CompiledItem> body,
            ExpressionLanguage expressionLanguage,
            ConstraintTypeRegistry registry,
            Mapper mapper
    ) {
        this.name = name;
        this.gate = List.copyOf(gate);
        this.body = List.copyOf(body);
        this.expressionLanguage = expressionLanguage;
        this.registry = registry;
        this.mapper = mapper;
    }

    /** @return what the document calls itself */
    public String name() {
        return name;
    }

    /**
     * Judges one record.
     *
     * @param record the values, by field name
     * @return what the document had to say
     */
    public ValidationOutcome validate(Map<String, Object> record) {
        RecordJudgement judgement = new RecordJudgement(record, expressionLanguage, registry, mapper);

        judgement.evaluate(gate);

        // ⚠️ The gate's failure IS the answer. Going on would report faults against a shape nobody
        // claimed the record had — which reads to whoever submitted it as a list of unrelated problems
        // hiding the one that matters.
        if (judgement.hasErrors()) {
            return judgement.outcome(true);
        }

        judgement.evaluate(body);

        return judgement.outcome(false);
    }

    @Override
    public String toString() {
        return "compiled validation \"" + name + "\"";
    }
}
