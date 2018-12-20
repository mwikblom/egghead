package egghead.eggheadfront.service;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mikael
 */
public interface RemoteServiceComm {

    RemoteService.ApiResponse call(RemoteService.Endpoint remoteEndpoint, Map<String, Object> params);

    // TEST with okhttpclient
    // TEST with jersey rest client
    // TEST with retrofit
    // TEST with RestTemplate
    // TEST with RESTeasy

    class ServiceCommException extends RuntimeException {
        public ServiceCommException(String message) {
            super(message);
        }

        public ServiceCommException(Throwable cause) {
            super(cause);
        }
    }

    @Service
    class Factory {

        private final ApplicationContext applicationContext;

        public Factory(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        public <T extends RemoteServiceComm> T create(Class<T> impl) {
            return applicationContext.getBean(impl);
        }

        public RemoteServiceComm createAny() {
            Map<String, RemoteServiceComm> serviceComms = applicationContext.getBeansOfType(RemoteServiceComm.class);
            List<RemoteServiceComm> instances = new ArrayList<>(serviceComms.values());
            Collections.shuffle(instances);
            return instances.get(0);
        }
    }
}
