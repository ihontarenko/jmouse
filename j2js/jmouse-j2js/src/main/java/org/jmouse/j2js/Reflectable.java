package org.jmouse.j2js;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🌐 A class whose members are read by reflection in the browser: a record or a bean an exported method hands the engine, read
 * by its fields, its accessors and its getters.
 *
 * <p>A compiler for the browser keeps no member of a class unless told to — reflection there is declared, not given. This is
 * how a module tells it, naming no compiler: the binding keeps every member of a class so marked, and of no other.</p>
 *
 * <p>A module that hands the engine classes it does not own — a library's records, which must not know of the browser —
 * names their packages instead, one a line, in {@value #PACKAGES_RESOURCE}: every member of every class in each, and in the
 * packages below it, is kept.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reflectable {

    /** Where a module names the packages whose classes are read by reflection: one package a line, {@code #} a comment. */
    String PACKAGES_RESOURCE = "META-INF/j2js/reflectable-packages";
}
