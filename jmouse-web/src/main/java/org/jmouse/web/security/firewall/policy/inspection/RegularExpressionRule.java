package org.jmouse.web.security.firewall.policy.inspection;

import java.util.regex.Pattern;

record RegularExpressionRule(String id, Pattern pattern) implements InspectionRule {

    @Override
    public boolean test(String value) {
        return value != null && pattern.matcher(value).find();
    }

    @Override
    public String toString() {
        return "REGULAR_EXPRESSION[%s]: %s".formatted(id, pattern);
    }

}
