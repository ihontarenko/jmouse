package org.jmouse.mvc.negotiation;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.web.negotiation.QueryParameterLookup;

@Bean
public class QueryParameterLookupConfigurer
        extends MappingMediaTypeLookupConfigurer<QueryParameterLookup, QueryParameterLookupProperties> {

    @BeanConstructor
    public QueryParameterLookupConfigurer(QueryParameterLookupProperties properties) {
        super(properties);
    }

}
