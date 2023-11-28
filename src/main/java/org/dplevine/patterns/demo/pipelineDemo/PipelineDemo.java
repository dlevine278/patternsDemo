package org.dplevine.patterns.demo.pipelineDemo;

import org.dplevine.patterns.pipeline.*;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;

@StageBuilderDefinition(id = "stage 0")
@StageBuilderDefinition(id = "stage 1")
@StageBuilderDefinition(id = "stage 2")
@StageBuilderDefinition(id = "stage 3")
@StageBuilderDefinition(id = "stage 4")
@StageBuilderDefinition(id = "stage 5")
@StageBuilderDefinition(id = "stage 6")
@StageBuilderDefinition(id = "stage 7")
@StageBuilderDefinition(id = "stage 8")
@StageBuilderDefinition(id = "stage 9")
@PipelineStepsDefinition(pipelineRootId = "Demo Pipeline", parallelId = "parallel 1", steps = {"stage 1", "stage 2"})
@PipelineStepsDefinition(pipelineRootId = "Demo Pipeline", parallelId = "parallel 1", steps = {"stage 3", "stage 4"})
@PipelineStepsDefinition(pipelineRootId = "Demo Pipeline", parallelId = "parallel 1", steps = {"stage 5", "stage 6"})
@PipelineStepsDefinition(pipelineRootId = "Demo Pipeline", steps = {"stage 0", "parallel 1", "stage 7", "stage 8", "stage 9"})
public class PipelineDemo implements Stage, StageBuilder {
    private final static String MAX_DELAY_ENV_VAR = "PIPELINE_DEMO_MAX_DELAY";
    static final String PIPLINE_GRAPH = "/tmp/pipelineDemo.json";
    static final Long MAX_DELAY_DEFAULT = 10L;
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
        Long maxDelay = Long.valueOf(System.getenv().getOrDefault(MAX_DELAY_ENV_VAR, MAX_DELAY_DEFAULT.toString()));
        try {
            List<String> packages = new Vector<>();
            packages.add(PipelineDemo.class.getPackage().getName());
            PipelineBuilder builder = PipelineBuilder.createBuilder(packages);
            //demoPipeline = builder.buildFromPathName(PIPLINE_GRAPH);
            demoPipeline = builder.buildFromAnnotations("Demo Pipeline");

            TimerContext context = new TimerContext();
            context.setMaxDelay(maxDelay);
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
