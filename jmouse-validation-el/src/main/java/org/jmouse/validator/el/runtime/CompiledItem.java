package org.jmouse.validator.el.runtime;

/**
 * Something a compiled document evaluates against a record.
 *
 * <p>Sealed, because the three shapes are the language's and a fourth would be a grammar change rather
 * than an extension — a reader switching over them is entitled to know the list is complete.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public sealed interface CompiledItem permits CompiledLine, CompiledGuard, CompiledInvariant {

    /**
     * Judges the record, adding whatever it finds.
     *
     * @param record what is being validated, and where errors are collected
     */
    void evaluate(RecordJudgement record);
}
