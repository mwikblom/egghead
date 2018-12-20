package egghead.eggheadfront.controller;

import com.google.common.collect.ImmutableMap;
import egghead.eggheadfront.service.RemoteService;
import egghead.eggheadfront.service.RemoteServiceComm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiController.class);

    private final RemoteServiceComm.Factory remoteServiceCommFactory;

    public RestApiController(RemoteServiceComm.Factory remoteServiceCommFactory) {
        this.remoteServiceCommFactory = remoteServiceCommFactory;
    }

    @RequestMapping(value = "API", method = RequestMethod.GET)
    public ApiResponse api(
        @RequestParam("service") RemoteService.Endpoint service,
        @RequestParam(value = "delay", required = false) Long delay
    ) {

        LOGGER.debug("Requesting service {}", service);

        Map<String, Object> params = delay == null ? ImmutableMap.of() : ImmutableMap.of("delay", delay);

        RemoteServiceComm remoteServiceComm = remoteServiceCommFactory.createAny();
        RemoteService.ApiResponse remoteServiceApiResponse = remoteServiceComm.call(service, params);

        LOGGER.debug("remote service response: {}", remoteServiceApiResponse);

        return ApiResponse.success(remoteServiceComm.getClass(), remoteServiceApiResponse);
    }

    @ExceptionHandler
    public ApiResponse onException(Exception e) {
        LOGGER.error("Unexpected exception", e);
        return ApiResponse.error(e);
    }
}