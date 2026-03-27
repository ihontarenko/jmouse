package org.jmouse.beans.resolve;

import java.util.List;

/**
 * Resolved bean candidate with metadata. 🧩
 *
 * @param name    bean name
 * @param bean    bean instance
 * @param type    bean runtime type
 * @param primary whether this bean is marked as primary
 */
public record BeanCandidate(
        String name,
        Object bean,
        Class<?> type,
        boolean primary
) {

    /**
     * Returns a single candidate or fails if multiple are present.
     *
     * @param candidates list of candidates
     * @param type       requested type (for error reporting)
     * @return single candidate or {@code null} if none
     *
     * @throws IllegalStateException if more than one candidate exists
     */
    public static BeanCandidate single(List<BeanCandidate> candidates, Class<?> type) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() > 1) {
            throw new IllegalStateException(
                    "Multiple beans found for type '%s': %s"
                            .formatted(type.getName(), candidates.stream().map(BeanCandidate::name).toList())
            );
        }

        return candidates.getFirst();
    }

    /**
     * Returns the primary candidate if present.
     *
     * @param candidates list of candidates
     * @param type       requested type (for error reporting)
     * @return primary candidate or {@code null} if none
     *
     * @throws IllegalStateException if multiple primary candidates exist
     */
    public static BeanCandidate primary(List<BeanCandidate> candidates, Class<?> type) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<BeanCandidate> primaryCandidates = candidates.stream()
                .filter(BeanCandidate::primary)
                .toList();

        if (primaryCandidates.isEmpty()) {
            return null;
        }

        if (primaryCandidates.size() > 1) {
            throw new IllegalStateException(
                    "Multiple primary beans found for type '%s': %s"
                            .formatted(type.getName(), primaryCandidates.stream().map(BeanCandidate::name).toList())
            );
        }

        return primaryCandidates.getFirst();
    }

}