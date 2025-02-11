package org.jmouse.testing_ground.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a method that should be called after a bean is constructed,
 * typically for performing custom initialization logic.
 * <p>
 * Example usage:
 * <pre>{@code
 * public class UserService {
 *
 *     @BeanInitializer
 *     public void init() {
 *         // post construct initialization
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanInitializer {

}