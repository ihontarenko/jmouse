package org.jmouse.core;

import java.util.*;

import static org.jmouse.core.Charset.UTF_8;

/**
 * Extension of {@link MimeType} that adds support for the HTTP "q" (quality) parameter.
 * <p>Quality factor allows ranking of media types during content negotiation.</p>
 */
public class MediaType extends MimeType {

    /** Parameter name for the quality factor (0.0 to 1.0). */
    public static final String PARAMETER_NAME_QUALITY_FACTOR = "q";

    private double qFactor;

    public static final MediaType ALL;
    public static final MediaType ALL_APPLICATION;
    public static final MediaType APPLICATION_ATOM_XML;
    public static final MediaType APPLICATION_JSON;
    public static final MediaType JSON; // alias
    public static final MediaType APPLICATION_OCTET_STREAM;
    public static final MediaType APPLICATION_PDF;
    public static final MediaType APPLICATION_RSS_XML;
    public static final MediaType APPLICATION_XHTML_XML;
    public static final MediaType APPLICATION_XML;
    public static final MediaType XML; // alias
    public static final MediaType APPLICATION_FORM_URLENCODED;
    public static final MediaType MULTIPART_FORM_DATA;
    public static final MediaType APPLICATION_YAML;

    public static final MediaType TEXT_PLAIN;
    public static final MediaType TEXT; // alias
    public static final MediaType TEXT_HTML;
    public static final MediaType HTML; // alias
    public static final MediaType TEXT_XML;
    public static final MediaType TEXT_EVENT_STREAM;

    public static final MediaType IMAGE_GIF;
    public static final MediaType IMAGE_JPEG;
    public static final MediaType IMAGE_PNG;

    public static final String ALL_VALUE;
    public static final String ALL_APPLICATION_VALUE;

    public static final String APPLICATION_ATOM_XML_VALUE;
    public static final String APPLICATION_JSON_VALUE;
    public static final String APPLICATION_OCTET_STREAM_VALUE;
    public static final String APPLICATION_PDF_VALUE;
    public static final String APPLICATION_RSS_XML_VALUE;
    public static final String APPLICATION_XHTML_XML_VALUE;
    public static final String APPLICATION_XML_VALUE;
    public static final String APPLICATION_FORM_URLENCODED_VALUE;
    public static final String MULTIPART_FORM_DATA_VALUE;

    public static final String TEXT_PLAIN_VALUE;
    public static final String TEXT_VALUE;
    public static final String TEXT_HTML_VALUE;
    public static final String HTML_VALUE;
    public static final String TEXT_XML_VALUE;
    public static final String TEXT_EVENT_STREAM_VALUE;

    public static final String IMAGE_GIF_VALUE;
    public static final String IMAGE_JPEG_VALUE;
    public static final String IMAGE_PNG_VALUE;

