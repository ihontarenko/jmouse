package org.jmouse.access.enforcement;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * One named value a call publishes, so a condition can read it.
 *
 * <p>Exactly one of {@link #from()} and {@link #is()} is written, and which one says where the value
 * comes from: an argument the caller passed, or a constant this route always carries.
 *
 * <h2>Why two members rather than one array of pairs</h2>
 *
 * <p>Because {@code {"keyA", "valueA", "keyB"}} compiles. An odd number of elements is a runtime
 * failure on a route nobody exercised, and brace-magic is unreadable besides. Two members are two
 * things a compiler can see and a reader can name.
 *
 * <h2>⚠️ Required by default, and that is not tidiness</h2>
 *
 * <p>A route declaring {@code @AccessValue(name = "purpose", from = "purposeCode")} has
 * <strong>promised</strong> the value. Absence is a broken promise and refuses the call, naming the
 * parameter — rather than becoming a null nobody planned for.
 *
 * <p>The reason is what a missing value does to a rule. Conditions only narrow, so a conditional
 * <em>deny</em> whose condition is false <strong>lets the call through</strong>. The rule this whole
 * mechanism exists for is a deny written with {@code !=}, and whether it closes or opens on a missing
 * value would otherwise depend on how one expression engine happens to treat null. Required by
 * default removes the question instead of answering it.
 *
 * <p>{@link #optional()} is the explicit opt-in, and only there do ordinary null semantics apply.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessValue {

    /**
     * What a rule calls this value — {@code purpose}, {@code tier}.
     *
     * <p>Not necessarily what the parameter is called. A rule is read by somebody administering an
     * installation, and {@code purposeCode} is a name from the code rather than from the product.
     */
    String name();

    /**
     * The method parameter to read it out of.
     *
     * <p>⚠️ <strong>A method parameter, not a path variable.</strong> Matched on the name the caller
     * uses, which is {@link ParameterNaming}'s question — {@code @PathVariable("purposeCode")} under
     * Spring MVC, the declared name for a plain call. Nothing here knows what a request is, which is
     * what lets the same declaration gate an agent tool or a scheduled job.
     *
     * <p>⚠️ {@link ParameterNaming#declared()} needs the {@code -parameters} compiler flag. Without
     * it the JDK reports {@code arg0} and a declaration naming {@code purposeCode} matches nothing —
     * which the startup check is there to catch, but only where somebody runs one.
     */
    String from() default "";

    /**
     * A constant this route always publishes.
     *
     * <p>May be written as {@code ${some.property}} and filled from the installation's configuration
     * at load, through {@link org.jmouse.access.PlaceholderResolver} — so a value that differs per
     * installation does not need code that differs per installation.
     *
     * <p>⚠️ <strong>Resolved once, at startup, and fail-closed.</strong> A placeholder nothing can
     * fill refuses to start rather than being compared literally forever. It is configuration, not
     * runtime state: a rule whose meaning could change between two requests would be a rule nobody
     * can reason about from its own text.
     */
    String is() default "";

    /**
     * Whether the call may proceed without it.
     *
     * <p>Only meaningful with {@link #from()} — a literal is never absent. ⚠️ Read the class comment
     * before reaching for it: an optional value in a conditional deny is how a rule comes to open a
     * door it was written to close.
     */
    boolean optional() default false;
}
