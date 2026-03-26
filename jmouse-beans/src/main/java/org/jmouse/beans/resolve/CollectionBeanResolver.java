package org.jmouse.beans.resolve;

import org.jmouse.core.CollectionFactory;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves all beans assignable to a collection element type. 📚
 */
public class CollectionBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionContext context) {
        return BeanTypes.isCollection(context.type());
    }

    @Override
    public Object resolve(BeanResolutionContext context) {
        InferredType type         = InferredType.forType(context.type());
        Type         elementType  = BeanTypes.getGenericArgument(context.type());
        Class<?>     elementClass = BeanTypes.rawType(elementType);

        List<BeanCandidate> candidates = candidates(context).getCandidates(type.getFirst().getClassType());
        List<Object>        beans      = candidates.stream().map(BeanCandidate::bean).toList();

        Class<?> rawCollectionType = BeanTypes.rawType(context.type());

        if (List.class.isAssignableFrom(rawCollectionType)) {
            return beans;
        }

//        CollectionFactory.<Object, Object>createCollection(type.getClassType())

        if (LinkedHashSet.class.isAssignableFrom(rawCollectionType) || Set.class.isAssignableFrom(
                rawCollectionType)) {
            return new LinkedHashSet<>(beans);
        }

        if (Collection.class == rawCollectionType || rawCollectionType.isAssignableFrom(List.class)) {
            return beans;
        }

        throw new IllegalStateException("Unsupported collection type '%s'".formatted(rawCollectionType.getName()));
    }

}