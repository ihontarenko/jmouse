package org.jmouse.access;

/**
 * The names under which an installation publishes <em>how</em> the caller proved who they are.
 *
 * <p>The engine resolves <strong>who</strong> is calling — that is {@link Subject}, and it is the same
 * answer whether somebody signed in a minute ago or last Tuesday. These three facts are the other half:
 * how recently it happened, and how strongly.
 *
 * <p>They are published as ambient values, so nothing here reaches a request or a session; whoever owns
 * authentication attaches them, and a condition reads them off
 * {@link org.jmouse.access.spi.ConditionContext#values()}.
 *
 * <h2>⚠️ A name declared here is not a name anybody publishes</h2>
 *
 * <p>Constants are the vocabulary, not a promise that it is populated. An installation that publishes
 * none of these has every rule reading them <strong>refuse</strong> — which is the fail-closed half and
 * is correct, and will still look like a broken installation to whoever wrote the first such rule. Wire
 * the publisher in the same change as the first rule.
 *
 * <h2>What each product can actually say</h2>
 *
 * <ul>
 *   <li>An installation whose tokens come from an OIDC provider has all three for free: {@code auth_time},
 *       {@code amr} and {@code acr} are standard claims.</li>
 *   <li>An installation with its own session-based sign-in has {@link #AUTHENTICATED_AT} — the session's
 *       creation time, which coincides with the sign-in because session-fixation protection makes a new
 *       session at that moment — and honestly has nothing to say for the other two.</li>
 * </ul>
 *
 * <p>⚠️ Publishing an invented assurance level is worse than publishing none. None refuses; an invented
 * one passes a rule written to protect something.
 */
public final class AuthenticationFacts {

    /**
     * When the caller last actually proved who they are, as anything {@link Moments} can read.
     *
     * <p>⚠️ Not when the session was last <em>used</em>. A session touched every minute for a week is a
     * week old to this fact, which is the entire point: a sudo-mode rule asks whether the person is still
     * there, not whether the browser is.
     */
    public static final String AUTHENTICATED_AT = "authenticated-at";

    /**
     * How they proved it — {@code password}, {@code otp}, {@code webauthn}, {@code sso}. A collection, or
     * one value; a caller may have used more than one.
     */
    public static final String AUTHENTICATION_METHODS = "authentication-methods";

    /**
     * How strongly, as a single name the installation chooses — {@code mfa}, {@code high}, an OIDC
     * {@code acr} value.
     *
     * <p>⚠️ The engine takes no view on what the name means. It compares what a rule wrote against what
     * the installation published, and an installation that has not decided on a vocabulary should publish
     * nothing rather than something plausible.
     */
    public static final String ASSURANCE_LEVEL = "assurance-level";

    private AuthenticationFacts() {
    }
}
