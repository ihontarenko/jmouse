package org.jmouse.datamap.node;

import org.jmouse.datamap.DataMapSource;
import org.jmouse.datamap.DataMapValue;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.Optional;

/** {@code name value [source …]} — one line of a block. */
public class FieldNode extends AbstractExpression {

    private String name;
    private DataMapValue value;
    private DataMapSource source;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataMapValue getValue() {
        return value;
    }

    public void setValue(DataMapValue value) {
        this.value = value;
    }

    public Optional<DataMapSource> getSource() {
        return Optional.ofNullable(source);
    }

    public void setSource(DataMapSource source) {
        this.source = source;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "%s %s".formatted(name, value == null ? "" : value.written());
    }
}
