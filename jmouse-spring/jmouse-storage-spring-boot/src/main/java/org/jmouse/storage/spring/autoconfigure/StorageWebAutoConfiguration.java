package org.jmouse.storage.spring.autoconfigure;

import org.jmouse.storage.FileStore;
import org.jmouse.storage.spring.StorageProblemDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 🌐 The web-facing half: what a storage refusal answers with.
 *
 * <p>Separate from {@link StorageAutoConfiguration} because it is the only part of this starter that
 * requires a web application. A batch job or a worker uses the byte layer with no {@code spring-web}
 * on the classpath at all, and this configuration simply never applies there.</p>
 *
 * <p>⚠️ A product that already maps these exceptions keeps working — see
 * {@link StorageProblemDetails} for how precedence resolves and how to override one deliberately.</p>
 */
@AutoConfiguration(after = StorageAutoConfiguration.class)
@ConditionalOnClass({FileStore.class, ProblemDetail.class, RestControllerAdvice.class})
public class StorageWebAutoConfiguration {

    /**
     * 🚑 Storage refusals as RFC 7807 problem details.
     *
     * @return the advice
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageProblemDetails storageProblemDetails() {
        return new StorageProblemDetails();
    }
}
