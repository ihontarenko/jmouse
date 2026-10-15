package org.jmouse.parallax.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🏷️ This argument is the owner the address names, as one string — {@code TYPE:id}, the form
 * {@link org.jmouse.parallax.owner.ParallaxOwner#parse(String)} reads.
 *
 * <h3>Why one string rather than two path variables</h3>
 *
 * <p>An owner is addressed by two segments, {@code /owners/{ownerType}/{ownerId}}, and an authorization rule names
 * <em>one</em> argument as the row a route is about. Two variables would leave the rule able to see only half of the
 * owner, and a resolver handed {@code 42} cannot tell a workspace from a project. So the two segments reach the method
 * — and the guard around it — joined, and the rule says {@code about(ParallaxOwner.class, "owner")}.</p>
 *
 * <p>⚠️ The argument must be a {@link String}: the access guard reads a row's identifier only from a string.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OwnerAddress {

    /** The path variable holding the owner's type. */
    String type() default ParallaxManagementRoutes.OWNER_TYPE;

    /** The path variable holding the owner's identifier. */
    String id() default ParallaxManagementRoutes.OWNER_ID;
}
