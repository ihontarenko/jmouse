package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

import java.util.List;
import java.util.Optional;

/**
 * What a part is connected to: {@code (in, out)} — its pins in order —, or {@code (positive = vcc, negative = 0)} — each
 * by its name, a unit's pin with its unit, {@code A.output = out}.
 */
public class ConnectionsNode extends AbstractExpression {

    /** One connection: the pin it names, or none when it goes by order, and the net. */
    public record Connection(String pin, String net) {

        public Optional<String> named() {
            return Optional.ofNullable(pin);
        }
    }

    private final List<Connection> connections;

    public ConnectionsNode(List<Connection> connections) {
        this.connections = List.copyOf(connections);
    }

    public List<Connection> connections() {
        return connections;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return connections;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word("(");
        for (int index = 0; index < connections.size(); index++) {
            Connection connection = connections.get(index);
            if (index == 0) {
                writer.join(connection.pin() == null ? connection.net() : connection.pin());
            } else {
                writer.join(",").word(connection.pin() == null ? connection.net() : connection.pin());
            }
            if (connection.pin() != null) {
                writer.word("=").word(connection.net());
            }
        }
        writer.join(")");
    }
}
