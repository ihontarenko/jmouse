package org.jmouse.testing_ground.binder;

import org.jmouse.core.bind.*;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.jb.JavaBeanIntrospector;
import org.jmouse.testing_ground.binder.dto.*;
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

        Book book = new Book();

        book.setTitle("Book 2");
        book.setAuthor("John Doe");
        book.setPlaceCreation("New York");

        books.add(book);

        books.add(new BookImmutable("Title", "Stephen King", "Maine"));

        Map<String, Object> data = new HashMap<>();

        data.put("name", "James Bond");
        data.put("status", Map.of("status", "Blocked"));
        data.put("books", books);
        data.put("address", addresses);

        ObjectAccessorWrapper wrapper = new StandardAccessorWrapper();



        ObjectAccessor wrapped = wrapper.wrap(data);
        ObjectAccessor vo      = wrapper.wrap(new BookImmutable("Title", "Stephen King", "Maine"));

        wrapped.set("olo", "asd");

        wrapped.inject("books[1].title", "Book Injected");
        wrapped.inject("global", "Global Injected");

//        vo.get("title2");

        User   user   = Bind.with(data).get(User.class);

        ObjectAccessor accessor = wrapper.wrap(user);

        accessor.set("name", "John Doe");

        VirtualPropertyResolver resolver = VirtualPropertyResolver.defaultResolver();
        resolver.register(new UserMainAddressVirtualProperty());

        PropertyValueResolver valueResolver = new DefaultPropertyValueResolver(accessor, resolver);

        System.out.println(
                valueResolver.getProperty("name")
        );

        Object value = valueResolver.getProperty("mainAddress");

        ObjectDescriptor<User> descriptor = new JavaBeanIntrospector<>(User.class).introspect().toDescriptor();

        Setter<User, Object> setter = descriptor.getProperty("status").getSetter();

        setter.set(user, new UserStatus(Status.REGISTERED));

        accessor.navigate("books[0].placeCreation").asText();
        System.out.println(user);
    }


}
