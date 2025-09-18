package org.jmouse.core.cache;

/**
 * 🧽 Cache that benefits from periodic maintenance (e.g., CMS decay, doorkeeper reset).
 */
public interface MaintainedCache {
    void maintenance();
}
