package org.jmouse.access.spring.policy;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;

/**
 * Whether this application configured any policy locations.
 *
 * <p>⚠️ <strong>Not {@code @ConditionalOnProperty}.</strong> A list is written four ways — a YAML
 * sequence, an indexed property, a comma-separated value, an environment variable — and the property
 * key {@code jmouse.access.policy.locations} exists in only some of them. A condition that read the
 * key directly would turn the feature off for anybody who wrote their configuration in the shape it
 * missed, and turning authorization off quietly is the failure this whole cluster exists to avoid. So
 * this binds the list the same way the properties bean does, and asks whether it came back with
 * anything.
 */
public class OnPolicyLocationsCondition extends SpringBootCondition {

    private static final String LOCATIONS = AccessPolicyProperties.PREFIX + ".locations";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<String> locations = Binder.get(context.getEnvironment())
                .bind(LOCATIONS, Bindable.listOf(String.class))
                .orElse(List.of());

        if (locations.isEmpty()) {
            return ConditionOutcome.noMatch(
                    "no policy locations are configured under " + LOCATIONS);
        }

        return ConditionOutcome.match(locations.size() + " policy location(s) configured");
    }
}
