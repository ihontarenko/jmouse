package org.jmouse.parallax.store.spice;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;

/**
 * Matches when {@code jmouse.parallax.spice.command} names a program — written as a list or as one comma-separated
 * line, as the properties bind it; an empty value means no second engine.
 */
public class SpiceConfigured extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<String> command = Binder.get(context.getEnvironment())
                .bind("jmouse.parallax.spice.command", Bindable.listOf(String.class)).orElse(List.of());
        boolean configured = command.stream().anyMatch(word -> !word.isBlank());
        return configured ? ConditionOutcome.match("a SPICE command is configured")
                : ConditionOutcome.noMatch("no SPICE command is configured");
    }
}
