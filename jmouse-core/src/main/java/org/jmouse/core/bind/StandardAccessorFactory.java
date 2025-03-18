package org.jmouse.core.bind;

import java.util.List;

public class StandardAccessorFactory extends BasicAccessorFactory {

    public StandardAccessorFactory() {
        this(List.of(
                new StandardTypesAccessorProvider(),
                new RecordObjectAccessorProvider(),
                new JavaBeanAccessorProvider(),
                new PropertyResolverAccessorProvider()
        ));
    }

    public StandardAccessorFactory(List<ObjectAccessorProvider> providers) {
        super(providers);
    }

}
