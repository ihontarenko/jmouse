package org.jmouse.access.enforcement;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What this endpoint needs, declared where it is served.
 *
 * <p>One line replaces a permission expression, a module annotation, a membership check in the
 * service and an ownership check below it. The decision is made once, by the engine, and a refusal
 * names the axis that produced it.
 *
 * <p><strong>Method wins, class is the default — attribute by attribute.</strong> A controller
 * declares its module and its scope once and each method adds only the permission its own verb needs.
 *
 * <h2>Why {@link #scope()} is a string</h2>
 *
 * <p>It was an enum, and reading {@code scope = AccessScope.SPACE} was better than reading
 * {@code scope = "SPACE"} — the compiler checked it. An annotation attribute has to be a constant of
 * a type the annotation declares, though, and a library cannot declare a product's scope enum. So the
 * choice was between a compile-time check for one product and an engine two products can use.
 *
 * <p>What replaces the compiler is not nothing: the name is resolved against the
 * {@link org.jmouse.access.ScopeCatalog} the moment a declaration is read, so a typo fails loudly
 * rather than silently widening a route. Declare the names as constants —
 * {@code @RequiresAccess(scope = Scopes.SPACE)} — and a rename is still one edit.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RequiresAccess {

    /**
     * What the caller must hold.
     *
     * <p>Blank where the endpoint gates on a module alone — which is a real case: a route can be
     * closed because the workspace has the feature switched off while needing no permission of its
     * own beyond being there.
     */
    String permission() default "";

    /**
     * The feature module this endpoint belongs to.
     *
     * <p>Blank where it belongs to none. Named here, the module runs as the product's own axes — a
     * ceiling, an entitlement, a per-place switch — so a place that never could have the feature, one
     * that has not paid for it and one that has switched it off each read a different sentence.
     */
    String module() default "";

    /**
     * How far this endpoint reaches, as the name of a registered scope.
     *
     * <p>Blank means the widest scope — the unscoped route, aimed at nothing in particular. Naming a
     * place means the decision is about that kind of place, and a caller with no grant covering it is
     * refused for that reason rather than for lacking the permission everywhere.
     *
     * <p><strong>Naming the own-rows scope is what a route that <em>creates</em> something says.</strong>
     * There is no row yet to resolve, so the target names the place <em>and the caller as its
     * owner</em> — because that is who will own what is about to exist. Without it a grant over one's
     * own rows could never satisfy a create: the covering chain would have no own-rows link to land
     * on, and everybody would be able to edit their own rows and unable to make one.
     *
     * <p>The scope is deliberately not inferred from whether a place happened to be resolvable. An
     * endpoint whose scope is guessed from the request is an endpoint that stops being scoped the
     * first time a client omits a header.
     */
    String scope() default "";

    /**
     * The kind of row this endpoint acts on, where it acts on one.
     *
     * <p>Naming it is what turns "is this mine" into an ordinary permission question: the feature's
     * {@link org.jmouse.access.spi.AccessTargetResolver} answers where the row lives and who owns it,
     * and the engine reads that as a target. An unknown identifier refuses as "no such row" rather
     * than passing as an unscoped call.
     *
     * <p>A type nothing resolves is a startup failure, not a request-time surprise.
     */
    Class<?> resource() default void.class;

    /**
     * Which parameter carries that row's identifier.
     *
     * <p>Matched against the name the <em>request</em> uses, which is what
     * {@link ParameterNaming} answers. Blank means the method's single identifier parameter, which is
     * what almost every by-identifier route has.
     */
    String resourceId() default "";
}
