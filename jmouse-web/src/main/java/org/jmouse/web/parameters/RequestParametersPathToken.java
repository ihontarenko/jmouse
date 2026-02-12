package org.jmouse.web.parameters;

public sealed interface RequestParametersPathToken
        permits RequestParametersPathToken.PropertyNameToken,
                RequestParametersPathToken.ArrayIndexToken,
                RequestParametersPathToken.ArrayAppendToken {

    record PropertyNameToken(String propertyName) implements RequestParametersPathToken {}

    record ArrayIndexToken(int index) implements RequestParametersPathToken {}

    final class ArrayAppendToken implements RequestParametersPathToken {
        private static final ArrayAppendToken INSTANCE = new ArrayAppendToken();
        private ArrayAppendToken() {}
        public static ArrayAppendToken instance() { return INSTANCE; }
    }

}
