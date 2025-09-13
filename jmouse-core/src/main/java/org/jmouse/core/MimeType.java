package org.jmouse.core;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a MIME media type, consisting of a primary type, subtype, and optional parameters.
 * <p>Supports wildcards for type and subtype, suffix-based matching (e.g., "*+xml"),
 * and extraction of the charset parameter.</p>
 */
public class MimeType {

    /**
     * ✳️ Wildcard for any primary type.
     */
    public static final String WILDCARD_TYPE          = "*";
    /**
     * ✳️ Wildcard prefix for suffix patterns (e.g., {@code *+xml}).
     */
    public static final String WILDCARD_PREFIX_TYPE   = "*+";
    /**
     * ➕ Separator between subtype prefix and suffix (before/after {@code +}).
     */
    public static final String PREFIX_SEPARATOR       = "+";
    /**
     * 🔤 Standard parameter key for character set.
     */
    public static final String PARAMETER_NAME_CHARSET = "charset";
    /**
     * 🐞 Standard parameter key for debug set.
     */
    public static final String PARAMETER_NAME_DEBUG   = "x-debug";
    /**
     * 🗂️ Cache of parsed {@link MimeType} by raw string.
     * <p>Note: simple {@link HashMap} – not thread-safe.</p>
     */
    public static final Map<String, MimeType> CACHE = new HashMap<>();

    private final Map<String, String> parameters;
    private final String              type;
    private final String              subtype;
    private       String              toStringValue;
    private       Charset             charset;

    /**
     * Create a MimeType with wildcard subtype (i.e. "type/*").
     *
     * @param type the primary type (e.g. "text")
     */
    public MimeType(String type) {
        this(type, WILDCARD_TYPE);
    }

    /**
     * Create a MimeType with the given type and subtype.
     *
     * @param type the primary type (e.g. "application")
     * @param subtype the subtype (e.g. "json")
     */
    public MimeType(String type, String subtype) {
        this(type, subtype, Collections.emptyMap());
    }

    /**
     * Create a MimeType with parameters.
     *
     * @param type the primary type
     * @param subtype the subtype
     * @param parameters map of parameters (e.g. charset)
     */
    public MimeType(String type, String subtype, Map<String, String> parameters) {
        parameters.forEach(this::performParameters);
        this.parameters = Collections.unmodifiableMap(parameters);
        this.type = type.toLowerCase();
        this.subtype = subtype.toLowerCase();
    }

    /**
     * Copy constructor.
     *
     * @param other the MimeType to clone
     */
    public MimeType(MimeType other, Map<String, String> parameters) {
        this(other.getType(), other.getSubType(), parameters);
        this.toStringValue = other.toStringValue;
        this.charset = other.charset;
    }

    /**
     * Copy constructor.
     *
     * @param other the MimeType to clone
     */
    public MimeType(MimeType other) {
        this(other, other.getParameters());
    }

    /**
     * Process a parameter entry. Currently only supports 'charset'.
     *
     * @param attribute parameter name
     * @param value parameter value
     */
    public void performParameters(String attribute, String value) {
        if (PARAMETER_NAME_CHARSET.equalsIgnoreCase(attribute)) {
            charset = Charset.forName(value, Charset.defaultCharset());
        }
    }

