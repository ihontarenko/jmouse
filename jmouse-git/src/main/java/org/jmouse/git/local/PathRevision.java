package org.jmouse.git.local;

import java.time.Instant;

/**
 * One commit that changed a file, as the file's history reads it: where the file stood in it — a file moved stands at
 * another path in its older commits —, who made it, when, and everything the commit says.
 *
 * @param commit  the commit's identifier
 * @param path    the file's path in that commit
 * @param author  the author's name
 * @param email   the author's e-mail address
 * @param time    when the change was made: the author's time
 * @param message the commit's whole message, trailers included
 * @param content the identifier of the file's content in that commit: its text is read by it, when it is wanted
 */
public record PathRevision(String commit, String path, String author, String email, Instant time, String message, String content) {
}
