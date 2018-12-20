package egghead.eggheadserviceb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mikael
 */
@RestController
public class RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiController.class);

    @RequestMapping(value = "API", method = RequestMethod.POST)
    public ApiResponse api(
        @RequestBody(required = false) ApiRequestBody requestBody
    ) throws InterruptedException {

        LOGGER.debug("Requesting with delay {}", requestBody.getDelay());

        if (requestBody.getDelay() != null) {
            Thread.sleep(requestBody.getDelay());
        }

        return ApiResponse.success();
    }

    @ExceptionHandler
    public ApiResponse onException(Exception e) {
        LOGGER.error("Unexpected exception", e);
        return ApiResponse.error(e);
    }
}
