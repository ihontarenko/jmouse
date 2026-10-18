package org.jmouse.git.local;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;

/** A local repository's configured identity, also shown before enabling Commit. */
record GitAuthor(String name, String email) {
    static GitAuthor read(Repository repository) {
        String name =
                repository
                        .getConfig()
                        .getString(
                                ConfigConstants.CONFIG_USER_SECTION,
                                null,
                                ConfigConstants.CONFIG_KEY_NAME);
        String email =
                repository
                        .getConfig()
                        .getString(
                                ConfigConstants.CONFIG_USER_SECTION,
                                null,
                                ConfigConstants.CONFIG_KEY_EMAIL);
        return new GitAuthor(name == null ? "" : name, email == null ? "" : email);
    }

    boolean configured() {
        return !name.isBlank() && !email.isBlank();
    }
}
