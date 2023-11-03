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

    public void runDemo() throws Exception {
        try {
            PipelineBuilder builder = PipelineBuilder.createBuilder();

            //URL url = PipelineDemo.class.getResource(PIPLINE_GRAPH);
            demoPipeline = builder.buildFromPathName(PIPLINE_GRAPH);

            StageCallback pre = (String s, Stage stage, ExecutionContext executionContext) -> {
                Logger logger = LogManager.getLogger(PipelineDemo.class);

                logger.info("Calling " + s + " ...");
            };

            StageCallback post = (String s, Stage stage, ExecutionContext executionContext) -> {
                Logger logger = LogManager.getLogger(PipelineDemo.class);
                logger.info("Called " + s + " with a status of: " + executionContext.getLastStageEvent(s).getEventType());
            };

            demoPipeline.registerPreStageCallback("stage 0", pre);
            demoPipeline.registerPreStageCallback("stage 1", pre);
            demoPipeline.registerPreStageCallback("stage 2", pre);
            demoPipeline.registerPreStageCallback("stage 3", pre);
            demoPipeline.registerPreStageCallback("stage 4", pre);
            demoPipeline.registerPreStageCallback("stage 5", pre);
            demoPipeline.registerPreStageCallback("stage 6", pre);
            demoPipeline.registerPreStageCallback("stage 7", pre);
            demoPipeline.registerPreStageCallback("stage 8", pre);
            demoPipeline.registerPreStageCallback("stage 9", pre);

            demoPipeline.registerPostStageCallback("stage 0", post);
            demoPipeline.registerPostStageCallback("stage 1", post);
            demoPipeline.registerPostStageCallback("stage 2", post);
            demoPipeline.registerPostStageCallback("stage 3", post);
            demoPipeline.registerPostStageCallback("stage 4", post);
            demoPipeline.registerPostStageCallback("stage 5", post);
            demoPipeline.registerPostStageCallback("stage 6", post);
            demoPipeline.registerPostStageCallback("stage 7", post);
            demoPipeline.registerPostStageCallback("stage 8", post);
            demoPipeline.registerPostStageCallback("stage 9", post);

            TimerContext context = new TimerContext();
            context.setMaxDelay(MAX_DELAY);
            Future<ExecutionContext> future = demoPipeline.runDetached(context);
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
