package org.jmouse.testing_ground.binder;

import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.Binder;
import org.jmouse.core.bind.DefaultBindingCallback;
import org.jmouse.core.bind.ImmutableBean;
import org.jmouse.core.bind.descriptor.TypeDescriptor;
import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.bean.PropertyDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.testing_ground.binder.dto.User;
import org.jmouse.testing_ground.binder.dto.UserService;
import org.jmouse.web.server.WebServerConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static void main(String[] args) {

        // public Factory<T> getFactory(Bindable<T> bindable) {
        // public Supplier<T> getInstance(Values values) {
        ImmutableBean<WebServerConfig> immutableBean = ImmutableBean.forClass(WebServerConfig.class);

        JavaType type = JavaType.forType(UserService.class);

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
//        data.put("books", books);
        data.put("address", addresses);

        Binder binder = Binder.with(data, new DefaultBindingCallback());
        User user = Bind.with(binder).get(null, User.class);

        JavaBeanDescriptor<User> descriptor = JavaBeanDescriptor.forBean(User.class);

        for (PropertyDescriptor<User> property : descriptor.getProperties()) {
            System.out.println(property);
        }

        TypeDescriptor.forClass(User.class);

        System.out.println(user);

    }


}
