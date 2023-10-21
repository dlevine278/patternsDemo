package org.dplevine.patterns.demo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
public class DemoController {
    private static int MAX_DEMOS = 1000;
    private static long MAX_TIME_SECONDS = 60 * 1;  // demos can stick around for 15 min before they get purged


    DemoController() {}

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
        return "list";
    }

    @GetMapping(value = "/patterns/demos/pipeline")
    public String startNewPipelineDemo() {
        String uuid;
        try {
            return ActiveDemos.getDemos().addDemo("demo");
        } catch (Exception e) {
            return e.getMessage();
        }
    }


    @RequestMapping(value = "/patterns/index", method = RequestMethod.GET)
    public void getDemolist(HttpServletResponse response) throws IOException {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER,Content-Type,X-Requested-With,accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization");
            response.setHeader("Access-Control-Expose-Headers", "xsrf-token");
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            final PrintWriter pw = response.getWriter();
            pw.println("patterns");
            pw.close();
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
