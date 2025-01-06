package svit.scanner;

import java.util.Set;

public class ClassScannerExample {

    public static void main(String... arguments) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        ClassScanner        scanner          = new ClassScanner();
        JrtClassScanner       jrtClassScanner     = new JrtClassScanner("java.base");
        ResourcesClassScanner defaultClassScanner = new ResourcesClassScanner();

        defaultClassScanner.addScanner(new LocalClassScanner());
        defaultClassScanner.addScanner(new JarClassScanner());

        scanner.addScanner(jrtClassScanner);
        scanner.addScanner(defaultClassScanner);

//        scanner.addIncludeFilter(Class::isEnum);
//        scanner.addFilter(Class::isInterface);
//        scanner.addFilter(Class::isAnnotation);
//        scanner.addIncludeFilter(new AnnotationClassFilter(FunctionalInterface.class));
//        scanner.addFilter(new SubclassClassFilter(BeanDefinition.class));

//        Set<Class<?>> classes = scanner.scan("df.base.internal.application_context", loader);
        Set<Class<?>> classes = scanner.scan("df.common", loader);

        for (Class<?> type : classes) {
            System.out.println(type);
        }

        System.out.println("total classes found: " + classes.size());
/*
        FileScanner filesScanner = new FileScanner();

        filesScanner.addFilter(new IsRegularPathFilter());

        System.out.println(
                ClassLoader.getSystemClassLoader().getResource("db/")
        );

        Set<Path> paths = filesScanner.scan("db/", ClassLoader.getSystemClassLoader());

        for (Path path : paths) {
            System.out.println(path);
        }*/
    }

}
