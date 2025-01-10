package svit.container.example;

import svit.container.annotation.Dependency;
import svit.container.annotation.BeanInitializer;

public class ExternalUser implements User {

    @Dependency("gal")
    private String name;

    @Dependency("int123")
    private Integer count;

    @BeanInitializer
    public void init() {
        System.out.println("Initialization method");
    }

    @Override
    public String getName() {
        return name;
    }

}
