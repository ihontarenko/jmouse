package org.jmouse.validator;

import org.jmouse.core.Verify;

/**
 * Default implementation of {@link MessageCodesResolver} producing Spring-like message code chains. 🧩
 *
 * <p>Code resolution strategy builds progressively less specific codes so a message source can
 * fallback from the most specific key to the most generic one.</p>
 *
 * <p><b>Separator:</b> {@value #SEPARATOR}</p>
 *
 * <h3>Object codes</h3>
 * <ul>
 *   <li>{@code objectName + "." + errorCode}</li>
 *   <li>{@code errorCode}</li>
 * </ul>
 *
 * <h3>Field codes</h3>
 * <ul>
 *   <li>{@code objectName + "." + fieldPath + "." + errorCode}</li>
 *   <li>{@code fieldPath + "." + errorCode}</li>
 *   <li>{@code errorCode}</li>
 * </ul>
 *
 * <p>If {@code objectName} is blank, object scoping is omitted.
 * If {@code fieldPath} is blank, field resolution falls back to object codes.</p>
 */
public final class SimpleMessageCodesResolver implements MessageCodesResolver {

    /**
     * Path separator used to compose hierarchical error codes.
     */
    public static final String SEPARATOR = ".";

    /**
     * Resolve message codes for an object-level error.
     *
     * <p>If {@code objectName} is blank, returns {@code [errorCode]}.</p>
     *
     * @param objectName logical object name (may be {@code null}/blank)
     * @param errorCode base error code (must not be blank)
     * @return ordered array of message codes (never {@code null})
     */
    @Override
    public String[] resolveCodes(String objectName, String errorCode) {
        Verify.notBlank(errorCode, "errorCode");

        if (isBlank(objectName)) {
            return new String[]{errorCode};
        }

        return new String[]{
                objectName + SEPARATOR + errorCode,
                errorCode
        };
    }

    /**
     * Resolve message codes for a field-level error.
     *
     * <p>Resolution rules:</p>
     * <ul>
     *   <li>If {@code fieldPath} is blank → delegates to {@link #resolveCodes(String, String)}</li>
     *   <li>If {@code objectName} is blank → returns {@code [fieldPath + "." + errorCode, errorCode]}</li>
     *   <li>Otherwise → returns full chain with object + field + code</li>
     * </ul>
     *
     * @param objectName logical object name (may be {@code null}/blank)
     * @param fieldPath field path/name (may be {@code null}/blank)
     * @param errorCode base error code (must not be blank)
     * @return ordered array of message codes (never {@code null})
     */
    @Override
    public String[] resolveCodes(String objectName, String fieldPath, String errorCode) {
        Verify.notBlank(errorCode, "errorCode");

        if (isBlank(fieldPath)) {
            return resolveCodes(objectName, errorCode);
        }

        if (isBlank(objectName)) {
            return new String[]{
                    fieldPath + SEPARATOR + errorCode,
                    errorCode
            };
        }

        return new String[]{
                objectName + SEPARATOR + fieldPath + SEPARATOR + errorCode,
                fieldPath + SEPARATOR + errorCode,
                errorCode
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
