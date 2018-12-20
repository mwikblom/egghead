package egghead.eggheadfront.controller;

import com.google.common.collect.ImmutableSet;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@RestController
public class RestAPIController {

    enum Service {
        A("http://localhost:8081/service/API"),
        B("http://localhost:8082/service/API");

        private final String url;

        Service(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    private static final int TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    private final CloseableHttpClient httpClient;

    public RestAPIController() {
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

        // TEST with okhttpclient
        // TEST with jersey rest client
        // TEST with retrofit
        // TEST with RestTemplate
        // TEST with RESTeasy
    }

    @RequestMapping(value = "front/API", method = RequestMethod.GET)
    public String api(
        @RequestParam("service") Service service,
        @RequestParam(value = "delay", required = false) Long delay
    ) {

        log("Requesting service " + service);
        try {
            URIBuilder uriBuilder = new URIBuilder(service.getUrl());
            if (delay != null) {
                uriBuilder.addParameter("delay", String.valueOf(delay));
            }

            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(uriBuilder.build()))) {
                String payload = EntityUtils.toString(response.getEntity());

                log("\tpayload: " + payload);

                return payload;
            }
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