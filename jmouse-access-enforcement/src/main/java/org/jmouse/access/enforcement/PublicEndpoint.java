package org.jmouse.access.enforcement;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This route is reachable without an account, <em>on purpose</em>.
 *
 * <p>It grants nothing and enforces nothing. Its whole job is to make the decision <strong>visible
 * and deliberate</strong>: {@code AccessDeclarationsTest} fails the build for any route under
 * {@code /api} that carries neither an access declaration nor this. Before it, an endpoint reached a
 * production installation ungated by being written without an annotation, which looks exactly like
 * an endpoint written carefully.
 *
 * <p>Say <em>why</em> beside it. A public route is a decision somebody made, and the next reader has
 * to be able to tell "anyone may fill a shared form" from "nobody has got round to this yet".
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface PublicEndpoint {

    /** Why this is reachable without an account. Required, because the reason is the point. */
    String value();
}
