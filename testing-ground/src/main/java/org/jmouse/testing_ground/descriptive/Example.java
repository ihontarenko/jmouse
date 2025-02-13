package org.jmouse.testing_ground.descriptive;

import org.jmouse.core.bind.PropertyValueAccessor;
import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.bind.descriptor.TypeDescriptor;
import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.bean.MapDescriptor;
import org.jmouse.core.bind.descriptor.bean.ObjectDescriptor;
import org.jmouse.testing_ground.beancontext.application.WideConstructorDto;
import org.jmouse.testing_ground.binder.WebServer;
import org.jmouse.web.server.WebServers;
import org.jmouse.testing_ground.beancontext.application.ExternalUser;
import org.jmouse.testing_ground.beancontext.application.InternalUser;
import org.jmouse.testing_ground.beancontext.application.UserHolder;

import java.util.HashMap;
import java.util.Map;

public class Example {

    public static void main(String... arguments) {
        JavaBean<InternalUser> bean = JavaBean.of(InternalUser.class);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "James");
        map.put("email", "james@binder.com");
        map.put("password", "password");


        TypeDescriptor descriptorA = TypeDescriptor.forClass(InternalUser.class);
        TypeDescriptor descriptorB = TypeDescriptor.forClass(ExternalUser.class);
        TypeDescriptor descriptorC = TypeDescriptor.forClass(WebServers.class);
        TypeDescriptor descriptorD = TypeDescriptor.forClass(WebServer.class);
        TypeDescriptor descriptorE = TypeDescriptor.forClass(UserHolder.class);

        ExternalUser externalUser = new ExternalUser();

        map.put("user", externalUser);

        MapDescriptor<String, Object> mapDescriptor = MapDescriptor.forMap(map);

        mapDescriptor.getPropertyAccessor("email").injectValue(map, "new@email.com");
        mapDescriptor.getProperty("name").getSetter().set(map, "newName");

        externalUser.setUser(new InternalUser());

        ObjectDescriptor<ExternalUser> descriptor = JavaBeanDescriptor.forBean(ExternalUser.class, externalUser);
        ObjectDescriptor<UserHolder> recordBean = JavaBeanDescriptor.forValueObject(UserHolder.class);

        recordBean.getProperty("user").getGetter();

        WideConstructorDto    dto    = new WideConstructorDto(123, "James");
        PropertyValueAccessor source = PropertyValueAccessor.wrap(dto);
        source.get("id").asObject(); // 123

        JavaBeanDescriptor<WideConstructorDto> dd = JavaBeanDescriptor.forBean(WideConstructorDto.class);

        System.out.println(recordBean);
    }

}
