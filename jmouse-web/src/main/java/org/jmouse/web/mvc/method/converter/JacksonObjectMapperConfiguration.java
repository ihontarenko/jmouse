package org.jmouse.web.mvc.method.converter;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.jmouse.beans.Beans;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.MediaType;

import java.util.List;
import java.util.Set;

/**
 * ⚙️ Auto-configuration for Jackson {@link com.fasterxml.jackson.databind.ObjectMapper}
 * instances across supported content types.
 * <p>
 * Declares and registers {@link JacksonObjectMapperRegistration} beans for:
 * <ul>
 *     <li>📄 JSON — via {@link JsonMapper}</li>
 *     <li>📘 XML — via {@link XmlMapper}</li>
 *     <li>📒 YAML — via {@link YAMLMapper}</li>
 * </ul>
 * Each registration associates its mapperProvider with one or more {@link MediaType}s,
 * allowing {@link JacksonObjectMapperResolver} to dynamically select the appropriate
 * mapperProvider based on the content type.
 * </p>
 */
@BeanFactories
public class JacksonObjectMapperConfiguration {

    /**
     * 📘 XML mapperProvider registration supporting {@code application/xml},
     * {@code text/xml}, and {@code application/atom+xml}.
     *
     * @return XML mapperProvider registration bean
     */
    @Bean
    public JacksonObjectMapperRegistration xmlRegistration() {
        return new JacksonObjectMapperRegistration(new XmlMapper(), Set.of(
                MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_ATOM_XML
        ));
    }

    /**
     * 📄 JSON mapperProvider registration for {@code application/json}.
     *
     * @return JSON mapperProvider registration bean
     */
    @Bean
    public JacksonObjectMapperRegistration jsonRegistration() {
        return new JacksonObjectMapperRegistration(new JsonMapper(), Set.of(
                MediaType.APPLICATION_JSON
        ));
    }

    /**
     * 📒 YAML mapperProvider registration for {@code application/yaml}.
     *
     * @return YAML mapperProvider registration bean
     */
    @Bean
    public JacksonObjectMapperRegistration yamlRegistration() {
        return new JacksonObjectMapperRegistration(new YAMLMapper(), Set.of(
                MediaType.APPLICATION_YAML
        ));
    }

    /**
     * 🧩 Aggregates all {@link JacksonObjectMapperRegistration} beans into a single
     * {@link JacksonObjectMapperResolver}.
     * <p>
     * This resolver enables runtime lookup of {@link com.fasterxml.jackson.databind.ObjectMapper}
     * instances based on {@link MediaType}.
     * </p>
     *
     * @param registrationBeans aggregated mapperProvider registration beans
     * @return unified Jackson object mapperProvider resolver
     */
    @Bean
    public JacksonObjectMapperResolver jacksonObjectMapperResolver(
            @AggregatedBeans Beans<JacksonObjectMapperRegistration> registrationBeans) {
        return new JacksonObjectMapperResolver(List.copyOf(registrationBeans.getBeans()));
    }
}
