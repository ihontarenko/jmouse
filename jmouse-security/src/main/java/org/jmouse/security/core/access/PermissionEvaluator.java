package org.jmouse.security.core.access;

import org.jmouse.security.core.Authentication;

/**
 * ✅ Evaluates fine-grained permissions on a domain object or identifier.
 */
public interface PermissionEvaluator {

    boolean hasPermission(Authentication authentication, Object object, String permission);

    boolean hasPermission(Authentication authentication, Object id, String targetType, String permission);

    class DenyAll implements PermissionEvaluator {

        @Override
        public boolean hasPermission(Authentication authentication, Object object, String permission) {
            return false;
        }

        @Override
        public boolean hasPermission(Authentication authentication, Object id, String targetType, String permission) {
            return false;
        }

    }

}