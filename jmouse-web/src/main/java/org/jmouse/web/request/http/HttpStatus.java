package org.jmouse.web.request.http;

/**
 * Enum representing HTTP status codes with a brief text description and category.
 */
public enum HttpStatus implements HttpStatusCode {

    CONTINUE("Continue", 100, HttpStatusType.INFORMATIONAL),
    SWITCHING_PROTOCOLS("Switching Protocols", 101, HttpStatusType.INFORMATIONAL),
    PROCESSING("Processing", 102, HttpStatusType.INFORMATIONAL),
    EARLY_HINTS("Early Hints", 103, HttpStatusType.INFORMATIONAL),

    OK("OK", 200, HttpStatusType.SUCCESS),
    CREATED("Created", 201, HttpStatusType.SUCCESS),
    ACCEPTED("Accepted", 202, HttpStatusType.SUCCESS),
    NON_AUTHORITATIVE_INFORMATION("Non-Authoritative Information", 203, HttpStatusType.SUCCESS),
    NO_CONTENT("No Content", 204, HttpStatusType.SUCCESS),
    RESET_CONTENT("Reset Content", 205, HttpStatusType.SUCCESS),
    PARTIAL_CONTENT("Partial Content", 206, HttpStatusType.SUCCESS),
    MULTI_STATUS("Multi-Status", 207, HttpStatusType.SUCCESS),
    ALREADY_REPORTED("Already Reported", 208, HttpStatusType.SUCCESS),
    IM_USED("IM Used", 226, HttpStatusType.SUCCESS),

    MULTIPLE_CHOICES("Multiple Choices", 300, HttpStatusType.REDIRECTION),
    MOVED_PERMANENTLY("Moved Permanently", 301, HttpStatusType.REDIRECTION),
    FOUND("Found", 302, HttpStatusType.REDIRECTION),
    SEE_OTHER("See Other", 303, HttpStatusType.REDIRECTION),
    NOT_MODIFIED("Not Modified", 304, HttpStatusType.REDIRECTION),
    USE_PROXY("Use Proxy", 305, HttpStatusType.REDIRECTION),
    TEMPORARY_REDIRECT("Temporary Redirect", 307, HttpStatusType.REDIRECTION),
    PERMANENT_REDIRECT("Permanent Redirect", 308, HttpStatusType.REDIRECTION),

    BAD_REQUEST("Bad Request", 400, HttpStatusType.CLIENT_ERROR),
    UNAUTHORIZED("Unauthorized", 401, HttpStatusType.CLIENT_ERROR),
    PAYMENT_REQUIRED("Payment Required", 402, HttpStatusType.CLIENT_ERROR),
    FORBIDDEN("Forbidden", 403, HttpStatusType.CLIENT_ERROR),
    NOT_FOUND("Not Found", 404, HttpStatusType.CLIENT_ERROR),
    METHOD_NOT_ALLOWED("Method Not Allowed", 405, HttpStatusType.CLIENT_ERROR),
    NOT_ACCEPTABLE("Not Acceptable", 406, HttpStatusType.CLIENT_ERROR),
    PROXY_AUTHENTICATION_REQUIRED("Proxy Authentication Required", 407, HttpStatusType.CLIENT_ERROR),
    REQUEST_TIMEOUT("Request Timeout", 408, HttpStatusType.CLIENT_ERROR),
    CONFLICT("Conflict", 409, HttpStatusType.CLIENT_ERROR),
    GONE("Gone", 410, HttpStatusType.CLIENT_ERROR),
    LENGTH_REQUIRED("Length Required", 411, HttpStatusType.CLIENT_ERROR),
    PRECONDITION_FAILED("Precondition Failed", 412, HttpStatusType.CLIENT_ERROR),
    PAYLOAD_TOO_LARGE("Payload Too Large", 413, HttpStatusType.CLIENT_ERROR),
    URI_TOO_LONG("URI Too Long", 414, HttpStatusType.CLIENT_ERROR),
    UNSUPPORTED_MEDIA_TYPE("Unsupported Media Type", 415, HttpStatusType.CLIENT_ERROR),
    RANGE_NOT_SATISFIABLE("Range Not Satisfiable", 416, HttpStatusType.CLIENT_ERROR),
    EXPECTATION_FAILED("Expectation Failed", 417, HttpStatusType.CLIENT_ERROR),
    IM_A_TEAPOT("I'm a teapot", 418, HttpStatusType.CLIENT_ERROR),
    MISDIRECTED_REQUEST("Misdirected Request", 421, HttpStatusType.CLIENT_ERROR),
    UNPROCESSABLE_ENTITY("Unprocessable Entity", 422, HttpStatusType.CLIENT_ERROR),
    LOCKED("Locked", 423, HttpStatusType.CLIENT_ERROR),
    FAILED_DEPENDENCY("Failed Dependency", 424, HttpStatusType.CLIENT_ERROR),
    TOO_EARLY("Too Early", 425, HttpStatusType.CLIENT_ERROR),
    UPGRADE_REQUIRED("Upgrade Required", 426, HttpStatusType.CLIENT_ERROR),
    PRECONDITION_REQUIRED("Precondition Required", 428, HttpStatusType.CLIENT_ERROR),
    TOO_MANY_REQUESTS("Too Many Requests", 429, HttpStatusType.CLIENT_ERROR),
    REQUEST_HEADER_FIELDS_TOO_LARGE("Request Header Fields Too Large", 431, HttpStatusType.CLIENT_ERROR),
    UNAVAILABLE_FOR_LEGAL_REASONS("Unavailable For Legal Reasons", 451, HttpStatusType.CLIENT_ERROR),

