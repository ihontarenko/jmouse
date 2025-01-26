package svit.reflection.scanner;

import org.slf4j.Logger;

import java.util.List;

public class Example {

    public static void main(String... arguments) {
        ClassScanner scanner = new DefaultClassScanner();

        for (Class<?> aClass : scanner.scan(Example.class, Logger.class)) {
            System.out.println(aClass);
        }
    }

}
