package svit.reflection.scanner;

import java.sql.Connection;
import java.util.Map;

public class Example {

    public static void main(String... arguments) {
        ClassScanner scanner = new DefaultClassScanner();

        for (Class<?> aClass : scanner.scan(Example.class, Connection.class)) {
            System.out.println(aClass);
        }
    }

}
