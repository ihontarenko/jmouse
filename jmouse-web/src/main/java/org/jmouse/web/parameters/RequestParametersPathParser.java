package org.jmouse.web.parameters;

import java.util.ArrayList;
import java.util.List;

public final class RequestParametersPathParser {

    public List<RequestParametersPathToken> parse(String rawParameterName) {
        if (rawParameterName == null || rawParameterName.isBlank()) {
            return List.of();
        }

        List<RequestParametersPathToken> tokens = new ArrayList<>();
        StringBuilder                    buffer = new StringBuilder();
        int                              index  = 0;

        while (index < rawParameterName.length()) {
            char character = rawParameterName.charAt(index);

            if (character == '.') {
                flushPropertyName(buffer, tokens);
                index++;
                continue;
            }

            if (character == '[') {
                flushPropertyName(buffer, tokens);

                int closingBracketIndex = rawParameterName.indexOf(']', index);
                if (closingBracketIndex < 0) {
                    // Malformed, treat '[' as literal
                    buffer.append(character);
                    index++;
                    continue;
                }

                String inside = rawParameterName.substring(index + 1, closingBracketIndex);

                if (inside.isEmpty()) {
                    tokens.add(RequestParametersPathToken.ArrayAppendToken.instance());
                } else if (isAllDigits(inside)) {
                    tokens.add(new RequestParametersPathToken.ArrayIndexToken(parseInt(inside)));
                } else {
                    tokens.add(new RequestParametersPathToken.PropertyNameToken(inside));
                }

                index = closingBracketIndex + 1;
                continue;
            }

            buffer.append(character);
            index++;
        }

        flushPropertyName(buffer, tokens);
        return tokens;
    }

    private static void flushPropertyName(StringBuilder buffer, List<RequestParametersPathToken> tokens) {
        if (buffer.isEmpty()) {
            return;
        }

        String name = buffer.toString();
        buffer.setLength(0);

        if (!name.isBlank()) {
            tokens.add(new RequestParametersPathToken.PropertyNameToken(name));
        }
    }

    private static boolean isAllDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return !text.isEmpty();
    }

    private static int parseInt(String digits) {
        int value = 0;

        for (int i = 0; i < digits.length(); i++) {
            value = value * 10 + (digits.charAt(i) - '0');
        }

        return value;
    }
}
