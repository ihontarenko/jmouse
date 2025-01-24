package svit.io;

import svit.matcher.Matcher;
import svit.matcher.TextMatchers;

import java.io.IOException;
import java.util.Collection;

public class Example {

    public static void main(String[] args) throws IOException {

        ResourceLoader jarLoader = new JarURLResourceLoader();
        ResourceLoader fileSystemLoader = new FileSystemResourceLoader();
        ResourceLoader urlLoader = new NetworkURLResourceLoader();
        ResourceLoader classpathLoader = new ClasspathResourceLoader();

        Matcher<String> matcher = TextMatchers.ant("/ch/**Disallowed/*ss*/**");

        Collection<Resource> resources = classpathLoader.loadResources("classpath:ch", matcher);

        // jar check
        for (Resource r : resources) {
            System.out.println(r.getName());
        }

        System.out.println(resources.size());



//        for (Resource r : fileSystemLoader.loadResources("local:svit-framework/svit-common/src/main/resources/")) {
//            System.out.println(r.getName());
//        }
//
//        System.out.println(
//                new String(
//                        fileSystemLoader.getResource("local:svit-framework/svit-common/src/main/resources/META-INF/package.txt")
//                                .getInputStream().readAllBytes()
//                )
//        );
//
//        System.out.println(
//                urlLoader.getResource("https://uakino.me/robots.txt").asString()
//        );
//
//        Collection<Resource> cpr = new ClasspathResourceLoader().loadResources("classpath:META-INF/");
//
//        cpr.forEach(System.out::println);
    }

}
