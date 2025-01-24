package svit.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.matcher.Matcher;
import svit.matcher.ant.AntMatcher;
import svit.util.Files;

import java.util.Collection;

import static svit.matcher.TextMatchers.ant;
import static svit.matcher.TextMatchers.endsWith;

public interface PatternMatcherResourceLoader extends ResourceLoader {

    Logger LOGGER = LoggerFactory.getLogger(PatternMatcherResourceLoader.class);

    default Collection<Resource> findResources(String path) {
        String protocol = Files.extractProtocol(path, Resource.CLASSPATH_PROTOCOL);
        String location = path.substring(0, path.indexOf(AntMatcher.ANY_SINGLE_SEGMENT) - 1);
        String pattern  = Files.removeProtocol(path);

        Matcher<String> ant = ant("**/" + pattern);

        ant = ant.and(endsWith("class")).and(endsWith("module-info.class").not());

        LOGGER.info("Protocol: {}, Location: {}, {}", protocol, location, ant);
        Collection<Resource> resources = loadResources(location, ant);
        LOGGER.info("Found {} resources", resources.size());

        return resources;
    }

}
