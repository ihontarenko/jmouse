package org.jmouse.storage;

import org.jmouse.core.MediaType;
import org.jmouse.http.ContentDisposition;

/**
 * 🎁 How the backend should present bytes it serves directly.
 *
 * <p>A direct link bypasses the application, so headers the application would normally attach have
 * to be baked into the link itself. <em>Deciding</em> what those headers say stays with the caller;
 * a backend only applies them.</p>
 *
 * @param contentType        type the backend should respond with
 * @param contentDisposition disposition the backend should respond with, filename included
 */
public record Presentation(MediaType contentType, ContentDisposition contentDisposition) {
}
