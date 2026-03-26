package org.jmouse.beans.resolve;

import java.util.List;

/**
 * Resolved bean candidate with metadata. 🧩
 *
 * @param name    bean name
 * @param bean    bean instance
 * @param type    bean runtime type
 * @param primary whether this bean is primary among candidates
 */
public record BeanCandidate(
        String name,
        Object bean,
        Class<?> type,
        boolean primary
) {

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