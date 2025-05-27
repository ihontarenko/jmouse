package org.jmouse.core;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Extension of {@link MimeType} that adds support for the HTTP "q" (quality) parameter.
 * <p>Quality factor allows ranking of media types during content negotiation.</p>
 */
public class MediaType extends MimeType {

    /** Parameter name for the quality factor (0.0 to 1.0). */
    public static final String PARAMETER_NAME_QUALITY_FACTOR = "q";

    private double qFactor;

    /**
     * Create a MediaType with wildcard subtype, default qFactor=1.0.
     *
     * @param type primary type (e.g. "application")
     */
    public MediaType(String type) {
        super(type);
    }

    /**
     * Create a MediaType with the specified type and subtype, default qFactor=1.0.
     *
     * @param type primary type
     * @param subtype subtype
     */
    public MediaType(String type, String subtype) {
        super(type, subtype);
    }

    /**
     * Create a MediaType with parameters, including optional 'q'.
     *
     * @param type primary type
     * @param subtype subtype
     * @param parameters map of parameters (may include 'q')
     */
    public MediaType(String type, String subtype, Map<String, String> parameters) {
        super(type, subtype, parameters);
    }

    /**
     * Copy constructor from generic MimeType; copies any parsed 'q' parameter.
     *
     * @param other the MimeType instance to copy
     */
    public MediaType(MimeType other) {
        super(other);
    }

    /**
     * Handle parameters during parsing. Parses 'q' into a quality factor.
     *
     * @param attribute parameter name
     * @param value parameter value
     * @throws InvalidMimeTypeException if q is outside [0.0,1.0]
     */
    @Override
    public void performParameters(String attribute, String value) {
        super.performParameters(attribute, value);
        if (PARAMETER_NAME_QUALITY_FACTOR.equals(attribute)) {
            double quality = Double.parseDouble(value);

            if (quality < 0.0D || quality > 1.0D) {
                throw new InvalidMimeTypeException(
                        null, "Quality factor must be between 0.0 and 1.0: was " + value
                );
            }

            qFactor = quality;
        }
    }

    /**
     * Sort a list of MediaType by their quality factor (ascending).
     *
     * @param mediaTypes list to sort
     */
    public static void sortByQFactor(List<MediaType> mediaTypes) {
        mediaTypes.sort(Comparator.comparingDouble(MediaType::getQFactor));
    }

    /**
     * @return the quality factor (0.0 to 1.0)
     */
    public double getQFactor() {
        return qFactor;
    }

    /**
     * Parse or retrieve a MediaType from cache, including quality parameter.
     *
     * @param type raw media type string
     * @return MediaType instance with parsed qFactor
     */
    public static MediaType forString(String type) {
        return new MediaType(MimeType.forString(type));
    }

}
