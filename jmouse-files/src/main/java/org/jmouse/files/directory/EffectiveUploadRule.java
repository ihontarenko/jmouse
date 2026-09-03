package org.jmouse.files.directory;

import org.jmouse.storage.delivery.ContentDispositions;
import org.jmouse.storage.policy.AcceptanceMode;
import org.jmouse.storage.policy.UploadPolicy;

import java.util.Set;

/**
 * 🛃 The upload rule that actually applies to a folder, as a screen has to draw it.
 *
 * <p>{@link DirectoryUploadConfiguration} is what somebody <em>wrote</em>; this is what a folder
 * <em>gets</em> — after inheritance, with the installation's size limit filled in where the folder
 * stated none, and with the one fact a screen cannot work out for itself already computed.</p>
 *
 * <h3>⚠️ {@code admitsActiveContent} is a FACT, not a verdict</h3>
 *
 * <p>Not {@code unsafe}, not {@code dangerous}, not {@code warning}. The owner decided this on purpose —
 * the library reserves no type and declined to say no on anybody's behalf — and an API that editorialises
 * about a deliberate choice is an API somebody stops reading. What was declined was a <em>prohibition</em>;
 * a warning was never declined, and hidden risk is not what "understood risk" means.</p>
 *
 * <p>⚠️ Computed here, from {@link ContentDispositions}' own lists, and nowhere else. Three interfaces
 * each keeping their own idea of what counts as active content would be three lists that drift, and the
 * one that drifts is the one that stops warning.</p>
 *
 * @param mode                how the lists are read
 * @param contentTypes        bare {@code type/subtype} values
 * @param extensions          extensions without their dot
 * @param maxSizeBytes        largest content accepted here
 * @param admitsActiveContent whether anything that executes in a browser could be stored here
 * @param admitsNothing       whether this rule accepts nothing at all — an allowlist listing nothing
 */
public record EffectiveUploadRule(AcceptanceMode mode, Set<String> contentTypes, Set<String> extensions,
                                  long maxSizeBytes, boolean admitsActiveContent,
                                  boolean admitsNothing) {

    /**
     * 🏗️ Describe the rule a policy enforces.
     *
     * @param policy the resolved policy
     * @return the rule, as a screen reads it
     */
    public static EffectiveUploadRule of(UploadPolicy policy) {
        return new EffectiveUploadRule(
                policy.mode(), policy.contentTypes(), policy.extensions(), policy.maxSizeBytes(),
                admitsActiveContent(policy), admitsNothing(policy));
    }

    /**
     * ⚡ Whether anything that executes in a browser could be stored under this rule.
     *
     * <p>Both lists are asked, because a client controls both: a rule admitting {@code html} by
     * extension and nothing at all by type still admits it.</p>
     */
    private static boolean admitsActiveContent(UploadPolicy policy) {
        if (policy.mode() == AcceptanceMode.ALLOW_LIST) {
            return containsAny(policy.contentTypes(), ContentDispositions.activeContentTypes())
                   || containsAny(policy.extensions(), ContentDispositions.activeContentExtensions());
        }

        // Under a denylist, what is NOT listed is admitted — so the rule admits active content unless it
        // names every way in. Naming one type and forgetting the matching extension leaves the door open,
        // which is exactly the case worth reporting.
        return !policy.contentTypes().containsAll(ContentDispositions.activeContentTypes())
               || !policy.extensions().containsAll(ContentDispositions.activeContentExtensions());
    }

    private static boolean admitsNothing(UploadPolicy policy) {
        return policy.mode() == AcceptanceMode.ALLOW_LIST
               && policy.contentTypes().isEmpty()
               && policy.extensions().isEmpty();
    }

    private static boolean containsAny(Set<String> listed, Set<String> candidates) {
        return candidates.stream().anyMatch(listed::contains);
    }
}
