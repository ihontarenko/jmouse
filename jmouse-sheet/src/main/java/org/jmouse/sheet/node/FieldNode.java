package org.jmouse.sheet.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.sheet.SheetSource;
import org.jmouse.sheet.SheetValue;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.Optional;

/** {@code name value [source …]} — one line of a block. */
public class FieldNode extends AbstractExpression {

    private String name;
    private SheetValue value;
    private SheetSource source;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SheetValue getValue() {
        return value;
    }

    public void setValue(SheetValue value) {
        this.value = value;
    }

    public Optional<SheetSource> getSource() {
        return Optional.ofNullable(source);
    }

    public void setSource(SheetSource source) {
        this.source = source;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    /** {@code name value [source …]}, as the sheet language reads it — in a sheet document or a host language's. */
    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(name).word(value.written());
        if (source != null) {
            writer.word(source.written());
        }
    }

    @Override
    public String toString() {
        return "%s %s".formatted(name, value == null ? "" : value.written());
    }
}
