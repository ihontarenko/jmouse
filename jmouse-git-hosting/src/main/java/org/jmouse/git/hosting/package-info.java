/**
 * Git hosting: GitHub and GitLab behind one interface, {@link org.jmouse.git.hosting.GitHosting}.
 *
 * <p>A hosting answers what plain Git cannot: which repository an address names, whether it is private and which
 * branch is its default, whether a token is valid and what it may read, which repositories a token can see, and the
 * clone address — a {@link org.jmouse.git.GitRemote} — to hand to {@code jmouse-git}. It reuses that module's
 * {@link org.jmouse.git.GitCredentials}, so a token is one value from inspection to clone.
 *
 * <p>Every call is one bounded GET through a {@link org.jmouse.git.hosting.HostingTransport}: timed, byte-capped,
 * never following redirects. Rate-limit headers are surfaced on every reply and on every refusal rather than retried.
 * The module holds no persistence, retries, caching or product policy — those are the adopter's.
 */
package org.jmouse.git.hosting;
