package org.jmouse.access.enforcement;

import org.jmouse.access.Subject;

/**
 * Who is asking, for the call being served right now.
 *
 * <p>Enforcement cannot answer this and must not try. A {@link Subject} is built from an
 * authenticated account, an impersonation, a share token — all of which belong to whatever does
 * authentication, and none of which this module can see. Spring Security has a context holder,
 * another stack has something else, and a test simply hands one over.
 */
@FunctionalInterface
public interface CurrentSubject {

    /** The subject behind the call, or {@link Subject#anonymous()} where nobody is signed in. */
    Subject get();
}
