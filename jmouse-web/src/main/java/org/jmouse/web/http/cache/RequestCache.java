package org.jmouse.web.http.cache;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🧭 RequestCache
 *
 * Saves and restores the original HTTP request that triggered authentication.
 * Typical flow: save on entry point ➜ authenticate ➜ restore (redirect) after success.
 */
public interface RequestCache {

    /** Save current request details for later restoration. */
    void saveRequest(HttpServletRequest request, HttpServletResponse response);

    /** Load previously saved request (or null if none). */
    SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response);

    /** Remove any saved request (idempotent). */
    void removeRequest(HttpServletRequest request, HttpServletResponse response);
}
