package org.jmouse.pipeline.context;

import org.jmouse.core.context.ProcessingControl;
import org.jmouse.core.context.beans.BeanLookup;
import org.jmouse.core.context.mutable.MutableArgumentsContext;
import org.jmouse.core.context.mutable.MutableKeyValueContext;
import org.jmouse.core.context.result.MutableResultContext;

public interface PipelineContext extends MutableArgumentsContext, ProcessingControl, MutableKeyValueContext {

    BeanLookup getBeanLookup();

    MutableArgumentsContext getArgumentsContext();

    MutableResultContext getResultContext();

}
