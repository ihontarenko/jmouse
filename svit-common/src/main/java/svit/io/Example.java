package svit.io;

import java.io.IOException;

public class Example {

    public static void main(String[] args) throws IOException {

        ResourceLoader jarLoader = new JarURLResourceLoader();
        ResourceLoader fileSystemLoader = new FileSystemResourceLoader();
        ResourceLoader urlLoader = new URLResourceLoader();

        // jar check
        for (Resource r : jarLoader.loadResources("jar:META-INF/")) {
            System.out.println(r.getName());
            System.out.println("-".repeat(64));
        }

        for (Resource r : fileSystemLoader.loadResources("local:svit-framework/svit-common/src/main/resources/")) {
            System.out.println(r.getName());
        }

        System.out.println(
                new String(
                        fileSystemLoader.getResource("local:svit-framework/svit-common/src/main/resources/package.txt")
                                .getInputStream().readAllBytes()
                )
        );

        System.out.println(
                urlLoader.getClassLoader().getResource("svit")
        );

    }

}
