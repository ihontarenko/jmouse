package org.jmouse.testing_ground.binder;


import org.jmouse.JMouseModule;
import org.jmouse.core.bind.*;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.core.env.PropertyResolver;
import org.jmouse.core.env.*;
import org.jmouse.testing_ground.binder.dto.Address;
import org.jmouse.testing_ground.binder.dto.User;
import org.jmouse.util.helper.Strings;

import java.nio.file.FileSystems;
import java.util.*;

import static org.jmouse.util.helper.Files.removeExtension;

public class Example {

    public static void main(String[] args) {
        long time = System.currentTimeMillis();

        PropertyResolver resolver = createPropertyResolver();

        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        System.out.println("Resolved property resolver: " + resolver);
        PropertyValueAccessor dataSource = PropertyValueAccessor.wrap(resolver);
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        Map<String, Object> map = Map.of(
                "name", "Stephen King",
                "address", List.of(
                        Map.of(
                                "street", "Elm Street 666",
                                "city", "Castle Rock"
                        ),
                        Map.of(
                                "street", "Elm Street 777",
                                "city", "New York"
                        )
                )
        );

        PropertyValueAccessor mapAccessor = PropertyValueAccessor.wrap(map);

        Binder mapBinder = Binder.withValueAccessor(map);

        User user = Bind.with(mapBinder).to(User.class).getValue();
        Address address = Bind.with(mapBinder).to("address[0]", Address.class).getValue();

        User copyUser = Bind.with(user).to(User.class).getValue();

        PropertyValueAccessor accessor = PropertyValueAccessor.wrap(user);

        Binder binder = Binder.with(resolver, new DefaultBindingCallback());

        BindResult<Map<String, JMouseModule>> result = Bind.with(binder).toMap("package", JMouseModule.class);

        Bind.with(binder).to("default.webserver", WebServer.class);
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        System.out.println(result.getValue());

        Bind.with(binder).toMap("services", Object.class);
    }

    private static PropertyResolver createPropertyResolver() {

        PatternMatcherResourceLoader resourceLoader = new CompositeResourceLoader();

        MapPropertySource mapSource = new MapPropertySource("rawMaps", Map.of(
                "app.name", "jMouse Core v${package.core.version}",
                "app.full-name", "jMouse Framework",
                "app.java.version", "21",
                "app.description", "Application: '${app.name}' Java ${app.java.version} ${app.userName:Default}"
        ));

        PropertyResolver resolver = new StandardPropertyResolver();

        resolver.addPropertySource(mapSource);

        int counter = 0;
        for (Resource resource : resourceLoader.findResources("classpath:package.properties")) {
            String name      = Strings.suffix(resource.getName(), FileSystems.getDefault().getSeparator(), true, 1);
            String formatted = "%s[%d]".formatted(removeExtension(name), counter++);
            resolver.addPropertySource(new ResourcePropertySource(formatted, resource));
        }

        counter = 0;
        for (Resource resource : resourceLoader.findResources("classpath:webserver.properties")) {
            String name      = Strings.suffix(resource.getName(), FileSystems.getDefault().getSeparator(), true, 1);
            String formatted = "%s[%d]".formatted(removeExtension(name), counter++);
            resolver.addPropertySource(new ResourcePropertySource(formatted, resource));
        }

        return resolver;
    }



}
