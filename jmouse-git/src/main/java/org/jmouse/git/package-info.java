/**
 * Git without hosting knowledge: bounded checkouts of one branch of a remote repository, through JGit.
 *
 * <p>{@link org.jmouse.git.GitClient} is the contract callers use and {@link org.jmouse.git.JGitClient}
 * its in-process implementation. A {@link org.jmouse.git.GitRemote} pairs an address with
 * {@link org.jmouse.git.GitCredentials}; a {@link org.jmouse.git.GitAddressPolicy} decides which addresses
 * may be reached at all; {@link org.jmouse.git.GitLimits} bounds every call in time, bytes and history
 * depth. Every refusal is a {@link org.jmouse.git.GitException} with a typed reason, and no message, string
 * form or exception ever carries a token.
 *
 * <p>The module holds no persistence, scheduling, retries or product policy — those are the adopter's.
 */
package org.jmouse.git;