    static {
        // Wildcard
        ALL = new MediaType("*", "*");
        ALL_VALUE = ALL.toString();

        // Wildcard
        ALL_APPLICATION = new MediaType("application", "*");
        ALL_APPLICATION_VALUE = ALL_APPLICATION.toString();

        // application/*
        APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
        APPLICATION_ATOM_XML_VALUE = APPLICATION_ATOM_XML.toString();

        APPLICATION_JSON = new MediaType("application", "json", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        JSON = APPLICATION_JSON;
        APPLICATION_JSON_VALUE = APPLICATION_JSON.toString();

        APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
        APPLICATION_OCTET_STREAM_VALUE = APPLICATION_OCTET_STREAM.toString();

        APPLICATION_PDF = new MediaType("application", "pdf");
        APPLICATION_PDF_VALUE = APPLICATION_PDF.toString();

        APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
        APPLICATION_RSS_XML_VALUE = APPLICATION_RSS_XML.toString();

        APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        APPLICATION_XHTML_XML_VALUE = APPLICATION_XHTML_XML.toString();

        APPLICATION_XML = new MediaType("application", "xml", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        XML = APPLICATION_XML;
        APPLICATION_XML_VALUE = APPLICATION_XML.toString();

        APPLICATION_YAML = new MediaType("application", "yaml", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));

        APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
        APPLICATION_FORM_URLENCODED_VALUE = APPLICATION_FORM_URLENCODED.toString();

        MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
        MULTIPART_FORM_DATA_VALUE = MULTIPART_FORM_DATA.toString();

        // text/*
        TEXT_PLAIN = new MediaType("text", "plain", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        TEXT = TEXT_PLAIN;
        TEXT_PLAIN_VALUE = TEXT_PLAIN.toString();
        TEXT_VALUE = TEXT_PLAIN_VALUE;

        TEXT_HTML = new MediaType("text", "html", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        HTML = TEXT_HTML;
        TEXT_HTML_VALUE = TEXT_HTML.toString();
        HTML_VALUE = TEXT_HTML_VALUE;

        TEXT_XML = new MediaType("text", "xml", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        TEXT_XML_VALUE = TEXT_XML.toString();

        TEXT_EVENT_STREAM = new MediaType("text", "event-stream", Map.of(PARAMETER_NAME_CHARSET, UTF_8.getName()));
        TEXT_EVENT_STREAM_VALUE = TEXT_EVENT_STREAM.toString();

        // images
        IMAGE_GIF = new MediaType("image", "gif");
        IMAGE_GIF_VALUE = IMAGE_GIF.toString();

        IMAGE_JPEG = new MediaType("image", "jpeg");
        IMAGE_JPEG_VALUE = IMAGE_JPEG.toString();

        IMAGE_PNG = new MediaType("image", "png");
        IMAGE_PNG_VALUE = IMAGE_PNG.toString();
    }

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
     * 🎯 Copy the quality factor ({@code q}) parameter from {@code source} into this media type.
     *
     * <p>If {@code source} contains a {@code q} parameter, returns a <b>new</b> {@link MediaType}
     * with the same type/subtype as this instance and the {@code q} copied from {@code source}.
     * If {@code q} is absent or {@code source} is {@code null}, returns {@code this} unchanged.</p>
     *
     * <ul>
     *   <li>Overwrites existing {@code q} on this instance.</li>
     *   <li>Does not validate the {@code q} value (caller ensures it is within {@code [0.0, 1.0]}).</li>
     * </ul>
     *
     * @param source media type to take the {@code q} value from (may be {@code null})
     * @return a new {@link MediaType} with the copied {@code q}, or {@code this} if not present
     */
    public MediaType copyQFactor(MediaType source) {
        return copyParameter(source, PARAMETER_NAME_QUALITY_FACTOR);
    }

    /**
     * 🔤 Copy the {@code charset} parameter from {@code source} into this media type.
     *
     * <p>If {@code source} contains a {@code charset} parameter, returns a <b>new</b> {@link MediaType}
     * with the same type/subtype as this instance and the {@code charset} copied from {@code source}.
     * If {@code charset} is absent or {@code source} is {@code null}, returns {@code this} unchanged.</p>
     *
     * <ul>
     *   <li>Overwrites existing {@code charset} on this instance.</li>
     *   <li>No validation of the charset name is performed here.</li>
     * </ul>
     *
     * @param source media type to take the {@code charset} from (may be {@code null})
     * @return a new {@link MediaType} with the copied {@code charset}, or {@code this} if not present
     */
    public MediaType copyCharset(MediaType source) {
        return copyParameter(source, PARAMETER_NAME_CHARSET);
    }

    /**
     * 📥 Copy an arbitrary parameter from {@code source} into this media type.
     *
     * <p>If {@code source} contains the given {@code parameter}, returns a <b>new</b> {@link MediaType}
     * with the same type/subtype as this instance and that parameter set to the source's value.
     * Otherwise (no such parameter or {@code source == null}) returns {@code this} unchanged.</p>
     *
     * <ul>
     *   <li>Overwrites the parameter if it already exists on this instance.</li>
     *   <li>Returns an immutable copy (this instance remains unchanged).</li>
     *   <li>No validation of the parameter value is performed.</li>
     * </ul>
     *
     * @param source    media type to read the parameter from (may be {@code null})
     * @param parameter parameter name to copy (must not be {@code null})
     * @return a new {@link MediaType} with the copied parameter, or {@code this} if absent
     */
    public MediaType copyParameter(MediaType source, String parameter) {
        if (source == null) {
            return this;
        }

        String charset = source.getParameter(parameter);

        if (charset != null) {
            Map<String, String> parameters = new LinkedHashMap<>(this.getParameters());
            parameters.put(parameter, charset);
            return new MediaType(this.getType(), this.getSubType(), parameters);
        }

        return this;
    }

    /**
     * Parse or retrieve a MediaType from cache, including quality parameter.
     *
     * @param type raw media type string
     * @return MediaType instance with parsed qFactor
     */
    public static MediaType forString(String type) {
        MediaType mediaType = null;

        if (type != null) {
            mediaType = new MediaType(MimeType.forString(type));
        }

        return mediaType;
    }

    /**
     * 📊 Sort media types by preference.
     *
     * <p>Orders the list by ascending <em>q-factor</em> (quality value), then by
     * lexical order of their string representation to ensure deterministic results.</p>
     *
     * @param mediaTypes the list of media types to sort (not modified)
     * @return a new list containing the same items in prioritized order
     */
    public static List<MediaType> prioritize(List<MediaType> mediaTypes) {
        List<MediaType>       prioritized = new ArrayList<>(mediaTypes);
        Comparator<MediaType> comparator  = Comparator.comparingDouble(MediaType::getQFactor)
                .thenComparing(Comparator.comparing(MediaType::toString));

        prioritized.sort(comparator);

        return prioritized;
    }

    /**
     * 🤝 Compute the intersection of two media type lists, respecting priority.
     *
     * <p>Both lists are first prioritized via {@link #prioritize(List)}. Then, for each
     * item in the first list, a compatible item in the second list (per
     * {@link MimeType#includes(MediaType)}) adds that media type to the result.</p>
     *
     * <p><b>Note:</b> A match contributes the entry from <em>listA</em>.</p>
     *
     * @param listA the first list of media types
     * @param listB the second list of media types
     * @return a new list containing compatible media types from {@code listA}
     */
    public static List<MediaType> intersect(List<MediaType> listA, List<MediaType> listB) {
        Set<MediaType> result = new LinkedHashSet<>();

        listA = prioritize(listA);
        listB = prioritize(listB);

        for (MediaType typeA : listA) {
            for (MediaType typeB : listB) {
                if (typeB.compatible(typeA)) {
                    result.add(new MediaType(MediaType.getMoreSpecific(typeA, typeB)));
                }
            }
        }

        return List.copyOf(result);
    }

    /**
     * ➖ Compute the multiset difference between two media type lists.
     *
     * <p>Returns all elements from both lists minus their {@link #intersect(List, List) intersection}.
     * The result preserves duplicates only to the extent they exist outside the intersection.</p>
     *
     * @param listA the first list
     * @param listB the second list
     * @return a new list representing {@code (listA ∪ listB) \ intersect(listA, listB)}
     */
    public static List<MediaType> difference(List<MediaType> listA, List<MediaType> listB) {
        Set<MediaType> result = new LinkedHashSet<>(listA.size() + listB.size());

        result.addAll(listA);
        result.addAll(listB);

        intersect(listA, listB).forEach(result::remove);

        return List.copyOf(result);
    }

}
