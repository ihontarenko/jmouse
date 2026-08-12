package org.jmouse.access;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * What a rule may say about <strong>who is asking</strong> — the declared half of {@link Subject}.
 *
 * <h2>⚠️ Why a view and not the record itself</h2>
 *
 * <p>Conditions used to bind {@code caller} straight to {@link Subject}, and the consequence is the
 * failure this whole module is built to avoid. {@code Subject} carries {@code description}; it has
 * never carried {@code name}. So
 *
 * <pre>{@code @GLOBAL user:write when caller.name is contains ('GOD')}</pre>
 *
 * <p>tokenises legally, compiles, passes every check, boots clean — and <strong>never holds</strong>.
 * On a personal grant that quietly costs one person a permission. Inside a role it is a silent mass
 * denial for everybody holding it, because a conditional allow that cannot hold subtracts.
 *
 * <p>A view fixes it in the only way that lasts: the members become a <em>declared vocabulary</em>,
 * so {@link #MEMBERS} is what a condition is checked against at load and what the editor completes
 * from. A typo stops being a rule that runs forever and answers wrongly, and becomes a startup
 * failure naming the members that exist.
 *
 * <p>It also keeps {@code Subject} what it is. That record is deliberately "identifiers, not
 * accounts" — the engine compares them, hands them to a store and prints them, and needs nothing
 * else. Adding fields to it so that rules could read them would have made the engine's own model
 * grow to fit a rule language.
 *
 * @param id             the account the request runs as, or null where nobody is signed in
 * @param name           what a log line and the control room call them — {@code Subject.description},
 *                       supplied by the product, because how much of a person to name is a question
 *                       about that product's users
 * @param masterId       the account owning this one where it is a service sub-account, else null
 * @param originId       the administrator really acting, where this is an impersonated session
 * @param agent          whether this is a service sub-account, capped against its master
 * @param impersonated   whether an administrator is working as somebody else
 * @param authenticated  whether anybody is signed in at all
 * @param share          whether this is an anonymous holder of a share link
 */
public record CallerView(
        String  id,
        String  name,
        String  masterId,
        String  originId,
        boolean agent,
        boolean impersonated,
        boolean authenticated,
        boolean share
) {

    /**
     * Every name a rule may write after {@code caller.} — the vocabulary, checked at load.
     *
     * <p>⚠️ <strong>Read off the record rather than written out.</strong> A hand-kept list is the same
     * fact stated twice, and it fails in both directions: a name here that the record does not have
     * is a rule that passes validation and reads null forever, and a component the list forgets is a
     * rule refused although it works. Neither would fail loudly, which is exactly why the list is not
     * allowed to exist.
     */
    public static final List<String> MEMBERS = Arrays.stream(CallerView.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

    /** The view of one subject. Never null, so a rule about an anonymous caller still evaluates. */
    public static CallerView of(Subject subject) {
        if (subject == null) {
            return new CallerView(null, "anonymous", null, null, false, false, false, false);
        }

        return new CallerView(
                subject.principalId(),
                subject.description(),
                subject.masterId(),
                subject.originId(),
                subject.isAgent(),
                subject.isImpersonated(),
                subject.isAuthenticated(),
                subject.isShare());
    }
}
