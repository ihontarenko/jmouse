package org.jmouse.taxonomy.management;

import org.jmouse.access.enforcement.ExternalAccessRules;
import org.jmouse.access.enforcement.ExternalAccessRules.Declaration;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🔐 What the taxonomy routes require, said in a product's own vocabulary — the shape {@code ParallaxAccessRules} has.
 *
 * <pre>{@code
 * @Bean
 * ExternalAccessRules taxonomyAccessRules() {
 *     return TaxonomyAccessRules.builder()
 *             .capability(TaxonomyCapability.CATEGORY_WRITE, "catalogue:write", "GLOBAL")
 *             .capability(TaxonomyCapability.ASSIGNMENT_WRITE, "catalogue:write", "GLOBAL")
 *             .build();
 * }
 * }</pre>
 *
 * <p>⚠️ The type carries the strictest rule, so a route added later and not yet named here is gated as shaping the
 * tree rather than as nothing; a route marked {@link PublicEndpoint} decides for itself; and {@link #build()} refuses
 * to finish while any handler has neither — an ungated route is a failure to boot.</p>
 */
public final class TaxonomyAccessRules {

    /** Every controller this module serves; the boot check walks exactly these. */
    static final List<Class<?>> CONTROLLERS = List.of(TaxonomyController.class);

    private final Map<TaxonomyCapability, Declaration> declarations = new EnumMap<>(TaxonomyCapability.class);

    private TaxonomyAccessRules() {
    }

    public static TaxonomyAccessRules builder() {
        return new TaxonomyAccessRules();
    }

    /**
     * 🔑 What one capability means in this product.
     *
     * @param permission the product's permission for it
     * @param scope      the scope it is decided at, as the product's catalogue spells it
     */
    public TaxonomyAccessRules capability(TaxonomyCapability capability, String permission, String scope) {
        declarations.put(capability, Declaration.permission(permission).atScope(scope));
        return this;
    }

    /**
     * @throws IllegalStateException naming each capability left unmapped, or each handler left with neither a rule nor
     *                               {@link PublicEndpoint}
     */
    public ExternalAccessRules build() {
        List<TaxonomyCapability> missing = Arrays.stream(TaxonomyCapability.values())
                .filter(capability -> !declarations.containsKey(capability))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("The taxonomy routes are gated by " + missing + ", which this product does not "
                    + "map to a permission. Declare each with TaxonomyAccessRules.capability(…).");
        }
        Declaration categoryWrite = declarations.get(TaxonomyCapability.CATEGORY_WRITE);
        Declaration assignmentWrite = declarations.get(TaxonomyCapability.ASSIGNMENT_WRITE);

        ExternalAccessRules declared = ExternalAccessRules.builder()
                .type(TaxonomyController.class, categoryWrite)
                .method(TaxonomyController.class, "createCategory", categoryWrite)
                .method(TaxonomyController.class, "changeCategory", categoryWrite)
                .method(TaxonomyController.class, "moveCategory", categoryWrite)
                .method(TaxonomyController.class, "deleteCategory", categoryWrite)
                .method(TaxonomyController.class, "assign", assignmentWrite)
                .method(TaxonomyController.class, "unassign", assignmentWrite)
                .build();

        ExternalAccessRules rules = new PublicEndpointsHonoured(declared);
        requireEveryHandlerDeclared(rules);
        return rules;
    }

    private static void requireEveryHandlerDeclared(ExternalAccessRules rules) {
        List<String> ungated = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getMethods()) {
                if (isHandler(method) && !isPublic(method, controller) && rules.forMethod(method, controller).isEmpty()) {
                    ungated.add(controller.getSimpleName() + "#" + method.getName());
                }
            }
        }
        if (!ungated.isEmpty()) {
            throw new IllegalStateException("These taxonomy routes have neither an access rule nor @PublicEndpoint: "
                    + ungated + ". Declare each in TaxonomyAccessRules, or mark it public with the reason it is safe.");
        }
    }

    private static boolean isHandler(Method method) {
        return !Modifier.isStatic(method.getModifiers()) && AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    private static boolean isPublic(Method method, Class<?> targetClass) {
        return AnnotatedElementUtils.hasAnnotation(ClassUtils.getMostSpecificMethod(method, targetClass), PublicEndpoint.class);
    }

    /** Declared rules, with a route marked {@link PublicEndpoint} left to decide for itself. */
    private record PublicEndpointsHonoured(ExternalAccessRules declared) implements ExternalAccessRules {

        @Override
        public Optional<Declaration> forMethod(Method method, Class<?> targetClass) {
            if (isPublic(method, targetClass)) {
                return Optional.empty();
            }
            return declared.forMethod(method, targetClass);
        }

        @Override
        public boolean covers(Class<?> targetClass) {
            return declared.covers(targetClass);
        }
    }
}
