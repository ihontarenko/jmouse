package org.jmouse.core.bind;

import java.util.List;

public class StandardAccessorWrapper extends BasicAccessorWrapper {

    public StandardAccessorWrapper() {
        this(List.of(
                new StandardTypesAccessorProvider(),
                new JavaBeanAccessorProvider(),
                new RecordAccessorProvider(),
                new PropertyResolverAccessorProvider()
        ));
    }

    public StandardAccessorWrapper(List<ObjectAccessorProvider> providers) {
        super(providers);
    }

}
