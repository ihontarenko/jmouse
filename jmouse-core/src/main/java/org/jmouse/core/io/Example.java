package org.jmouse.core.io;

import java.io.IOException;

public class Example {

    public static void main(String[] args) throws IOException {
        PatternMatcherResourceLoader loader = new CompositeResourceLoader();
        for (Resource resource : loader.findResources("classpath:ch/**/*.class")) {
            System.out.println(resource);
        }
    }

}
