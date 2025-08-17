package org.jmouse.web.mvc.method.converter;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.MediaType;

import java.util.Set;

@BeanFactories
public class JacksonObjectMapperConfiguration {

    @Bean
    public JacksonObjectMapperRegistration xmlRegistration() {
        return new JacksonObjectMapperRegistration(new XmlMapper(), Set.of(
                MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_ATOM_XML
        ));
    }

    @Bean
    public JacksonObjectMapperRegistration jsonRegistration() {
        return new JacksonObjectMapperRegistration(new JsonMapper(), Set.of(
                MediaType.APPLICATION_JSON
        ));
    }

    @Bean
    public JacksonObjectMapperRegistration yamlRegistration() {
        return new JacksonObjectMapperRegistration(new YAMLMapper(), Set.of(
                MediaType.APPLICATION_YAML
        ));
    }

}
