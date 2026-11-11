package org.jmouse.cad.management;

import org.jmouse.access.enforcement.ExternalAccessRules;
import org.jmouse.access.enforcement.ExternalAccessRules.Declaration;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.store.authority.CadCapability;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 🔐 What jmCAD's routes require, said in a product's own vocabulary.
 *
 * <h3>Capabilities in, rules out</h3>
 *
 * <p>The library names what a route asks to be allowed — a {@link CadCapability} — and nothing else. The product says
 * which of its permissions each capability means, and at which of its scopes; this builder turns that into the
 * {@link ExternalAccessRules} that gate every controller here, by class and method, never by path.</p>
 *
 * <pre>{@code
 * @Bean
 * ExternalAccessRules cadAccessRules() {
 *     return CadAccessRules.builder()
 *             .capability(CadCapability.CATALOGUE_WRITE, "catalogue:write", "GLOBAL")
 *             .capability(CadCapability.OWNER_READ, "project:read", "PROJECT")
 *             .capability(CadCapability.OWNER_WRITE, "project:write", "PROJECT")
 *             .build();
 * }
 * }</pre>
 *
 * <h3>⚠️ The type carries the strictest rule, and every route is named</h3>
 *
 * <p>A method rule narrows, it does not replace, so each controller is declared with its most demanding capability and
 * every gated route is then named with its own. A method added later and not yet known here is gated as the strictest
 * thing its controller does rather than as nothing.</p>
 *
 * <h3>⚠️ {@link PublicEndpoint} is honoured, and nothing else is let through</h3>
 *
 * <p>A route marked public decides inside its service, per document. {@link #build()} refuses to finish, naming the
 * method, when any handler of a library controller has neither a rule nor that mark: an ungated route is a failure to
 * boot, never a silent default.</p>
 *
 * <h3>⚠️ What this cannot do for you</h3>
 *
 * <p>The rules name {@link DocumentOwner} (argument {@code owner}, as {@code TYPE:id}). Turning one into the product's
 * place — a workspace, the project in it — is the product's answer, registered as an access-target resolver. Without
 * it the engine has a requirement and no target, and refuses.</p>
 */
public final class CadAccessRules {

    /**
     * Every controller this module serves. The boot check walks exactly these, so a controller added here and not
     * declared in {@link #build()} fails the first start rather than serving ungated.
     */
    static final List<Class<?>> CONTROLLERS = List.of(CatalogueController.class, LibraryController.class,
            SimulationController.class, LanguageController.class);

    /** The capabilities some route here is gated by; each must be mapped before the rules can be built. */
    private static final Set<CadCapability> REQUIRED = EnumSet.of(
            CadCapability.CATALOGUE_WRITE, CadCapability.OWNER_READ, CadCapability.OWNER_WRITE);

    /** The argument an {@link OwnerAddress} reaches a handler as. */
    private static final String OWNER_ARGUMENT = "owner";

    private final Map<CadCapability, Declaration> declarations = new EnumMap<>(CadCapability.class);

    private CadAccessRules() {
    }

    public static CadAccessRules builder() {
        return new CadAccessRules();
    }

    /**
     * 🔑 What one capability means in this product.
     *
     * @param capability what jmCAD asks to be allowed
     * @param permission the product's permission for it
     * @param scope      the scope it is decided at, as the product's catalogue spells it; for a route about an owner the
     *                   owner's resolver decides the place and the scope is only declared
     * @return the builder
     */
    public CadAccessRules capability(CadCapability capability, String permission, String scope) {
        declarations.put(capability, Declaration.permission(permission).atScope(scope));
        return this;
    }

    /**
     * 🔐 The rules.
     *
     * @return rules covering every controller of this module
     * @throws IllegalStateException naming each capability left unmapped, or each handler left with neither a rule nor
     *                               {@link PublicEndpoint}
     */
    public ExternalAccessRules build() {
        requireEveryCapability();

        Declaration catalogueWrite = declarations.get(CadCapability.CATALOGUE_WRITE);
        Declaration ownerRead      = declarations.get(CadCapability.OWNER_READ);
        Declaration ownerWrite     = declarations.get(CadCapability.OWNER_WRITE);

        ExternalAccessRules declared = ExternalAccessRules.builder()
                // ⚠️ The catalogue's writes are the installation's, so the type falls back to writing it.
                .type(CatalogueController.class, catalogueWrite)
                .method(CatalogueController.class, "saveCategory", catalogueWrite)
                .method(CatalogueController.class, "deleteCategory", catalogueWrite)
                .method(CatalogueController.class, "create", catalogueWrite)
                .method(CatalogueController.class, "changeStatus", catalogueWrite)
                .method(CatalogueController.class, "takeShipped", catalogueWrite)
                .method(CatalogueController.class, "owned", aboutOwner(ownerRead))
                .method(CatalogueController.class, "sheets", aboutOwner(ownerRead))
                .method(CatalogueController.class, "createOwned", aboutOwner(ownerWrite))
                // Public through and through: every list and drawing is decided per document inside the service.
                .publicType(LanguageController.class, "the language and texts the caller sent; nothing stored is read")
                .publicType(SimulationController.class, "every run decides inside its service, where the owner it is for is")
                .publicType(LibraryController.class, "the library's documents, each answered only when the reader may see it")
                .build();

        ExternalAccessRules rules = new PublicEndpointsHonoured(declared);

        requireEveryHandlerDeclared(rules);

        return rules;
    }

    private void requireEveryCapability() {
        List<CadCapability> missing = REQUIRED.stream().filter(capability -> !declarations.containsKey(capability)).toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException("jmCAD's routes are gated by " + missing + ", which this product does not map "
                    + "to a permission. Declare each with CadAccessRules.capability(…); a route whose capability means "
                    + "nothing would be refused to everybody or served to anybody.");
        }
    }

    private static Declaration aboutOwner(Declaration declaration) {
        return declaration.about(DocumentOwner.class, OWNER_ARGUMENT);
    }

    /**
     * ⚠️ The check that makes an ungated route a failure to boot: every handler of every controller here has a rule or
     * is marked {@link PublicEndpoint}, or the rules are refused, each offender named.
     */
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
            throw new IllegalStateException("These jmCAD routes have neither an access rule nor @PublicEndpoint: "
                    + ungated + ". Declare each in CadAccessRules, or mark it public with the reason it is safe.");
        }
    }

    /**
     * ⚠️ The check a format beside the API — SPICE, KiCad — makes of its own controller: every handler of it decides
     * inside its service and is marked {@link PublicEndpoint} saying so, since these rules know nothing of it. A handler
     * left unmarked would be served ungated; it is a failure to boot instead, named.
     *
     * @throws IllegalStateException naming each handler without {@link PublicEndpoint}
     */
    public static void requireDecidedInside(Class<?> controller) {
        List<String> unmarked = new ArrayList<>();

        for (Method method : controller.getMethods()) {
            if (isHandler(method) && !isPublic(method, controller)) {
                unmarked.add(controller.getSimpleName() + "#" + method.getName());
            }
        }

        if (!unmarked.isEmpty()) {
            throw new IllegalStateException("These routes beside jmCAD's API are not marked @PublicEndpoint: " + unmarked
                    + ". A format's route decides inside its service, through CadAuthority, and says so.");
        }
    }

    private static boolean isHandler(Method method) {
        return !Modifier.isStatic(method.getModifiers()) && AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    private static boolean isPublic(Method method, Class<?> targetClass) {
        Method declaredMethod = ClassUtils.getMostSpecificMethod(method, targetClass);

        return AnnotatedElementUtils.hasAnnotation(declaredMethod, PublicEndpoint.class);
    }

    /**
     * Declared rules, with a route marked {@link PublicEndpoint} left to decide for itself — the annotation means what
     * it says on a library controller exactly as it does on a product's own.
     */
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
