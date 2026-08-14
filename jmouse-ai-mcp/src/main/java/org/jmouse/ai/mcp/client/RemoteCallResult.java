package org.jmouse.ai.mcp.client;

/**
 * What a server this application connected to answered with.
 *
 * <p>Three fields, and the third is the one that earns the type. A remote server distinguishes
 * <em>"here is your answer"</em> from <em>"I will not do that"</em>, and collapsing the two would leave
 * a refusal from somebody else's machine looking, to everything downstream, exactly like a successful
 * result whose payload happened to be an apology.
 *
 * <p>⚠️ This carries no third case for <em>"the server could not be reached"</em>, deliberately. That
 * is not an answer, so it is not a result — it is {@link RemoteToolException}, and the distinction
 * matters because a refusal ends by promising nothing was changed and an unanswered call cannot
 * promise anything at all.
 *
 * @param payload   the structured answer where the remote sent one, otherwise the text
 * @param text      what the remote said, for a model to read and a person to quote
 * @param refused   whether the remote declined to do it
 */
public record RemoteCallResult(Object payload, String text, boolean refused) {

    public static RemoteCallResult answered(Object payload, String text) {
        return new RemoteCallResult(payload == null ? text : payload, text, false);
    }

    public static RemoteCallResult refused(String text) {
        return new RemoteCallResult(text, text, true);
    }
}
