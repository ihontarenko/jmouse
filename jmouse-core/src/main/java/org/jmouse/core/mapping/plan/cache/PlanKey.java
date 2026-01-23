package org.jmouse.core.mapping.plan.cache;

import org.jmouse.core.mapping.values.ValueKind;

public record PlanKey(ValueKind sourceKind, Class<?> targetType, int policyFingerprint) {}
