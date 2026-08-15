package org.jmouse.ai;

/**
 * The names {@link CallerIdentity#attributes()} are filed under.
 *
 * <p><strong>Constants rather than convention, because convention already failed.</strong> Two products
 * carried the same two facts under three different keys — one calling the owner's display name
 * {@code subject.name} and the other {@code caller.subject}, which is not even the same fact — so a
 * trail, a log line and a screen read differently depending on which application had written them.
 * Nothing broke; things merely disagreed, which is worse, because nothing points at it.
 *
 * <p>⚠️ <strong>Every one of these is for display, and none is an authorization input.</strong> The map
 * is whatever a product's resolver happened to know, carried untouched by a mechanism that never reads
 * it. Deciding anything on a value from here means deciding on something no layer validated.
 *
 * <p>A product may file anything else it likes alongside these. The point is only that these mean the
 * same thing everywhere.
 */
public final class CallerAttributes {

    /** What the calling identity is called: an agent's name, or a person's own. */
    public static final String CALLER_NAME = "caller.name";

    /**
     * What the account it acts for is called, where those differ.
     *
     * <p>⚠️ Absent — not equal to {@link #CALLER_NAME} — when the caller acts for itself, so that
     * "acting for somebody" stays a question answered by {@link CallerIdentity#actsForItself()} rather
     * than by comparing two strings that happen to match.
     */
    public static final String SUBJECT_NAME = "caller.subject.name";

    /**
     * The identity-provider subject the caller was matched on.
     *
     * <p>Carried because the local identifier is what everything is keyed on, and the claim is what
     * anybody debugging a token is actually looking at.
     */
    public static final String SUBJECT_CLAIM = "caller.subject.claim";

    /**
     * Which agent this call is running as, where one is involved at all.
     *
     * <p>⚠️ <strong>Not redundant with the caller identifier, and this is the one to know.</strong> An
     * agent acting with its owner's authority <em>is</em> the owner as far as authorization goes, so the
     * caller identifier is the owner's and this is the only place the agent survives. A record's
     * provenance, a trail and a badge all read it — which is why an agent that inherits authority is
     * still a distinct identity rather than a person wearing a different hat.
     */
    public static final String AGENT_ID = "caller.agent.id";

    /** What that agent is called, so a badge prints a name rather than an identifier. */
    public static final String AGENT_NAME = "caller.agent.name";

    /** What the connected client called itself. ⚠️ A claim it made, shown as one, never an identity. */
    public static final String CLIENT_NAME = "caller.client.name";

    /**
     * Which connection this call arrived through.
     *
     * <p>So that a log line about something going wrong names the thing somebody can actually end, rather
     * than an agent with four clients and no clue which one to revoke.
     */
    public static final String CONNECTION_ID = "caller.connection.id";

    private CallerAttributes() {
    }
}
