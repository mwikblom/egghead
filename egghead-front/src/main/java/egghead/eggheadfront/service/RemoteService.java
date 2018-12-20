package egghead.eggheadfront.service;

import org.springframework.http.HttpMethod;

/**
 * @author mikael
 */
public interface RemoteService {

    enum Endpoint {

        A(HttpMethod.GET, "http://egghead-service-a/API"),
        B(HttpMethod.POST, "http://egghead-service-b/API");

        private final HttpMethod method;
        private final String url;

        Endpoint(HttpMethod method, String url) {
            this.method = method;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public HttpMethod getMethod() {
            return method;
        }
    }

    class ApiResponse {

        private String id;
        private String service;
        private String error;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "ApiResponse{" +
                "id=" + id +
                ", service='" + service + '\'' +
                ", error='" + error + '\'' +
                '}';
        }
    }
}
