package svit.resolver;

public class ResolversFactory {

    public AbstractValueResolver createResolver(Resolvers resolverType) {
        return switch (resolverType) {
            case BASIC -> new BasicParameterResolver();
            case APACHE_SUBSTITUTE -> new SubstitutionParameterResolver();
            case EXPRESSION -> new ExpressionParameterResolver();
        };
    }

}
