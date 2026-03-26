package org.jmouse.beans.resolve;

import java.util.List;

/**
 * Provider of bean candidates for resolution. 📚
 */
public interface BeanCandidateProvider {

    /**
     * Returns all candidates assignable to the given type. 🔎
     *
     * @param type target type
     *
     * @return matching candidates
     */
    List<BeanCandidate> getCandidates(Class<?> type);

    /**
     * Returns a candidate by bean name. 🏷
     *
     * @param name bean name
     *
     * @return matching candidate or {@code null}
     */
    BeanCandidate getCandidate(String name);

}