package org.dplevine.patterns.demo.pipelineDemo;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dplevine.patterns.pipeline.*;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.Future;


public class PipelineDemo implements Stage, StageBuilder {

    static final String PIPLINE_GRAPH = "/tmp/pipelineDemo.json";
    static final Long MAX_DELAY = 10L;
    private Pipeline demoPipeline;

    public static class TimerContext extends ExecutionContext {
        public static final String MAX_DELAY_SECONDS = "MaxDelay";

        public TimerContext() {
            super();
        }

        public Long getMaxDelay() {
            return (Long) getObject(MAX_DELAY_SECONDS);
        }

        public void setMaxDelay(Long maxDelay) {
            addObject(MAX_DELAY_SECONDS, maxDelay);
        }
    }

    public ExecutionContext doWork(ExecutionContext context) throws Exception {
        PipelineDemo.TimerContext timerContext = (PipelineDemo.TimerContext) context;
        long delay = Math.round(timerContext.getMaxDelay() * 1000 * Math.random());
        Thread.sleep(Math.round(delay));

        if (Math.round(Math.random()) == 1) {
            throw new Exception("simulated exception");
        }
        return context;
    }

    @Override public Stage buildStage() {
        return new PipelineDemo();
    }

    public void runDemo(boolean fastFail) throws Exception {
        try {
            PipelineBuilder builder = PipelineBuilder.createBuilder();
            demoPipeline = builder.buildFromPathName(PIPLINE_GRAPH);

            TimerContext context = new TimerContext();
            context.setMaxDelay(MAX_DELAY);
            /* Future<ExecutionContext> future = */ demoPipeline.runDetached(context, fastFail);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String getLogs() {
        if (demoPipeline != null) {
            return demoPipeline.getContext().getEventLog().toString();
        }
        return "";
    }

    public BufferedImage getImage() throws Exception {
            return demoPipeline.render();
    }
}
