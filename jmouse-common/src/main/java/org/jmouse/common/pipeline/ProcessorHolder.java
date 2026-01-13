package org.jmouse.common.pipeline;

public interface ProcessorHolder {

    PipelineProcessor getProcessor();

    void setTransition(String returnCode, String next);

    String getTransition(String returnCode);

}
