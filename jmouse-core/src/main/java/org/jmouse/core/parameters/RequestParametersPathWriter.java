package org.jmouse.core.parameters;

import java.util.List;

/**
 * ✍️ A path written back from its tokens, in the one spelling {@link RequestParametersPathParser} reads it by: a property after a
 * dot ({@code papers.width}, bare when it comes first), an index in brackets ({@code papers[0]}), an append as empty brackets
 * ({@code groups[]}), and a property that holds a dot in brackets ({@code looks[pcb.look]}), so it is never taken for depth.
 *
 * <p>A property named by digits alone is written after a dot ({@code multipliers.0}): in brackets it would read as an index.
 * Parsing what this writes gives the same tokens back, so a path written here has one spelling only.</p>
 */
public final class RequestParametersPathWriter {

    /** The path the tokens make. */
    public String write(List<RequestParametersPathToken> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("A path holds at least one token.");
        }
        StringBuilder path = new StringBuilder();
        for (RequestParametersPathToken token : tokens) {
            switch (token) {
                case RequestParametersPathToken.PropertyNameToken(String propertyName) -> appendProperty(path, propertyName);
                case RequestParametersPathToken.ArrayIndexToken(int index) -> path.append('[').append(index).append(']');
                case RequestParametersPathToken.ArrayAppendToken ignored -> path.append("[]");
            }
        }
        return path.toString();
    }

    private static void appendProperty(StringBuilder path, String propertyName) {
        if (propertyName.isBlank()) {
            throw new IllegalArgumentException("A property of a path has a name.");
        }
        if (propertyName.indexOf('[') >= 0 || propertyName.indexOf(']') >= 0) {
            throw new IllegalArgumentException("A property of a path holds no bracket: '" + propertyName + "'");
        }
        if (propertyName.indexOf('.') >= 0) {
            // A dot inside keeps the brackets from reading it as an index: «1.5» is never all digits.
            path.append('[').append(propertyName).append(']');
            return;
        }
        if (!path.isEmpty()) {
            path.append('.');
        }
        path.append(propertyName);
    }
}
