package org.jmouse.access.el.condition;

import org.jmouse.access.AuthenticationFacts;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.spi.DeferredValue;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code caller is assured('mfa')} — whether the caller proved who they are <em>strongly</em> enough.
 *
 * <p><em>Step-up</em>: the other axis from {@link FreshTest}. Not <strong>when</strong> they proved it but
 * <strong>how</strong>.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' billing:write deny   when caller is not assured('mfa')
 * &#64;SPACE:'id-space-01' key:export deny      when caller is not assured('webauthn')
 * </pre>
 *
 * <p>It matches the name a rule wrote against two published facts — the
 * {@linkplain AuthenticationFacts#ASSURANCE_LEVEL level} and the
 * {@linkplain AuthenticationFacts#AUTHENTICATION_METHODS methods} — so an installation may publish either
 * a single level it has decided on, or simply the methods that were used, and rules read naturally
 * against both.
 *
 * <h2>⚠️ The engine takes no view on what a level means</h2>
 *
 * <p>{@code mfa}, {@code high}, an OIDC {@code acr} value — it compares strings, case-insensitively, and
 * that is all. There is deliberately <strong>no ordering</strong>: an installation that wants
 * <em>"at least"</em> semantics has a ladder in its head that the engine cannot see, and guessing at one
 * would make {@code assured('high')} silently true for a level somebody thought was weaker.
 *
 * <p>A rule wanting one of several writes it out: {@code caller is assured('mfa') or caller is
 * assured('webauthn')}.
 *
 * <h2>⚠️ An absent fact refuses</h2>
 *
 * <p>An installation with no multi-factor authentication has nothing honest to publish, so every
 * {@code assured(…)} rule refuses. That is the fail-closed half and it is correct — ⚠️ and publishing an
 * invented level to make the rule pass is strictly worse than publishing none, because it passes a rule
 * written to protect something.
 */
public class AssuredTest implements AccessTest {

    public static final String NAME = "assured";

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        String           wanted   = required(arguments).trim().toLowerCase(Locale.ROOT);
        ConditionContext decision = ConditionBinding.require(context);
        Set<String>      proven   = provenIn(decision);

        if (proven.isEmpty()) {
            throw new IllegalStateException(
                    ("nothing published '%s' or '%s', so there is no way to tell how strongly this caller "
                     + "proved who they are. An installation publishes them as ambient values — and one "
                     + "with no multi-factor authentication should publish neither rather than inventing "
                     + "a level, because an invented one passes the rule that was written to stop it.")
                            .formatted(AuthenticationFacts.ASSURANCE_LEVEL,
                                       AuthenticationFacts.AUTHENTICATION_METHODS));
        }

        return proven.contains(wanted);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        if (arguments.isEmpty() || arguments.get(0) == null) {
            return;
        }

        if (arguments.get(0).isBlank()) {
            throw new IllegalArgumentException(
                    "assured needs a level to compare against — for example `caller is assured('mfa')`");
        }
    }

    /** The level and the methods, flattened — an installation may publish either, or both. */
    private static Set<String> provenIn(ConditionContext decision) {
        Set<String> proven = new LinkedHashSet<>();

        collect(proven, DeferredValue.resolve(decision.value(AuthenticationFacts.ASSURANCE_LEVEL)));
        collect(proven, DeferredValue.resolve(decision.value(AuthenticationFacts.AUTHENTICATION_METHODS)));

        return proven;
    }

    private static void collect(Set<String> proven, Object published) {
        switch (published) {
            case null -> {
            }
            case Collection<?> many -> many.forEach(one -> collect(proven, one));
            case Object[] many -> {
                for (Object one : many) {
                    collect(proven, one);
                }
            }
            default -> {
                String written = String.valueOf(published).trim().toLowerCase(Locale.ROOT);

                if (!written.isEmpty()) {
                    proven.add(written);
                }
            }
        }
    }

    private static String required(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            throw new IllegalArgumentException(
                    "assured needs a level to compare against — for example `caller is assured('mfa')`");
        }

        return String.valueOf(arguments.getFirst());
    }
}
