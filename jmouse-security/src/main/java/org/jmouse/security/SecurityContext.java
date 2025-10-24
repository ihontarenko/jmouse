package org.jmouse.security;

/**
 * 📦 Holds the current Authentication for the execution.
 */
public interface SecurityContext {

    static SecurityContext empty() {
        return ofAuthentication(null);
    }

    static SecurityContext ofAuthentication(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.newContext();
        context.setAuthentication(authentication);
        return context;
    }

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