package egghead.eggheadkafka.service;

import com.google.common.collect.ImmutableSet;
import egghead.eggheadkafka.util.Json;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * @author mikael
 */
@Service
public class HttpClientComm implements RemoteServiceComm {

    private static final int TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    private final CloseableHttpClient httpClient;

    public HttpClientComm() {
        this.httpClient = HttpClientBuilder.create()
            .setDefaultHeaders(ImmutableSet.of(
                new BasicHeader("Accept", "application/json"),
                new BasicHeader("Content-Type", "application/json")
            ))
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build()
            )
            .build();
    }

    @Override
    public RemoteService.ApiResponse call(RemoteService.Endpoint endpoint, Map<String, Object> params) {

        HttpUriRequest request = RequestFactory.createRequest(endpoint, params);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String payload = EntityUtils.toString(response.getEntity());
            return Json.deserialize(payload, RemoteService.ApiResponse.class);
        } catch (Exception e) {
            throw new ServiceCommException(e);
        }
    }

    private static class RequestFactory {

        private static final BiFunction<String, Map<String, Object>, HttpUriRequest> GET_REQUEST_FACTORY = (url, params) -> {

            try {
                URIBuilder uriBuilder = new URIBuilder(url);
                params.forEach((key, value) -> uriBuilder.addParameter(key, String.valueOf(value)));

                return new HttpGet(uriBuilder.build());
            } catch (Exception e) {
                throw new ServiceCommException(e);
            }
        };
        private static final BiFunction<String, Map<String, Object>, HttpUriRequest> POST_REQUEST_FACTORY = (url, params) -> {

            try {
                URIBuilder uriBuilder = new URIBuilder(url);

                HttpPost postRequest = new HttpPost(uriBuilder.build());
                postRequest.setEntity(new StringEntity(Json.serialize(params), StandardCharsets.UTF_8));

                return postRequest;
            } catch (Exception e) {
                throw new ServiceCommException(e);
            }
        };

        private static HttpUriRequest createRequest(RemoteService.Endpoint endpoint, Map<String, Object> params) {
            switch (endpoint.getMethod()) {
                case GET:
                    return GET_REQUEST_FACTORY.apply(endpoint.getUrl(), params);
                case POST:
                    return POST_REQUEST_FACTORY.apply(endpoint.getUrl(), params);
                case HEAD:
                case PUT:
                case PATCH:
                case DELETE:
                case OPTIONS:
                case TRACE:
                default:
                    throw new ServiceCommException("Invalid method " + endpoint.getMethod());
            }
        }
    }

}
