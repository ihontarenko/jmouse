package svit.matcher.ant;

import svit.matcher.Matcher;

public class Example {

    public static void main(String[] args) {
        System.out.println(new AntMatcher("/org/slf4j/**/*.java").matches("/org/slf4j/Logger.class"));
        System.out.println(new AntMatcher("/org/slf4j/**").matches("/org/slf4j/test/Logger.class"));
        System.out.println(new AntMatcher("**/*.class").matches("/org/slf4j/test/Logger.class"));
    }

}
