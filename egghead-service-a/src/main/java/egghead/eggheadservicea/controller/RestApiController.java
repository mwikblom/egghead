package egghead.eggheadservicea.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mikael
 */
@RestController
public class RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiController.class);

    @RequestMapping(value = "API", method = RequestMethod.GET)
    public ApiResponse api(
        @RequestParam(value = "delay", required = false) Long delay
    ) throws InterruptedException {

        LOGGER.debug("Requesting with delay {}", delay);

        if (delay != null) {
            Thread.sleep(delay);
        }

        return ApiResponse.success();
    }

    @ExceptionHandler
    public ApiResponse onException(Exception e) {
        LOGGER.error("Unexpected exception", e);
        return ApiResponse.error(e);
    }
 }
