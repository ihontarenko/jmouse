package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.writer.SourceWriter;

import java.util.List;
import java.util.Optional;

/**
 * Names given values between commas — {@code amplitude = A, frequency = F, bytes = "8 0"} —, or a name standing alone,
 * {@code closed}: what a part is set to.
 */
public class SettingsNode extends AbstractExpression {

    /** A name and its value — an expression or a text —, or none for a name that stands alone. */
    public record Setting(String name, Expression value) {

        public Optional<Expression> given() {
            return Optional.ofNullable(value);
        }
    }

    private final List<Setting> settings;

    public SettingsNode(List<Setting> settings) {
        this.settings = List.copyOf(settings);
    }

    public List<Setting> settings() {
        return settings;
    }

    public Optional<Expression> value(String name) {
        return settings.stream().filter(setting -> setting.name().equals(name)).findFirst().flatMap(Setting::given);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return settings;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        for (int index = 0; index < settings.size(); index++) {
            Setting setting = settings.get(index);
            if (index > 0) {
                writer.join(",");
            }
            writer.word(setting.name());
            if (setting.value() != null) {
                writer.word("=").write(setting.value());
            }
        }
    }
}
