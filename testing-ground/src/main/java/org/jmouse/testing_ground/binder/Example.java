package org.jmouse.testing_ground.binder;

import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.Binder;
import org.jmouse.core.bind.DefaultBindingCallback;
import org.jmouse.core.bind.PropertyValuesAccessor;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.jb.JavaBeanIntrospector;
import org.jmouse.testing_ground.binder.dto.*;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static void main(String[] args) {

        List<Map<String, Object>> addresses = new ArrayList<>();
        addresses.add(Map.of("street", "Street 123 ${address[0].city}", "city", "New-York"));

        List<Object> books = new ArrayList<>();
        books.add(Map.of(
                "title", "Book 1",
                "author", "${name}",
                "placeCreation", "Written on ${address[0].city:Unknown} city with love: ${name}"
        ));
        books.add(new Book(){{
            setTitle("Book 2");
            setAuthor("John Doe");
            setPlaceCreation("New York");
        }});

        books.add(new BookImmutable("Title", "Stephen King", "Maine"));

        Map<String, Object> data = new HashMap<>();

        data.put("name", "James Bond");
        data.put("status", Map.of("status", "Blocked"));
        data.put("books", books);
        data.put("address", addresses);

        PropertyValuesAccessor wrapped = PropertyValuesAccessor.wrap(data);
        PropertyValuesAccessor vo = PropertyValuesAccessor.wrap(new BookImmutable("Title", "Stephen King", "Maine"));

        vo.get("title");

        Binder binder = Binder.with(data, new DefaultBindingCallback());
        User   user   = Bind.with(binder).get(User.class);

        PropertyValuesAccessor accessor = PropertyValuesAccessor.wrap(user);

        ObjectDescriptor<User> descriptor = new JavaBeanIntrospector<>(User.class).introspect().toDescriptor();

        Getter<User, Object> getter = descriptor.getProperty("status").getGetter();
        Setter<User, Object> setter = descriptor.getProperty("status").getSetter();

        setter.set(user, new UserStatus(Status.REGISTERED));

        accessor.navigate("books[0].placeCreation").asText();
        System.out.println(user);
    }


}
