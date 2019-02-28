package egghead.swish.swishcreatepayment.swish;

import egghead.swish.swishcreatepayment.depositservice.model.DepositOrderResponse;
import egghead.swish.swishcreatepayment.swish.model.CreatePaymentRequestResponse;
import egghead.swish.swishcreatepayment.swish.model.PaymentRequestObject;
import egghead.swish.swishcreatepayment.swish.model.RetrievePaymentResponse;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class SwishApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwishApi.class);
    private static final String PAYMENT_REQUEST_PATH = "/paymentrequests";

    private final WebClient webClient;
    private final String swishApiBaseUrl;
    private final SslContextFactory sslContextFactory;

    public SwishApi(@Value("${swish.api.base.url}") String swishApiBaseUrl, SslContextFactory sslContextFactory) {
        this.swishApiBaseUrl = swishApiBaseUrl;
        this.sslContextFactory = sslContextFactory;
        this.webClient = createWebClient();
    }

    private WebClient createWebClient() {

        SslContext sslContext = sslContextFactory.createSslContext();
        HttpClient httpClient = HttpClient.create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(swishApiBaseUrl)
            .build();
    }

    public Mono<RetrievePaymentResponse> callRetrievePayment(Scheduler scheduler, String location) {
        return webClient
            .get()
            .uri(location)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToMono(RetrievePaymentResponse.class)
            .subscribeOn(scheduler)
            .doOnError(err -> {
                LOGGER.warn("Got error {}", err);
            })
            .doOnSuccess(retrievePaymentResponse -> LOGGER.info("Received {} from swish", retrievePaymentResponse));
    }

    public Mono<CreatePaymentRequestResponse> callCreatePaymentRequest(Scheduler scheduler, DepositOrderResponse depositOrder) {
        PaymentRequestObject paymentRequestObject = new PaymentRequestObject();
        paymentRequestObject.setAmount(depositOrder.getAmount().toString());
        paymentRequestObject.setCallbackUrl("https://www.youtube.com");
        paymentRequestObject.setMessage(depositOrder.getMessageOnStatement());
        paymentRequestObject.setCurrency(depositOrder.getCurrency().getCurrencyCode());
        paymentRequestObject.setPayeePaymentReference(depositOrder.getOrderId());
        paymentRequestObject.setPayeeAlias(depositOrder.getMerchantSwishAlias());

        return webClient
            .post()
            .uri(PAYMENT_REQUEST_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(BodyInserters.fromObject(paymentRequestObject))
            .exchange()
            .subscribeOn(scheduler)
            .map(response -> {
                HttpHeaders headers = response.headers().asHttpHeaders();
                String location = headers.getFirst("Location");
                String paymentRequestToken = headers.getFirst("PaymentRequestToken");
                LOGGER.info("Returning {} and {}", location, paymentRequestToken);
                return new CreatePaymentRequestResponse(location, paymentRequestToken);

            }).doOnError(err -> {
                LOGGER.warn("Got error {}", err);
            });
    }
}
