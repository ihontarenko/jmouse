package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;
import org.jmouse.el.renderable.Engine;
import org.jmouse.mvc.BeanConfigurer;
import org.jmouse.web.ViewResolver;
import org.jmouse.web.view.internal.InternalViewResolver;

@Bean
public class ViewResolverConfigurer implements BeanConfigurer<ViewResolver> {
    /**
     * 🔧 Apply additional configuration to the target object.
     *
     * @param object instance to initialize
     */
    @Override
    public void configure(ViewResolver object) {
        if (object instanceof InternalViewResolver internalViewResolver) {
            Engine engine = internalViewResolver.getEngine();
            engine.getExtensions().addFunction(new Function() {

                @Override
                public Object execute(Arguments arguments, EvaluationContext context) {
                    return getClass().getName();
                }

                @Override
                public String getName() {
                    return "hello";
                }
            });
        }
    }
}
