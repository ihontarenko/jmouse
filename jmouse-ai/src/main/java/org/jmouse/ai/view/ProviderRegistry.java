package org.jmouse.ai.view;

import java.util.Optional;

/**
 * Which model provider is in force, and whether it can authenticate.
 *
 * <p>⚠️ <strong>Never a key.</strong> This port answers <em>whether</em> one is set, and there is no
 * method that could be made to answer with the key itself. That is not caution about a screen somebody
 * might write badly — it is the only shape in which "read the provider configuration" is a safe thing
 * to expose at all. A management screen that can read a key back is a management screen that leaks
 * one, through its own audit log, through a browser cache, through whatever proxies the request.
 *
 * <p>Declared here rather than in {@code jmouse-ai-provider} on purpose: the two mechanisms — tools,
 * and talking to a model — meet only in {@code jmouse-ai-conversation}, and a read port that dragged
 * {@code ProviderSettings} into this module would be the import that joins them for good. So the shape
 * below is plain text and numbers, and the adapter over real settings lives where the two already meet.
 */
public interface ProviderRegistry {

    /**
     * What is configured, with the credential reduced to a yes or a no.
     *
     * @param providerName  which implementation answers, e.g. {@code anthropic}
     * @param model         the model name as the provider spells it
     * @param apiUrl        where it is called, or null where the provider's own default is in use
     * @param maximumTokens     the ceiling on one answer
     * @param keyConfigured whether a key is set. <strong>Never the key</strong>
     */
    /**
     * @param usable whether a call through this configuration could actually be sent.
     *
     *               <p>⚠️ <strong>Not the same as {@code keyConfigured}, and conflating them was a real
     *               bug.</strong> Every caller used to read "has a key" as "will work", which is right
     *               for a hosted provider and wrong for one running on this machine: a local model has
     *               no credential to give, so the assistant reported itself unavailable while being
     *               perfectly able to answer. Whether a key is <em>needed</em> is the provider's fact,
     *               resolved where the catalogue is visible, and this is the answer callers want.</p>
     */
    record ActiveProvider(
            String  providerName,
            String  model,
            String  apiUrl,
            int     maximumTokens,
            boolean keyConfigured,
            boolean usable
    ) {

        /** What a screen shows: enough to recognise the configuration, nothing to authenticate with. */
        public String describe() {
            if (usable) {
                return providerName + " / " + model + (keyConfigured ? " (key set)" : " (no key needed)");
            }

            return providerName + " / " + model
                 + " (NO KEY — calls will be refused before they are sent)";
        }
    }

    /**
     * The provider in force, or empty where none is configured.
     *
     * <p>Empty rather than throwing, because "nothing is configured" is an ordinary state a screen
     * exists to show — it is what a person opens the page to find out.
     */
    Optional<ActiveProvider> active();

    /** A product with no model provider at all. Tools without a model is a supported arrangement. */
    static ProviderRegistry none() {
        return Optional::empty;
    }
}
