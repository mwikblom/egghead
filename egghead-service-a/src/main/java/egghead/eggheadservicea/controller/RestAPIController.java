package egghead.eggheadservicea.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author mikael
 */
@RestController
public class RestAPIController {

    @RequestMapping(value = "service/API", method = RequestMethod.GET)
    public String api(
        @RequestParam(value = "delay", required = false) Long delay
    ) {

        log("Requesting with delay " + delay);
        try {
            if (delay != null) {
                Thread.sleep(delay);
            }

            return "{" +
                "\"id\":\"" + UUID.randomUUID() + "\"" +
                "\"service\":\"A\"" +
                "}";
        } catch (Exception e) {
            log("\terror: " + e.toString());

            return "{" +
                "\"error\":\"" + e.toString() + "\"" +
                "}";
        }
    }

    private void log(String message) {
        System.out.println(LocalDateTime.now() + " " + Thread.currentThread().getId() + ": " + message);
    }
}
