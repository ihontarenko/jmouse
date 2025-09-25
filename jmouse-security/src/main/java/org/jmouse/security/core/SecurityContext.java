package org.jmouse.security.core;

/**
 * 📦 Holds the current Authentication for the execution.
 */
public interface SecurityContext {

    Authentication getAuthentication();

    void setAuthentication(Authentication authentication);

    class Context implements SecurityContext {

        private Authentication authentication;

        @Override
        public Authentication getAuthentication() {
            return authentication;
        }

        @Override
        public void setAuthentication(Authentication authentication) {
            this.authentication = authentication;
        }
    }

}