    INTERNAL_SERVER_ERROR("Internal Server Error", 500, HttpStatusType.SERVER_ERROR),
    NOT_IMPLEMENTED("Not Implemented", 501, HttpStatusType.SERVER_ERROR),
    BAD_GATEWAY("Bad Gateway", 502, HttpStatusType.SERVER_ERROR),
    SERVICE_UNAVAILABLE("Service Unavailable", 503, HttpStatusType.SERVER_ERROR),
    GATEWAY_TIMEOUT("Gateway Timeout", 504, HttpStatusType.SERVER_ERROR),
    HTTP_VERSION_NOT_SUPPORTED("HTTP Version Not Supported", 505, HttpStatusType.SERVER_ERROR),
    VARIANT_ALSO_NEGOTIATES("Variant Also Negotiates", 506, HttpStatusType.SERVER_ERROR),
    INSUFFICIENT_STORAGE("Insufficient Storage", 507, HttpStatusType.SERVER_ERROR),
    LOOP_DETECTED("Loop Detected", 508, HttpStatusType.SERVER_ERROR),
    NOT_EXTENDED("Not Extended", 510, HttpStatusType.SERVER_ERROR),
    NETWORK_AUTHENTICATION_REQUIRED("Network Authentication Required", 511, HttpStatusType.SERVER_ERROR);

    private final String         text;
    private final int            code;
    private final HttpStatusType type;

    /**
     * Constructs an HttpStatus with the given text, code, and type.
     *
     * @param text the short text for the status
     * @param code the numeric HTTP status code
     * @param type the type of HTTP status
     */
    HttpStatus(String text, int code, HttpStatusType type) {
        this.text = text;
        this.code = code;
        this.type = type;
    }

    /**
     * Gets the short text of the HTTP status.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the numeric HTTP status code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the type of the HTTP status.
     *
     * @return the type
     */
    public HttpStatusType getType() {
        return type;
    }

    @Override
    public boolean is1xx() {
        return code >= 100 && code < 200;
    }

    @Override
    public boolean is2xx() {
        return code >= 200 && code < 300;
    }

    @Override
    public boolean is3xx() {
        return code >= 300 && code < 400;
    }

    @Override
    public boolean is4xx() {
        return code >= 400 && code < 500;
    }

    @Override
    public boolean is5xx() {
        return code >= 500 && code < 600;
    }

    @Override
    public boolean isSuccess() {
        return is2xx();
    }

    @Override
    public boolean isError() {
        return is4xx() || is5xx();
    }

    @Override
    public String toString() {
        return "%s[%d]: %s".formatted(getType(), getCode(), getText());
    }

    public static HttpStatus ofCode(int code) {
        HttpStatus status = null;

        for (HttpStatus candidate : values()) {
            if (candidate.code == code) {
                status = candidate;
                break;
            }
        }

        return status;
    }


}