    /**
     * Determine if this type includes the given other type.
     * <p>Wildcard type always includes any other. Matching subtype
     * or wildcard+suffix logic applies when types match.</p>
     *
     * @param other the other MimeType to check
     * @return true if this includes the other
     */
    public boolean includes(MimeType other) {
        if (other == null) {
            return false;
        }

        if (isWildcardType()) {
            return true;
        }

        if (getType().equals(other.getType())) {
            if (getSubType().equals(other.getSubType())) {
                return true;
            }

            if (isWildcardSubType()) {
                String suffix = getSubTypeSuffix();

                if (suffix != null) {
                    return suffix.equals(other.getSubTypeSuffix());
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Check mutual compatibility (symmetric includes).
     *
     * @param other the other MimeType
     * @return true if either includes the other
     */
    public boolean compatible(MimeType other) {
        return includes(other) || other.includes(this);
    }

    /**
     * @return true if type == '*'
     */
    public boolean isWildcardType() {
        return WILDCARD_TYPE.equals(getType());
    }

    /**
     * @return true if subtype is '*' or starts with '*+'
     */
    public boolean isWildcardSubType() {
        return WILDCARD_TYPE.equals(getSubType()) || getSubType().startsWith(WILDCARD_PREFIX_TYPE);
    }

    /**
     * @return the full string type
     */
    public String getStringType() {
        return type + "/" + subtype;
    }

    /**
     * @return the primary type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the subtype
     */
    public String getSubType() {
        return subtype;
    }

    /**
     * @return the subtype prefix (before '+'), or null if none
     */
    public String getSubTypePrefix() {
        String prefix    = null;
        int    lastIndex = getSubType().lastIndexOf(PREFIX_SEPARATOR);

        if (lastIndex != -1) {
            prefix = getSubType().substring(0, lastIndex);
        }

        return prefix;
    }

    /**
     * @return the subtype suffix (after '+'), or null if none
     */
    public String getSubTypeSuffix() {
        String suffix    = null;
        int    lastIndex = getSubType().lastIndexOf(PREFIX_SEPARATOR);

        if (lastIndex != -1) {
            suffix = getSubType().substring(lastIndex + 1);
        }

        return suffix;
    }

    /**
     * ⚖️ Specificity ordering helper.
     *
     * <p>Returns {@code true} if {@code this} is less specific than {@code that}.
     * Comparison rules:
     * <ol>
     *   <li>Wildcard type is less specific than concrete type.</li>
     *   <li>With same type, wildcard (or suffix-wildcard) subtype is less specific.</li>
     *   <li>If both type &amp; subtype equal, fewer parameters is less specific.</li>
     * </ol>
     * </p>
     *
     * @param that other mime type
     * @return {@code true} if {@code this} is less specific
     */
    public boolean isLowerPriority(MimeType that) {
        boolean thisWildcard    = this.isWildcardType();
        boolean thisSubWildcard = this.isWildcardSubType();
        boolean thatWildcard    = that.isWildcardType();
        boolean thatSubWildcard = that.isWildcardSubType();

        if (thisWildcard && !thatWildcard) {
            return true;
        } else if (!thisWildcard && thatWildcard) {
            return false;
        } else {
            if (thisSubWildcard && !thatSubWildcard) {
                return true;
            } else if (!thisSubWildcard && thatSubWildcard) {
                return false;
            } else if (getType().equals(that.getType()) && getSubType().equals(that.getSubType())) {
                int thisParameterCount = this.parameters.size();
                int thatParameterCount = that.parameters.size();
                return thatParameterCount > thisParameterCount;
            }
        }

        return false;
    }

    /**
     * ⚖️ Inverse of {@link #isLowerPriority(MimeType)}.
     *
     * @param that other mime type
     * @return {@code true} if {@code this} is more specific than {@code that}
     */
    public boolean isHigherPriority(MimeType that) {
        return that.isLowerPriority(this);
    }

    /**
     * ⚖️ Returns the more specific of two MIME types (null-safe, with tie-break).
     *
     * <p>Specificity rules are defined by {@link MimeType#isMoreSpecific(MimeType)}.
     * Tie-breakers:
     * <ol>
     *   <li>Greater parameter count ⇒ more specific.</li>
     *   <li>If still equal, returns {@code typeA} to keep selection stable.</li>
     * </ol>
     * </p>
     *
     * @param typeA the first MIME type (may be {@code null})
     * @param typeB the second MIME type (may be {@code null})
     * @return the chosen MIME type, or {@code null} if both are {@code null}
     */
    public static MimeType getMoreSpecific(MimeType typeA, MimeType typeB) {
        if (typeA == null) return typeB;
        if (typeB == null) return typeA;

        return typeA.isHigherPriority(typeB) ? typeA : typeB;
    }

    /**
     * Get a parameter value.
     *
     * @param name parameter name
     * @return value or null
     */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * @return unmodifiable map of all parameters
     */
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    /**
     * @return resolved Charset, if provided as a parameter
     */
    public Charset getCharset() {
        return charset;
    }

    @Override
    public String toString() {
        String value = toStringValue;

        if (value == null) {
            StringBuilder builder = new StringBuilder();
            toString(builder);
            value = builder.toString();
            toStringValue = value;
        }

        return toStringValue;
    }

    /**
     * Equality is case-insensitive for type/subtype and considers parameters.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof MimeType that &&
                this.type.equalsIgnoreCase(that.type) &&
                this.subtype.equalsIgnoreCase(that.subtype) &&
                parameters.equals(that.parameters)));
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.subtype.hashCode();
        result = 31 * result + this.parameters.hashCode();
        return result;
    }

    /**
     * Append string form to a StringBuilder.
     *
     * @param builder the StringBuilder to append to
     */
    public void toString(StringBuilder builder) {
        builder.append(type);
        builder.append('/');
        builder.append(subtype);

        if (!parameters.isEmpty()) {
            parameters.forEach((key, value) -> {
                builder.append("; ");
                builder.append(key);
                builder.append('=');
                builder.append(value);
            });
        }
    }

    /**
     * Return (and cache) a MimeType parsed from the given string.
     *
     * @param type the raw mime type string
     * @return the MimeType instance from cache or newly parsed
     */
    public static MimeType forString(String type) {
        return CACHE.computeIfAbsent(type, MimeParser::parseMimeType);
    }

}
