package org.jmouse.testing_ground.binder;

import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.Binder;
import org.jmouse.core.bind.DefaultBindingCallback;
import org.jmouse.testing_ground.binder.dto.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static void main(String[] args) {



        List<Map<String, Object>> addresses = new ArrayList<>();
        addresses.add(Map.of("street", "Street 123 ${address[0].city}", "city", "New-York"));

        List<Map<String, Object>> books = new ArrayList<>();
        books.add(Map.of(
                "title", "Book 1",
                "author", "${name}",
                "placeCreation", "Written on ${address[0].city:Unknown} city with love: ${name}"
        ));

        Map<String, Object> data = new HashMap<>();

        data.put("name", "James Bond");
        data.put("books", books);
        data.put("address", addresses);

        Binder binder = Binder.with(data, new DefaultBindingCallback());
        User   user   = Bind.with(binder).get(null, User.class);

        System.out.println(user);
    }


}
