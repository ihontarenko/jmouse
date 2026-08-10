package org.jmouse.access;

/**
 * Who is asking, and on whose behalf.
 *
 * <p>Three paths tend to go around an authorization system rather than through it — a service
 * sub-account capped against its master, an administrator working as somebody else, and an
 * unauthenticated share link. Each is usually a special case inside a guard. Here each is a
 * <em>kind of subject</em>, which is what lets one engine answer for all of them.
 *
 * <p><strong>Identifiers, not accounts.</strong> This record used to carry the product's own user
 * entity three times over and read the current one out of Spring's security context, which meant the
 * engine could not be handed a subject by anything that was not that product. It carries opaque
 * identifiers instead: the engine compares them, passes them to the {@link org.jmouse.access.spi.GrantStore}
 * and prints them, and it needs nothing else. Building one from an account, a session or a token is
 * the caller's job.
 *
 * @param principalId the account the request runs as, or null where nobody is signed in
 * @param masterId    the account that owns {@link #principalId} where it is a service sub-account, and
 *                    null otherwise. An agent's effective set is intersected with this one's, in the
 *                    same scope
 * @param originId    the administrator really acting, where this is an impersonated session. This
 *                    grants nothing: it exists so that a refusal and an audit entry can name both
 *                    identities
 * @param shareToken  the token a request arrived with where there is no principal at all, and null
 *                    otherwise. A share is an anonymous subject with a derived permission set rather
 *                    than a route that skips the question
 * @param description what a log line and the control room print. Supplied by whoever built the
 *                    subject, because how much of a person to name in a log is a question about that
 *                    product's users rather than about authorization
 */
public record Subject(
        String principalId,
        String masterId,
        String originId,
        String shareToken,
        String description
) {

    private static final Subject ANONYMOUS = new Subject(null, null, null, null, "anonymous");

    /** Nobody: no principal and no share token. */
    public static Subject anonymous() {
        return ANONYMOUS;
    }

    /** A signed-in account that owns its own rows. */
    public static Subject of(String principalId, String description) {
        return principalId == null ? anonymous() : new Subject(principalId, null, null, null, description);
    }

    /** A service sub-account, whose ceiling is its master's in every scope. */
    public static Subject agent(String principalId, String masterId, String description) {
        return new Subject(principalId, masterId, null, null, description);
    }

    /** A share link: no principal, and a permission set derived from what the share grants. */
    public static Subject ofShareToken(String shareToken) {
        return new Subject(null, null, null, shareToken, "share:" + shareToken);
    }

    /** The same subject, recorded as being driven by an administrator working as them. */
    public Subject workedAsBy(String administratorId, String description) {
        return administratorId == null
                ? this
                : new Subject(principalId, masterId, administratorId, shareToken, description);
    }

    public boolean isAuthenticated() {
        return principalId != null;
    }

    /** A service sub-account: its ceiling is its master's, in every scope. */
    public boolean isAgent() {
        return masterId != null;
    }

    public boolean isImpersonated() {
        return originId != null;
    }

    public boolean isShare() {
        return principalId == null && shareToken != null;
    }

    /**
     * Whose rows the {@linkplain ScopeNature#OWN_ROWS own-rows scope} means for this subject.
     *
     * <p>A service sub-account owns nothing — everything it creates belongs to its master, which is
     * what stops deleting an agent from orphaning the data it was created to capture. So "mine" for an
     * agent is <em>the master's</em> rows, and an ownership check comparing identifiers alone would
     * answer "not yours" about every record the agent exists to look after.
     */
    public String ownedRowsBelongTo() {
        return isAgent() ? masterId : principalId;
    }

    /** What the debug line and the control room print. */
    public String describe() {
        return description == null ? String.valueOf(principalId) : description;
    }
}
