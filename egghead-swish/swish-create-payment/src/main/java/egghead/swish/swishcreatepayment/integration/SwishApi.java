package egghead.swish.swishcreatepayment.integration;

import egghead.swish.swishcreatepayment.integration.model.PaymentRequestObject;
import egghead.swish.swishcreatepayment.integration.model.SwishPaymentRequest;
import io.netty.handler.ssl.SslContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuples;

import javax.annotation.PreDestroy;

@Service
public class SwishApi {

    private static final String PAYMENT_REQUEST_PATH = "paymentrequests";

    private final WebClient webClient;
    private final String swishApiBaseUrl;


    public SwishApi(@Value("${swish.api.base.url}") String swishApiBaseUrl) {
        this.swishApiBaseUrl = swishApiBaseUrl;
        this.webClient = createWebClient();
    }

    private WebClient createWebClient() {

        SslContext sslContext = Certificate.createSslContext();
        HttpClient httpClient = HttpClient.create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(swishApiBaseUrl)
            .build();
    }

    public Mono callCreatePaymentRequest(Scheduler scheduler, PaymentRequestObject paymentRequestObject) {

        Mono<String> req = webClient
            .post()
            .uri(PAYMENT_REQUEST_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(BodyInserters.fromObject(paymentRequestObject))
            .exchange()
            .subscribeOn(scheduler)
            .map(response -> {
                HttpHeaders headers = response.headers().asHttpHeaders();
                headers.get("Location");
                LOGGER.info("Got respose {}", headers.get("Location"));
                return headers.toString();
                // TODO .. should be response Location.
            }).doOnError(err -> {
                LOGGER.warn("Got error {}", err);
            })
            .flatMap(y -> {
                LOGGER.info("Got response {}", y);
                return Mono.just(y);
            });

        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        // TODO create request to swish using depositServiceResponse

        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(SwishPaymentRequest.class)
            .subscribeOn(scheduler)
            .map(response -> {

                // fake some data
                SwishPaymentRequest swishPaymentRequest = new SwishPaymentRequest();
                swishPaymentRequest.setAutoStartToken("aAutoStartToken");
                swishPaymentRequest.setLocation("https://swishit.se/theLocation");

                LOGGER.info("callSwishPaymentRequest - THREAD.ID {}: {}", Thread.currentThread().getId(), swishPaymentRequest);

                return Tuples.of(swishDepositKafkaRequest, receiverOffset, depositOrder, swishPaymentRequest);
            });''
    }
}
