package org.jmouse.java.parser.source;

/**
 * Why part of a source could not be read as written.
 */
public enum ProblemKind {

    /** A block or documentation comment reaches the end of the text without its closing delimiter. */
    UNTERMINATED_COMMENT,

    /** A string literal reaches the end of its line without a closing quote. */
    UNTERMINATED_STRING,

    /** A character literal reaches the end of its line without a closing quote. */
    UNTERMINATED_CHARACTER,

    /** A text block reaches the end of the text without its closing delimiter. */
    UNTERMINATED_TEXT_BLOCK,

    /** A text block's opening delimiter is followed by something other than a line terminator. */
    MALFORMED_TEXT_BLOCK,

    /** A backslash-{@code u} sequence is not followed by four hexadecimal digits. */
    INVALID_UNICODE_ESCAPE,

    /** A character that starts no Java token. */
    UNEXPECTED_CHARACTER,

    /** A token where the declaration grammar allows none of its kind. */
    UNEXPECTED_TOKEN,

    /** An opening parenthesis, bracket or brace that is never closed. */
    UNBALANCED_DELIMITER,

    /** The text ends inside a declaration. */
    UNEXPECTED_END_OF_FILE,

    /** Type declarations nested deeper than the parser follows. */
    NESTING_TOO_DEEP
}
