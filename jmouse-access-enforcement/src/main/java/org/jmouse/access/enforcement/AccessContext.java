package org.jmouse.access.enforcement;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What this method is doing, published so a rule can be written about it.
 *
 * <p>{@link RequiresAccess} says what the caller must <em>hold</em>. This says what the call
 * <em>is</em> — and the two are different questions that a single permission cannot separate.
 * {@code entry:read} gates listing entries, reading one, exporting and printing; a rule that must
 * close printing and leave reading open has nothing to name until the route says which it is.
 *
 * <pre>{@code
 * @RequiresAccess(permission = Permissions.ENTRY_READ, module = Modules.FORMS, scope = Scopes.SPACE)
 * @AccessContext(
 *     action = Actions.ENTRY_LIST_BY_PURPOSE,
 *     values = {
 *         @AccessValue(name = "purpose", from = "purposeCode"),
 *         @AccessValue(name = "tier",    is   = "${innoventa.tier}"),
 *         @AccessValue(name = "region",  from = "region", optional = true)
 *     })
 * public Page<EntryResponse> resultsByPurpose(@PathVariable String purposeCode, …) { … }
 * }</pre>
 *
 * <p>and a policy may then say
 *
 * <pre>{@code @GLOBAL entry:read deny when action == 'entry.listByPurpose' and purpose != 'HOLDER'}</pre>
 *
 * <h2>⚠️ This gates a call. It does not narrow a list</h2>
 *
 * <p>The rule above refuses {@code /results/OTHER} with a 403 and lets {@code /results/HOLDER} answer
 * exactly the rows it always did. Nothing here filters anything, and a reader expecting a shorter
 * list is expecting a different mechanism and a much larger one.
 *
 * <p>That is also what makes it cheap: an action and its values are <strong>constant for a call</strong>,
 * so the effective permission set is still a function of {@code (subject, scope chain)} and still
 * memoised, and a listing filter is still expressible.
 *
 * <h2>⚠️ Nothing here knows what HTTP is</h2>
 *
 * <p>{@link AccessValue#from()} names a <strong>method parameter</strong>, resolved through the same
 * {@link MethodArguments} + {@link ParameterNaming} pair that
 * {@code @RequiresAccess(resourceId = …)} already uses. For a web route the value is identical —
 * the framework has already bound the path variable into that parameter — and no new mechanism is
 * introduced. The payoff is larger than the tidiness: <strong>the same annotation gates an agent tool
 * or a scheduled job</strong>, because nothing about it is a request.
 *
 * <h2>⚠️ Where it may be written, and what merges</h2>
 *
 * <p>{@link #values()} may be declared on the class as well, and the two merge with the method
 * winning on a name collision — a controller states the tenant once and each method adds its own.
 * {@link #action()} is <strong>method-only</strong>: an action on a class would be a lie, because a
 * controller does several things.
 *
 * <p>⚠️ <strong>Attaching a value inside the method body cannot gate that method.</strong> The guard
 * runs around the invocation, so by the time the body executes the call has already been admitted. A
 * rule reading a value published there would silently never fire — and a conditional allow that never
 * holds is a refusal nobody ordered, which reads as <em>allowed</em> in every log. Publish from the
 * annotation, or — for a caller with no guard in front of it — from
 * {@link org.jmouse.access.spi.AccessContextScope} before its own check.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AccessContext {

    /**
     * What is being done, as a name the installation declares.
     *
     * <p>Written with dots — {@code entry.listByPurpose}, {@code label.print} — because
     * {@code entry:read} is a permission and the two answer different questions. Two vocabularies
     * that looked alike would be misread in both directions on the first busy day.
     *
     * <p>⚠️ Declare them as constants and check them against a catalogue. A free string means
     * {@code entry.listByPurpos} produces a rule that never fires, silently.
     *
     * <p>Method-only. Blank on a class, and blank on a method that has values to publish and no verb
     * worth naming.
     */
    String action() default "";

    /** What the call carries — read by a rule scoped to the action above. */
    AccessValue[] values() default {};
}
