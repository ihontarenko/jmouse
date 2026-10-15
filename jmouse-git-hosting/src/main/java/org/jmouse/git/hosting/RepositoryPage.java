package org.jmouse.git.hosting;

import java.util.List;
import java.util.Optional;

/**
 * One page of the repositories a token can see.
 *
 * @param repositories the page's repositories, in the hosting's stable order
 * @param request      the page this is
 * @param hasNextPage  whether the hosting reported another page after it
 */
public record RepositoryPage(List<RepositoryDescription> repositories, PageRequest request, boolean hasNextPage) {

    public RepositoryPage {
        if (repositories == null || request == null) {
            throw new IllegalArgumentException("A repository page needs its repositories and its request.");
        }
        repositories = List.copyOf(repositories);
    }

    /** The request for the following page, when there is one. */
    public Optional<PageRequest> nextPage() {
        if (!hasNextPage) {
            return Optional.empty();
        }
        return Optional.of(request.next());
    }
}
