package egghead.eggheadserviceb.controller;

import java.util.UUID;

/**
 * @author mikael
 */
public class ApiResponse {

    public static ApiResponse error(Exception e) {
        return new ApiResponse(null, e.toString());
    }

    public static ApiResponse success() {
        return new ApiResponse(UUID.randomUUID(), null);
    }

    private final UUID id;
    private final String service;
    private final String error;

    private ApiResponse(UUID id, String error) {
        this.id = id;
        this.service = "B";
        this.error = error;
    }

    public UUID getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getError() {
        return error;
    }
}
