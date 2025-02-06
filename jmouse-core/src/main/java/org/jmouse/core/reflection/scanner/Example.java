package org.jmouse.core.reflection.scanner;

import org.slf4j.Logger;

public class Example {

    public static void main(String... arguments) {
        ClassScanner scanner = new DefaultClassScanner();

        for (Class<?> aClass : scanner.scan(Example.class, Logger.class)) {
            System.out.println(aClass);
        }
    }

}
