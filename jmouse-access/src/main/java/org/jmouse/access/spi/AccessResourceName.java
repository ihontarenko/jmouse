package org.jmouse.access.spi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The word a policy writes for this type — stated on the type, once.
 *
 * <pre>
 * &#64;AccessResourceName("form")
 * public class Form { … }
 * </pre>
 *
 * <p>Read back with {@link AccessResourceNames#of(Class)}, and that is the <strong>only</strong> way a
 * resource name is ever produced. There is no fallback and no convention.
 *
 * <h2>⚠️ Why an annotation and not {@code getSimpleName().toLowerCase()}</h2>
 *
 * <p>Because a derived name is a contract nobody can see. Three things were wrong with deriving it:
 *
 * <ul>
 *   <li><strong>It is invisible.</strong> A reader of {@code Form.java} has no way to know the class name
 *       is load-bearing — that a policy file somewhere spells it, and that {@code /access/kinds}
 *       publishes it. A rename is a refactor with a green build and a broken authorization rule.
 *   <li><strong>It is not stable.</strong> {@code CustodyRecord} derives {@code custodyrecord}, which is
 *       neither the word a person would write nor one anybody would guess. The derivation had no way to
 *       say {@code custody_record}, and no way to keep the old word after a class is renamed.
 *   <li><strong>It is not unique by construction.</strong> Two classes called {@code Form} in different
 *       packages derive one word and the collision surfaces as a rule silently aiming at the wrong type.
 *       An explicit name lets the registry refuse the pair at startup.
 * </ul>
 *
 * <p>So the name is written down where the type is, the class may be renamed freely, and a policy that
 * names something no type claims fails the boot with the list of names that would have worked.
 *
 * <h2>What the name is used for</h2>
 *
 * <p>One word, one meaning, in every place the question "which resource" is asked:
 *
 * <ul>
 *   <li>{@code through form} in a permission declaration — the {@link ResourceRelation} destination
 *   <li>{@code GET /access/kinds} and the {@code kind} a {@code may} request names
 *   <li>every startup refusal that has to print which types exist
 * </ul>
 *
 * <p>⚠️ It is <strong>not</strong> a permission namespace. {@code form:read} and {@code through form} are
 * two different vocabularies that happen to share a spelling: the first is a permission, the second is a
 * type. Nothing joins them, and a type needs no permission named after it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AccessResourceName {

    /**
     * The word, lower-case, in the shape a person would write it — {@code form}, {@code custody_record}.
     *
     * <p>⚠️ <strong>Permanent once a policy names it.</strong> Changing it is changing every rule that
     * spells it, in every installation whose policy rows are already written.
     */
    String value();
}
