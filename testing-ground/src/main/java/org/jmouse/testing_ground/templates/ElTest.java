package org.jmouse.testing_ground.templates;

import org.jmouse.el.core.StringSource;
import org.jmouse.el.core.lexer.TokenizableSource;

public class ElTest {

    public static void main(String[] args) {

        TokenizableSource string = new StringSource("string-test", "<h1>{% if x is even and y is not inset(1, 2, 3) || a > 1 and a < 10 and x is even or data.users[0].name is odd %}</h1>");

    }

}
