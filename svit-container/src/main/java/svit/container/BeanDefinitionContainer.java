package svit.container;

import svit.container.definition.BeanDefinition;

public interface BeanDefinitionContainer {

    void registerDefinition(BeanDefinition definition);

    BeanDefinition getDefinition(String name);

}
