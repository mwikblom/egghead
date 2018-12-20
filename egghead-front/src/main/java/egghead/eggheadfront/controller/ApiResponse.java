package egghead.eggheadfront.controller;

import egghead.eggheadfront.service.RemoteService;
import egghead.eggheadfront.service.RemoteServiceComm;

/**
 * @author mikael
 */
public class ApiResponse {

    public static ApiResponse error(Exception e) {
        return new ApiResponse(null, null, e.toString());
    }

    public static ApiResponse success(Class<? extends RemoteServiceComm> remoteServiceCommClass, RemoteService.ApiResponse remoteServiceResponse) {
        return new ApiResponse(remoteServiceCommClass.getSimpleName(), remoteServiceResponse, null);
    }

    private final String remoteServiceComm;
    private final RemoteService.ApiResponse serviceResponse;
    private final String error;

    private ApiResponse(String remoteServiceComm, RemoteService.ApiResponse serviceResponse, String error) {
        this.remoteServiceComm = remoteServiceComm;
        this.serviceResponse = serviceResponse;
        this.error = error;
    }

    public String getRemoteServiceComm() {
        return remoteServiceComm;
    }

    public RemoteService.ApiResponse getServiceResponse() {
        return serviceResponse;
    }

    public String getError() {
        return error;
    }
}
