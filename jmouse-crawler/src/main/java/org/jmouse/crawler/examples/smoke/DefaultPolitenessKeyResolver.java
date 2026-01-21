package org.jmouse.crawler.examples.smoke;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.examples.smoke.smoke2.VoronHint;
import org.jmouse.crawler.runtime.politeness.PolitenessKey;
import org.jmouse.crawler.runtime.politeness.PolitenessKeyResolver;

public final class DefaultPolitenessKeyResolver implements PolitenessKeyResolver<PolitenessKey> {

    @Override
    public PolitenessKey resolve(ProcessingTask task) {
        String host = task.url().getHost();

        String lane = switch (task.hint()) {
            case VoronHint.MEDIA -> "media";
            case VoronHint.LISTING -> "html";
            default -> "page";
        };

        return new PolitenessKey(lane, host);
    }
}
