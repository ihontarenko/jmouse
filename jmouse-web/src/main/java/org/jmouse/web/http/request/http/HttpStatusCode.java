package org.jmouse.web.http.request.http;

/**
 * Interface for HTTP status codes.
 */
public sealed interface HttpStatusCode permits HttpStatus {

    /**
     * Checks if the status code is in the 1xx range (Informational).
     *
     * @return true if the status code is 1xx, false otherwise
     */
    boolean is1xx();

    /**
     * Checks if the status code is in the 2xx range (Success).
     *
     * @return true if the status code is 2xx, false otherwise
     */
    boolean is2xx();

    /**
     * Checks if the status code is in the 3xx range (Redirection).
     *
     * @return true if the status code is 3xx, false otherwise
     */
    boolean is3xx();

    /**
     * Checks if the status code is in the 4xx range (Client Error).
     *
     * @return true if the status code is 4xx, false otherwise
     */
    boolean is4xx();

    /**
     * Checks if the status code is in the 5xx range (Server Error).
     *
     * @return true if the status code is 5xx, false otherwise
     */
    boolean is5xx();

    /**
     * Checks if the status code represents a success (2xx).
     *
     * @return true if the status code is in the success range, false otherwise
     */
    boolean isSuccess();

    /**
     * Checks if the status code represents an error (4xx or 5xx).
     *
     * @return true if the status code is in the error range, false otherwise
     */
    boolean isError();

    /**
     * Gets the numeric HTTP status code.
     *
     * @return the numeric code
     */
    int getCode();

    enum HttpStatusType {
        INFORMATIONAL,
        SUCCESS,
        REDIRECTION,
        CLIENT_ERROR,
        SERVER_ERROR
    }

}