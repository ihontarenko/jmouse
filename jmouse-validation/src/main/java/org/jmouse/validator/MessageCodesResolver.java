package org.jmouse.validator;

public interface MessageCodesResolver {

    /**
     * Resolve error codes for a global (object-level) error.
     *
     * @param objectName logical object name (e.g. "user")
     * @param errorCode base error code (e.g. "invalid")
     * @return ordered list of codes, most specific first
     */
    String[] resolveCodes(String objectName, String errorCode);

    /**
     * Resolve error codes for a field error.
     *
     * @param objectName logical object name (e.g. "user")
     * @param fieldPath field path (e.g. "profile.email")
     * @param errorCode base error code (e.g. "notBlank")
     * @return ordered list of codes, most specific first
     */
    String[] resolveCodes(String objectName, String fieldPath, String errorCode);
}
