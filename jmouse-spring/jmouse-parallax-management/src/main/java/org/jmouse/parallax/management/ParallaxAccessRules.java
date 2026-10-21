package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.ExternalAccessRules;
import org.jmouse.access.enforcement.ExternalAccessRules.Declaration;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.jpa.scene.Comparison;
import org.jmouse.parallax.jpa.scene.Scene;
import org.jmouse.parallax.owner.ParallaxOwner;
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
 * 🔐 What Parallax's routes require, said in a product's own vocabulary.
 *
 * <h3>Capabilities in, rules out</h3>
 *
 * <p>The library names what a route asks to be allowed — a {@link ParallaxCapability} — and nothing else. The product
 * says which of its permissions each capability means, and at which of its scopes; this builder turns that into the
 * {@link ExternalAccessRules} that gate every controller here, by class and method, never by path.</p>
 *
 * <pre>{@code
 * @Bean
 * ExternalAccessRules parallaxAccessRules() {
 *     return ParallaxAccessRules.builder()
 *             .capability(ParallaxCapability.CATALOGUE_WRITE, "catalogue:write", "GLOBAL")
 *             .capability(ParallaxCapability.SCENE_READ, "scene:read", "WORKSPACE")
 *             …
 *             .build();
 * }
 * }</pre>
 *
 * <h3>⚠️ The type carries the strictest rule, and every route is named</h3>
 *
 * <p>A method rule narrows, it does not replace, so each controller is declared with its most demanding capability and
 * every gated route is then named with its own. A method added to a controller in a later release, and not yet known
 * here, is gated as the strictest thing that controller does rather than as nothing.</p>
 *
 * <h3>⚠️ {@link PublicEndpoint} is honoured, and nothing else is let through</h3>
 *
 * <p>A route marked public decides inside its service — per object, per job, per collection — and a type rule laid
 * over it would refuse the anonymous reader an open installation lets in. So a public route gets no rule. And
 * {@link #build()} refuses to finish, naming the method, when any handler of a library controller has neither a rule
 * nor that mark: an ungated route is a failure to boot, never a silent default.</p>
 *
 * <h3>⚠️ What this cannot do for you</h3>
 *
 * <p><strong>Resolving a row to a place.</strong> The rules name {@link ParallaxOwner} (argument {@code owner}, as
 * {@code TYPE:id}), {@link Scene} ({@code sceneId}) and {@link Comparison} ({@code comparisonId}). Turning one of those
 * into the product's place — a workspace, the project in it — is the product's answer, registered as an access-target
 * resolver for each. Without them the engine has a requirement and no target, and refuses.</p>
 */
public final class ParallaxAccessRules {

    /**
     * Every controller this module serves. The boot check walks exactly these, so a controller added here and not
     * declared in {@link #build()} fails the first start rather than serving ungated.
     */
    static final List<Class<?>> CONTROLLERS = List.of(
            CatalogueController.class, ComputationController.class, DocumentController.class, SpiceController.class,
            FormulaController.class, SubjectController.class, SceneController.class, CollectionController.class,
            ParallaxAdministrationController.class, KiCadController.class, PartSearchController.class);

    /** The capabilities some route here is gated by; each must be mapped before the rules can be built. */
    private static final Set<ParallaxCapability> REQUIRED = EnumSet.of(
            ParallaxCapability.CATALOGUE_WRITE, ParallaxCapability.CATALOGUE_PUBLISH,
            ParallaxCapability.SCENE_READ, ParallaxCapability.SCENE_WRITE, ParallaxCapability.SCENE_DELETE,
            ParallaxCapability.JOB_READ, ParallaxCapability.JOB_ADMINISTER, ParallaxCapability.COLLECTION_CREATE,
            ParallaxCapability.EVALUATION_READ, ParallaxCapability.EVALUATION_RUN,
            ParallaxCapability.MANAGEMENT_READ, ParallaxCapability.MANAGEMENT_WRITE);

    /** The argument an {@link OwnerAddress} reaches a handler as. */
    private static final String OWNER_ARGUMENT = "owner";

    private final Map<ParallaxCapability, Declaration> declarations = new EnumMap<>(ParallaxCapability.class);

    private ParallaxAccessRules() {
    }

    /**
     * 🏗️ Start declaring.
     *
     * @return the builder
     */
    public static ParallaxAccessRules builder() {
        return new ParallaxAccessRules();
    }

    /**
     * 🔑 What one capability means in this product.
     *
     * @param capability what Parallax asks to be allowed
     * @param permission the product's permission for it
     * @param scope      the scope it is decided at, as the product's catalogue spells it; for a route about an owner, a
     *                   scene or a comparison the row's resolver decides the place and the scope is only declared
     * @return the builder
     */
    public ParallaxAccessRules capability(ParallaxCapability capability, String permission, String scope) {
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

        Declaration catalogueWrite   = declared(ParallaxCapability.CATALOGUE_WRITE);
        Declaration cataloguePublish = declared(ParallaxCapability.CATALOGUE_PUBLISH);
        Declaration sceneRead        = declared(ParallaxCapability.SCENE_READ);
        Declaration sceneWrite       = declared(ParallaxCapability.SCENE_WRITE);
        Declaration sceneDelete      = declared(ParallaxCapability.SCENE_DELETE);
        Declaration jobRead          = declared(ParallaxCapability.JOB_READ);
        Declaration jobAdminister    = declared(ParallaxCapability.JOB_ADMINISTER);
        Declaration collectionCreate = declared(ParallaxCapability.COLLECTION_CREATE);
        Declaration evaluationRead   = declared(ParallaxCapability.EVALUATION_READ);
        Declaration evaluationRun    = declared(ParallaxCapability.EVALUATION_RUN);
        Declaration managementRead   = declared(ParallaxCapability.MANAGEMENT_READ);
        Declaration managementWrite  = declared(ParallaxCapability.MANAGEMENT_WRITE);

        ExternalAccessRules declared = ExternalAccessRules.builder()
                // ⚠️ The catalogue's writes are the installation's, so the type falls back to writing it.
                .type(CatalogueController.class, catalogueWrite)
                .method(CatalogueController.class, "saveCategory", catalogueWrite)
                .method(CatalogueController.class, "deleteCategory", catalogueWrite)
                .method(CatalogueController.class, "create", catalogueWrite)
                .method(CatalogueController.class, "saveInstrument", catalogueWrite)
                .method(CatalogueController.class, "changeStatus", cataloguePublish)
                .method(CatalogueController.class, "ownedObjects", aboutOwner(sceneRead))
                .method(CatalogueController.class, "ownedSheets", aboutOwner(sceneRead))
                .method(CatalogueController.class, "createOwned", aboutOwner(sceneWrite))
                // A KiCad design imported is circuits and symbols written into the owner's place, as createOwned writes one.
                .method(KiCadController.class, "importDesign", aboutOwner(sceneWrite))
                // A part fetched is a footprint and a component written into the owner's place, as an import writes them.
                .method(PartSearchController.class, "fetch", aboutOwner(sceneWrite))

                // Every other computation route decides per object and per job, inside its service.
                .type(ComputationController.class, jobAdminister)
                .method(ComputationController.class, "ownedJobs", aboutOwner(jobRead))

                // ⚠️ The stricter rule on the type: an unnamed scene route is gated as deleting that scene.
                .type(SceneController.class, aboutScene(sceneDelete))
                .method(SceneController.class, "ownedScenes", aboutOwner(sceneRead))
                .method(SceneController.class, "create", aboutOwner(sceneWrite))
                .method(SceneController.class, "scene", aboutScene(sceneRead))
                .method(SceneController.class, "revisions", aboutScene(sceneRead))
                .method(SceneController.class, "revision", aboutScene(sceneRead))
                .method(SceneController.class, "snapshots", aboutScene(sceneRead))
                // A fork reads its source here; writing at the target is checked by the service.
                .method(SceneController.class, "fork", aboutScene(sceneRead))
                .method(SceneController.class, "save", aboutScene(sceneWrite))
                .method(SceneController.class, "describe", aboutScene(sceneWrite))
                .method(SceneController.class, "restore", aboutScene(sceneWrite))
                .method(SceneController.class, "snapshot", aboutScene(sceneWrite))
                .method(SceneController.class, "deleteSnapshot", aboutScene(sceneWrite))
                .method(SceneController.class, "archive", aboutScene(sceneDelete))
                .method(SceneController.class, "delete", aboutScene(sceneDelete))
                .method(SceneController.class, "ownedComparisons", aboutOwner(sceneRead))
                .method(SceneController.class, "createComparison", aboutOwner(sceneWrite))
                .method(SceneController.class, "comparison", aboutComparison(sceneRead))
                .method(SceneController.class, "updateComparison", aboutComparison(sceneWrite))
                .method(SceneController.class, "deleteComparison", aboutComparison(sceneDelete))

                .type(CollectionController.class, collectionCreate)
                .method(CollectionController.class, "create", collectionCreate)

                // ⚠️ The machinery spans every owner, so everything here is the installation's to administer.
                .type(ParallaxAdministrationController.class, managementWrite)
                .method(ParallaxAdministrationController.class, "allJobs", jobAdminister)
                .method(ParallaxAdministrationController.class, "cache", managementRead)
                .method(ParallaxAdministrationController.class, "activity", managementRead)
                .method(ParallaxAdministrationController.class, "warmupStatus", managementRead)
                .method(ParallaxAdministrationController.class, "statistics", managementRead)
                .method(ParallaxAdministrationController.class, "startWarmup", managementWrite)
                .method(ParallaxAdministrationController.class, "checks", evaluationRead)
                .method(ParallaxAdministrationController.class, "runs", evaluationRead)
                .method(ParallaxAdministrationController.class, "report", evaluationRead)
                .method(ParallaxAdministrationController.class, "run", evaluationRun)

                // Public through and through: every route reads a text the caller sent or the language itself.
                .publicType(DocumentController.class, "the jMX language and texts the caller sent; nothing stored is read")
                .publicType(SpiceController.class, "texts the caller sent, converted; components are read as the reader may see them")
                .publicType(FormulaController.class, "the jMF language and formulas the caller sent; nothing stored is read")
                .publicType(SubjectController.class, "a stored subject is read through the catalogue, which decides")
                .build();

        ExternalAccessRules rules = new PublicEndpointsHonoured(declared);

        requireEveryHandlerDeclared(rules);

        return rules;
    }

    private void requireEveryCapability() {
        List<ParallaxCapability> missing = REQUIRED.stream()
                .filter(capability -> !declarations.containsKey(capability))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Parallax's routes are gated by " + missing + ", which this product does not "
                    + "map to a permission. Declare each with ParallaxAccessRules.capability(…); a route whose "
                    + "capability means nothing would be refused to everybody or served to anybody.");
        }
    }

    private Declaration declared(ParallaxCapability capability) {
        return declarations.get(capability);
    }

    private static Declaration aboutOwner(Declaration declaration) {
        return declaration.about(ParallaxOwner.class, OWNER_ARGUMENT);
    }

    private static Declaration aboutScene(Declaration declaration) {
        return declaration.about(Scene.class, "sceneId");
    }

    private static Declaration aboutComparison(Declaration declaration) {
        return declaration.about(Comparison.class, "comparisonId");
    }

    /**
     * ⚠️ The check that makes an ungated route a failure to boot: every handler of every controller here has a rule or
     * is marked {@link PublicEndpoint}, or the rules are refused, each offender named.
     */
    private static void requireEveryHandlerDeclared(ExternalAccessRules rules) {
        List<String> ungated = new ArrayList<>();

        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getMethods()) {
                if (isHandler(method) && !isPublic(method, controller)
                        && rules.forMethod(method, controller).isEmpty()) {
                    ungated.add(controller.getSimpleName() + "#" + method.getName());
                }
            }
        }

        if (!ungated.isEmpty()) {
            throw new IllegalStateException("These Parallax routes have neither an access rule nor @PublicEndpoint: "
                    + ungated + ". Declare each in ParallaxAccessRules, or mark it public with the reason it is safe.");
        }
    }

    private static boolean isHandler(Method method) {
        return !Modifier.isStatic(method.getModifiers())
                && AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
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
