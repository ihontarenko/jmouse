package org.jmouse.testing_ground.binder;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.ParameterDescriptor;
import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.testing_ground.binder.dto.Address;
import org.jmouse.testing_ground.binder.dto.PostalInfo;
import org.jmouse.testing_ground.binder.dto.User;

public class Example {

    public static void main(String[] args) throws NoSuchMethodException {

        // or Describer.of(User.class.getMethod("getName"))
        // or Describer.of(User.class.getField("name"))


        // java-bean type
        JavaBeanDescriptor<User> d1 = JavaBeanDescriptor.forBean(User.class);
        // value-object type
        JavaBeanDescriptor<Address> d2 = JavaBeanDescriptor.forBean(Address.class);
        // record value-object
        JavaBeanDescriptor<PostalInfo> d3 = JavaBeanDescriptor.forRecord(PostalInfo.class);

        System.out.println(d1);
        System.out.println(d2);
        System.out.println(d3);

        for (ParameterDescriptor parameter : d2.getConstructor(0).getParameters()) {
            System.out.println(parameter);
        }


/*        List<Map<String, Object>> addresses = new ArrayList<>();
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
        User user = Bind.with(binder).get(null, User.class);

        System.out.println(user);*/
    }


}
