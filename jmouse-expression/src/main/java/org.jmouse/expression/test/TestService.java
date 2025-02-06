package svit.expression.test;

import java.util.Random;

public class TestService {

    public String test;

    public String getValue(int value, String name) {
        return getClass().getName();
    }

    public static int random() {
        return new Random().nextInt();
    }

    public String hello(int random, String var, double value) {
        return "%s - %s - %s".formatted(random, var, value);
    }

    public int sum(int a, int b) {
        return a + b;
    }

}
