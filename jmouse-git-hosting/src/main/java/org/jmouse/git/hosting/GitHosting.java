package org.jmouse.git.hosting;

import org.jmouse.git.GitCredentials;
import org.jmouse.git.GitRemote;

/**
 * One Git hosting — GitHub or a GitLab instance — and what it can say about its repositories and tokens.
 *
 * <p>An instance serves one host: a GitHub Enterprise server or a self-hosted GitLab is its own instance, built
 * with that host's base address. An adopter holding several picks the one that {@link #recognises(String)} an
 * address. Every network call is one bounded GET per request and refuses with a {@link HostingException}; no call
 * retries, and every reply and refusal carries the hosting's rate-limit state.
 */
public interface GitHosting {

    /** The web host this hosting serves, as {@link RepositoryReference#host()} records it. */
    String host();

    /**
     * Which repository an https address names. The address may be the repository's page, its clone address, or a
     * deeper page under it ({@code /tree/main}, {@code /-/blob/…}); only the owner and name are kept. No network call.
     *
     * @throws HostingException {@link HostingException.Reason#ADDRESS_NOT_RECOGNISED} for an address of another host,
     *                          a scheme other than https, credentials inside it, or a path that names no repository
     */
    RepositoryReference parseRepositoryAddress(String address);

    /** Whether {@link #parseRepositoryAddress(String)} would accept the address. */
    default boolean recognises(String address) {
        try {
            parseRepositoryAddress(address);
            return true;
        } catch (HostingException failure) {
            if (failure.reason() == HostingException.Reason.ADDRESS_NOT_RECOGNISED) {
                return false;
            }
            throw failure;
        }
    }

    /**
     * A token in the form this hosting's Git endpoint expects for https clones — the username beside it is the
     * hosting's convention, so the same value inspects, lists and clones.
     */
    GitCredentials.AccessToken accessToken(String token);

    /**
     * Whether a repository is private, and its default branch.
     *
     * @param repository  the repository
     * @param credentials {@link GitCredentials#anonymous()} for a public repository, or a token
     */
    HostingReply<RepositoryDescription> describe(RepositoryReference repository, GitCredentials credentials);

    /**
     * Whether the hosting accepts a token, which account it acts as, and the scopes it reports. A rejected token is an
     * answer, {@link TokenInspection.Rejected}, not a refusal.
     */
    HostingReply<TokenInspection> inspectToken(GitCredentials.AccessToken token);

    /** One page of the repositories the token can see: its own, its collaborations and its organisations' or groups'. */
    HostingReply<RepositoryPage> repositories(GitCredentials.AccessToken token, PageRequest page);

    /**
     * The https clone address of a repository, paired with the credentials to read it — ready for
     * {@code jmouse-git}'s {@code GitClient}. No network call.
     *
     * @throws HostingException {@link HostingException.Reason#ADDRESS_NOT_RECOGNISED} for a repository of another host
     */
    GitRemote cloneRemote(RepositoryReference repository, GitCredentials credentials);
}
