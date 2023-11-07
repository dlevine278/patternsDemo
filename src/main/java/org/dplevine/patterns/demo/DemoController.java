package org.dplevine.patterns.demo;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.dplevine.patterns.demo.pipelineDemo.PipelineDemo;
import org.dplevine.patterns.pipeline.Pipeline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Controller
public class DemoController {
    private final static String HOST_ENV_VAR = "PATTERNS_DEMO_HOST";
    private final static int MAX_DEMOS = 50;
    private final static long MAX_TIME_SECONDS = 60 * 15;  // demos can stick around for 15 min before they get purged
    private final String hostname = System.getenv().getOrDefault(HOST_ENV_VAR, "localhost");  // used by Thymeleaf template --> injected into the demo page via model attribute

    DemoController() {
    }

    private static class ActiveDemos {
        private Map<String, Object> demoInstances = new ConcurrentHashMap<>();
        private Map<String, LocalDateTime> demoActivationTimes = new ConcurrentHashMap<>();
        private static ActiveDemos activeDemos;

        private ActiveDemos() {
            Runnable demoWatchDog = () -> {
                while (true) {
                    try {
                        Thread.sleep(30 * 1000);  // wake up every 30 seconds and purge expired demos
                        demoActivationTimes.keySet().stream().filter(uuid -> ChronoUnit.SECONDS.between(demoActivationTimes.get(uuid), LocalDateTime.now()) > MAX_TIME_SECONDS).forEach(uuid -> purgeActiveDemo(uuid));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            Thread thread = new Thread(demoWatchDog);
            thread.start();
        }

        // Singleton pattern
        static ActiveDemos getDemos() {
            if (activeDemos == null) {
                activeDemos = new ActiveDemos();
            }
            return activeDemos;
        }

        String addDemo(Object demo) throws Exception {
            synchronized (this) {
                if (demoInstances.size() > MAX_DEMOS) {
                    throw new Exception("Maximum running demos reached for this server, try back in a bit after some demos have expired.");
                }

                String uuid;
                do {
                    uuid = UUID.randomUUID().toString();
                } while (demoInstances.containsKey(uuid));
                demoInstances.put(uuid, demo);
                demoActivationTimes.put(uuid, LocalDateTime.now());
                return uuid;
            }
        }

        Object getActiveDemo(String uuid) throws Exception {
            synchronized(this) {
                if (!demoInstances.containsKey(uuid)) {
                    throw new Exception("Could no longer find the requested demo.");
                }
                return demoInstances.get(uuid);
            }
        }

        void purgeActiveDemo(String uuid) {
            synchronized (this) {
                demoInstances.remove(uuid);
                demoActivationTimes.remove(uuid);
            }
        }
    }

    @GetMapping(value = "/patterns/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getList() {
        return "index";
    }

    @GetMapping(path = "/patterns/pipeline/demo")
    public String index(@RequestParam(name = "fastFail", defaultValue = "true") Optional<Boolean> fastFail,Model model, HttpServletRequest request, Principal principal) {
        model.addAttribute("hostname", hostname);
        return "pipeline";
    }

    @RequestMapping(value = "/patterns/pipeline")
    public void startNewPipelineDemo(@RequestParam(name = "fastFail", defaultValue = "true") Optional<Boolean> fastFail, HttpServletRequest request, HttpServletResponse response) {
        String uuid;
        try {
            PipelineDemo demo = new PipelineDemo();
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Origin,X-PINGOTHER,Content-Type,X-Requested-With,accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization");
            response.setHeader("Access-Control-Expose-Headers", "xsrf-token");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            final PrintWriter pw = response.getWriter();
            pw.print( "{ \"id\" : \"" + ActiveDemos.getDemos().addDemo(demo) + "\" }");
            demo.runDemo(fastFail.get());
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @RequestMapping(value = "/patterns/pipeline/graph/{id}", method = RequestMethod.GET)
    public void getImageAsByteArray(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PipelineDemo pipelineDemo = (PipelineDemo) ActiveDemos.getDemos().getActiveDemo(id);
            BufferedImage image = pipelineDemo.getImage();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "gif", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Origin,X-PINGOTHER,Content-Type: image/gif,X-Requested-With,accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization");
            response.setHeader("Access-Control-Expose-Headers", "xsrf-token");
            response.setContentType(MediaType.IMAGE_GIF_VALUE);
            IOUtils.copy(is, response.getOutputStream());
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @RequestMapping(value = "/patterns/pipeline/log/{id}", method = RequestMethod.GET)
    public void getEventLog(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PipelineDemo pipelineDemo = (PipelineDemo) ActiveDemos.getDemos().getActiveDemo(id);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Origin,X-PINGOTHER,Content-Type: application/json;charset=UTF-8,X-Requested-With,accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization");
            response.setHeader("Access-Control-Expose-Headers", "xsrf-token");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            final PrintWriter pw = response.getWriter();
            pw.print(pipelineDemo.getLogs());
            pw.close();
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
