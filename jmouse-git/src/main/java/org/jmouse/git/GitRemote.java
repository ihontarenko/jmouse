package org.jmouse.git;

import java.net.URI;

/**
 * A remote repository and the credentials to read it with. Credentials never travel inside the address:
 * an address carrying user information is refused by every {@link GitAddressPolicy}.
 *
 * @param address     the repository address, such as {@code https://github.com/owner/name.git}
 * @param credentials how to authenticate; {@link GitCredentials#anonymous()} for a public repository
 */
public record GitRemote(URI address, GitCredentials credentials) {

    public GitRemote {
        if (address == null) {
            throw new IllegalArgumentException("A remote needs an address.");
        }
        if (credentials == null) {
            throw new IllegalArgumentException("A remote needs credentials; use GitCredentials.anonymous() for none.");
        }
    }

    /** A remote read without credentials. */
    public static GitRemote anonymous(URI address) {
        return new GitRemote(address, GitCredentials.anonymous());
    }
}
