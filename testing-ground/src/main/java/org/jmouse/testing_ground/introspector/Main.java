package org.jmouse.testing_ground.introspector;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeIntrospector;
import org.jmouse.core.bind.introspection.Describer;
import org.jmouse.core.bind.introspection.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.introspection.bean.JavaBeanIntrospector;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.testing_ground.binder.dto.User;

public class Main {

    public static void main(String... arguments) {

        Describer.className(Main.class);

        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forClass(User.class));
        introspector.introspect();
        ClassTypeDescriptor descriptor = introspector.toDescriptor();

        JavaBeanIntrospector<User> javaBeanIntrospector = new JavaBeanIntrospector<>(User.class);

        javaBeanIntrospector.introspect();

        JavaBeanDescriptor<User> javaBeanDescriptor = javaBeanIntrospector.toDescriptor();

        System.out.println(descriptor);
        System.out.println(javaBeanDescriptor.isArray());

    }

}
