package egghead.swish.swishcreatepayment.integration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Service
public class Certificate {
    private final WebClient webClient;

    @Autowired
    public Certificate() {
        this.webClient = createWebClient();

    }
    private final static Logger LOGGER = LoggerFactory.getLogger(Certificate.class);

    private static KeyManagerFactory getKeyManagerFactory(String file, String password) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fileInputStream, password.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, password.toCharArray());
            return keyManagerFactory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SslContext createSslContext() {
        try {
            String pkcs12FilePath = Certificate.class.getClassLoader().getResource("swish.p12").getPath();
            KeyManagerFactory keyManagerFactory = getKeyManagerFactory(pkcs12FilePath, "swish");
            return SslContextBuilder
                .forClient()
                .startTls(true)
                .keyManager(keyManagerFactory)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSlContext.", e);
        }
    }

    private static WebClient createWebClient() {
        String baseUrl = "HTTPS://mss.cpc.getswish.net/swish-cpcapi/api/v1/";
        SslContext sslContext = createSslContext();
        HttpClient httpClient = HttpClient.create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(baseUrl)
            .build();

        return webClient;
    }

    public void doSwishCall() {
        SwishPaymentRequest swishPaymentRequest = new SwishPaymentRequest();
        swishPaymentRequest.setAmount("100");
        swishPaymentRequest.setCallbackUrl("https://www.youtube.com");
        swishPaymentRequest.setMessage("Britta");
        swishPaymentRequest.setCurrency("SEK");
        swishPaymentRequest.setPayeePaymentReference("ONETWOTHREE");
        swishPaymentRequest.setPayeeAlias("1231181189");
        String path = "paymentrequests";

        Scheduler scheduler = Schedulers.elastic();

        LOGGER.info("Creating Request.");

        Mono<String> req = webClient
            .post()
            .uri(path)
            .accept( MediaType.APPLICATION_JSON )
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(BodyInserters.fromObject(swishPaymentRequest))
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

        String res = req.block();

        String foo = "";
    }

    public static class SwishPaymentRequest {
        private String payeePaymentReference;
        private String callbackUrl;
        private String payeeAlias;
        private String amount;
        private String message;
        private String currency;

        public String getPayeePaymentReference() {
            return payeePaymentReference;
        }

        public void setPayeePaymentReference(String payeePaymentReference) {
            this.payeePaymentReference = payeePaymentReference;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }

        public String getPayeeAlias() {
            return payeeAlias;
        }

        public void setPayeeAlias(String payeeAlias) {
            this.payeeAlias = payeeAlias;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

}
