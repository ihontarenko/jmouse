package org.jmouse.script.event;

/**
 * Something a product did, or is about to do, that anybody may care about.
 *
 * <h2>⚠️ A marker, and it earns its keep</h2>
 *
 * <p>A container asked for listeners of {@code Object} hands over every event it publishes — every
 * context refresh, every request, every bean lifecycle notice — and a dispatcher written that way
 * spends its time discarding framework noise. Declared on this, a listener is offered exactly the
 * events the product raises about itself.
 *
 * <h2>⚠️ It deliberately carries NO NAME AND NO TIMESTAMP</h2>
 *
 * <p>A name here would be the name a script writes after {@code on}, and that would put the scripting
 * contract inside the domain — where renaming a class would silently stop somebody's rule firing. The
 * name belongs to the STAGE that declares the event, one layer out.
 *
 * <h2>⚠️ THIS IS ONE OF THREE, AND THE THREE ARE THE SAFETY MODEL</h2>
 *
 * <ul>
 *   <li>{@code DomainEvent} — a script may only <b>observe</b>; it cannot affect anything;</li>
 *   <li>{@link DecisionEvent} — a script may <b>refuse</b>, and the refusal is used;</li>
 *   <li>{@link AmendableEvent} — a script may <b>change</b> what is about to happen.</li>
 * </ul>
 *
 * <p>A stage declares which of the three it is, so what a script may do at a place is knowable before
 * any script is written — by the screen that offers the place, and by the person choosing it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface DomainEvent {

    /**
     * Where this happened, for a product that keeps rules per tenant.
     *
     * <h2>⚠️ NULL IS THE INSTALLATION, AND IT IS THE DEFAULT SO THAT SINGLE-TENANT PRODUCTS SAY NOTHING</h2>
     *
     * <p>Most products have one of everything: one household, one archive, one installation. For those
     * this method is never overridden, never implemented and never thought about, and the mechanism
     * behaves exactly as it did before scopes existed. A required method would make single tenancy the
     * thing you have to write out, which is backwards — and would have meant editing every event class
     * in every product to say <em>null</em>.
     *
     * <h2>⚠️ THE LIBRARY DOES NOT KNOW WHAT A SCOPE IS</h2>
     *
     * <p>It is an opaque string the product chooses the meaning of: a workspace id in Innoventa, and it
     * could as easily be a household, a site or a customer. Nothing here compares it to anything but
     * another one of itself, so a product may not have two things it means at once — a scope that
     * sometimes means a workspace and sometimes a person is two indexes sharing a key.
     *
     * <h2>⚠️ A SCOPE IS NOT A PERMISSION</h2>
     *
     * <p>It decides <strong>which rules run</strong>, not what they may do. A rule confined to a
     * workspace still reaches everything its facades reach; confining <em>that</em> is
     * {@code jmouse-access}'s job, and reading this as a boundary is how a tenant's rule ends up
     * trusted with somebody else's rows.
     *
     * @return the tenant this happened in, or {@code null} for the installation as a whole
     */
    default String scope() {
        return null;
    }
}
