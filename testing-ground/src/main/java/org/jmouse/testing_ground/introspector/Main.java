package org.jmouse.testing_ground.introspector;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.descriptor.ClassTypeDescriptor;
import org.jmouse.core.access.descriptor.ClassTypeIntrospector;
import org.jmouse.core.access.descriptor.structured.bean.JavaBeanDescriptor;
import org.jmouse.core.access.descriptor.structured.bean.JavaBeanIntrospector;
import org.jmouse.core.access.descriptor.structured.map.MapDescriptor;
import org.jmouse.core.access.descriptor.structured.map.MapIntrospector;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectIntrospector;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.testing_ground.binder.dto.User;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String... arguments) {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(InferredType.forClass(User.class));
        introspector.introspect();
        ClassTypeDescriptor descriptor = introspector.toDescriptor();

        Map<Object, Object> data = new HashMap<>();

        PropertyPath propertyPath = PropertyPath.forPath("app.name[0].version");

        propertyPath.entries();

        data.put("firstName", "John");
        data.put(123, 123L);
        data.put(123D, 123F);
        MapDescriptor<Object, Object> mapDescriptor = new MapIntrospector<>(data).introspect().toDescriptor();

        JavaBeanIntrospector<User> javaBeanIntrospector = new JavaBeanIntrospector<>(User.class);

        javaBeanIntrospector.introspect();

        JavaBeanDescriptor<User> javaBeanDescriptor = javaBeanIntrospector.toDescriptor();

        javaBeanDescriptor.getProperties().forEach(
                (s, userPropertyDescriptor) -> System.out.println(userPropertyDescriptor));

        ValueObjectIntrospector<ClientUser> valueObjectIntrospector = new ValueObjectIntrospector<>(ClientUser.class);
        ValueObjectDescriptor<ClientUser> valueObjectDescriptor = valueObjectIntrospector.introspect().toDescriptor();
        ValueObjectDescriptor<UserAdmin> valueObjectDescriptorRecord = new ValueObjectIntrospector<>(UserAdmin.class).introspect().toDescriptor();

        System.out.println(descriptor);
    }

}
